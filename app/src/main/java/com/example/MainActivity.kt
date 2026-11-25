package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.dexdictate.ui.DexDictateApp
import com.example.dexdictate.ui.DexDictateViewModel
import com.example.ui.theme.DexDictateTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DexDictateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DexDictateTheme {
                DexDictateApp(controller = viewModel.controller)
            }
        }
    }
}
