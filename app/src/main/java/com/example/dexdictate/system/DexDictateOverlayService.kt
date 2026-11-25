package com.example.dexdictate.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.dexdictate.model.OverlayActionTab
import com.example.dexdictate.model.OverlayVisibility
import com.example.dexdictate.ui.FloatingDexterOverlayContent
import com.example.ui.theme.DexDictateTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Native Android WindowManager foreground service that floats Dexter over other apps.
 *
 * Keyboard preservation strategy:
 * - Uses FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL by default so tapping Collapsed or Expanded Dexter
 *   does NOT dismiss or steal focus from the underlying app's Android keyboard.
 * - Only clears FLAG_NOT_FOCUSABLE transiently when the user enters Review & Insert to edit text,
 *   and restores FLAG_NOT_FOCUSABLE immediately upon collapsing or leaving Review.
 */
class DexDictateOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var controller: OverlayController
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateWatchJob: Job? = null

    private var collapsedX: Int = 24
    private var collapsedY: Int = 280

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        controller = OverlayRuntimeBridge.getOrCreate(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_OVERLAY) {
            controller.onHideOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            _isRunning.value = false
            controller.refreshPermissions()
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        attachOverlayIfNeeded()
        controller.onRestoreOverlay()
        _isRunning.value = true
        controller.refreshPermissions()

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_notification_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hideIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, DexDictateOverlayService::class.java).setAction(ACTION_STOP_OVERLAY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_body))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(
                0,
                getString(R.string.overlay_notification_action_hide),
                hideIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun attachOverlayIfNeeded() {
        if (overlayView != null) return

        val overlayWindowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType,
            NON_FOCUSABLE_FLAGS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = collapsedX
            y = collapsedY
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@DexDictateOverlayService)
            setViewTreeViewModelStoreOwner(this@DexDictateOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DexDictateOverlayService)

            setContent {
                DexDictateTheme {
                    val state by controller.overlayState.collectAsState()
                    FloatingDexterOverlayContent(
                        state = state,
                        isSystemOverlayWindow = true,
                        onExpand = { controller.onExpandOverlay(fromLiveSystemOverlay = true) },
                        onCollapse = { controller.onCollapseOverlay() },
                        onHide = {
                            controller.onHideOverlay()
                            stopSelf()
                        },
                        onDragCollapsedBy = { dx, dy ->
                            updateCollapsedPosition(dx, dy)
                        },
                        onReadAloud = { controller.onReadAloudClicked(fromLiveSystemOverlay = true) },
                        onStopPlayback = { controller.onStopPlayback() },
                        onReplayPlayback = { controller.onReplayPlayback() },
                        onExplainMessage = { controller.onExplainMessageClicked(fromLiveSystemOverlay = true) },
                        onDictateReply = { controller.onDictateClicked(fromLiveSystemOverlay = true) },
                        onAcceptNetworkDisclosure = { controller.onAcceptNetworkRecognitionDisclosure() },
                        onStopDictation = { controller.onStopDictation() },
                        onOpenTypedFallback = { controller.onUseTypedFallback() },
                        onRequestMicPermission = {
                            // Open MainActivity / App Settings for microphone recovery when in system overlay
                            startActivity(controller.permissionsController.createAppSettingsIntent())
                        },
                        onOpenAppSettings = {
                            startActivity(controller.permissionsController.createAppSettingsIntent())
                        },
                        onReviewDraftChanged = { controller.onReviewDraftChanged(it) },
                        onPlayReviewDraft = { controller.onPlayReviewDraft() },
                        onInsertReviewedReply = { controller.onInsertReviewedReply() },
                        onCopyReviewedReply = { controller.onCopyReviewedReplyFallback() },
                        onClearTempData = { controller.onClearTemporaryData() }
                    )
                }
            }
        }

        layoutParams = params
        overlayView = composeView
        runCatching { windowManager.addView(composeView, params) }

        stateWatchJob?.cancel()
        stateWatchJob = serviceScope.launch {
            controller.overlayState.collect { uiState ->
                syncWindowParams(uiState.visibility, uiState.activeAction)
            }
        }
    }

    private fun updateCollapsedPosition(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        if (controller.overlayState.value.visibility != OverlayVisibility.COLLAPSED) return
        // Gravity is TOP | END so positive dx moves right (reduces end margin)
        collapsedX = (collapsedX - dx.roundToInt()).coerceIn(0, 900)
        collapsedY = (collapsedY + dy.roundToInt()).coerceIn(64, 1600)
        params.x = collapsedX
        params.y = collapsedY
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun syncWindowParams(visibility: OverlayVisibility, activeAction: OverlayActionTab) {
        val params = layoutParams ?: return
        val view = overlayView ?: return

        when (visibility) {
            OverlayVisibility.HIDDEN -> {
                stopSelf()
                return
            }
            OverlayVisibility.COLLAPSED -> {
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.TOP or Gravity.END
                params.x = collapsedX
                params.y = collapsedY
                params.flags = NON_FOCUSABLE_FLAGS
            }
            OverlayVisibility.EXPANDED -> {
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                params.x = 0
                params.y = 140
                // Keep Android keyboard visible in the underlying app unless user explicitly edits in Review
                params.flags = if (activeAction == OverlayActionTab.REVIEW_AND_INSERT) {
                    FOCUSABLE_REVIEW_FLAGS
                } else {
                    NON_FOCUSABLE_FLAGS
                }
            }
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    override fun onDestroy() {
        stateWatchJob?.cancel()
        serviceScope.cancel()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        layoutParams = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        _isRunning.value = false
        controller.refreshPermissions()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "dexdictate_overlay_channel"
        private const val NOTIFICATION_ID = 4201
        const val ACTION_START_OVERLAY = "com.example.dexdictate.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.dexdictate.STOP_OVERLAY"

        private const val NON_FOCUSABLE_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        private const val FOCUSABLE_REVIEW_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, DexDictateOverlayService::class.java)
                .setAction(ACTION_START_OVERLAY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DexDictateOverlayService::class.java)
                .setAction(ACTION_STOP_OVERLAY)
            context.startService(intent)
        }
    }
}
