package com.example.dexdictate.providers

import com.example.dexdictate.model.MessageExplanation

/**
 * Replaceable interface for plain-language message explanations.
 *
 * Round 1 uses OfflineDeterministicExplanationProvider (100% local, zero network).
 * Future rounds can plug in Gemini (Firebase AI Logic with explicit user consent),
 * OpenAI, or an on-device local LLM adapter without changing UI or controller code.
 */
interface MessageExplanationProvider {
    val providerName: String
    val isOffline: Boolean
    suspend fun explain(originalMessage: String): MessageExplanation
}

/**
 * Offline deterministic provider for Round 1.
 * Handles seeded informal, queer, and consensual adult slang directly without moralizing,
 * sanitizing intended meaning, or inventing unstated details.
 */
class OfflineDeterministicExplanationProvider : MessageExplanationProvider {

    override val providerName: String = "Offline Deterministic Provider (Round 1)"
    override val isOffline: Boolean = true

    private data class SeededEntry(
        val plainLanguage: String,
        val slangBreakdown: List<String>,
        val ambiguityNote: String?
    )

    private val seededMessages: Map<String, SeededEntry> = mapOf(
        normalize("yo want sum dik?") to SeededEntry(
            plainLanguage = "They're asking whether you're interested in a sexual encounter.",
            slangBreakdown = listOf(
                "\"yo\" — casual greeting (\"hey\")",
                "\"want sum\" — informal spelling of \"want some\"",
                "\"dik\" — informal phonetic spelling of \"dick\""
            ),
            ambiguityNote = "The message does not specify a particular sexual act, time, or place. You can ask what they have in mind if you want clarity."
        ),
        normalize("u hostin rn? tryna link b4 11") to SeededEntry(
            plainLanguage = "They're asking if you have a private place where you can meet right now, because they want to meet up before 11.",
            slangBreakdown = listOf(
                "\"u\" — \"you\"",
                "\"hostin\" — hosting (having someone come over to your place)",
                "\"rn\" — \"right now\"",
                "\"tryna link\" — \"trying to meet up\"",
                "\"b4 11\" — \"before 11\""
            ),
            ambiguityNote = "They didn't clarify whether they mean 11 PM or 11 AM, or what kind of hangout they're looking for."
        ),
        normalize("top/vers? no flaking pls, dl ok") to SeededEntry(
            plainLanguage = "They're asking about your preferred sexual role (top or versatile), asking you not to cancel last minute, and letting you know that being discreet is fine with them.",
            slangBreakdown = listOf(
                "\"top/vers\" — preferred sexual role: top or versatile",
                "\"no flaking\" — please don't cancel or disappear at the last minute",
                "\"pls\" — \"please\"",
                "\"dl ok\" — \"down low\" / discreet is okay"
            ),
            ambiguityNote = "They haven't shared their own preferences or location yet."
        ),
        normalize("wyd tn? down to chill n see where it goes") to SeededEntry(
            plainLanguage = "They're asking what you are doing tonight and if you want to hang out casually without strict expectations.",
            slangBreakdown = listOf(
                "\"wyd\" — \"what are you doing?\"",
                "\"tn\" — \"tonight\"",
                "\"down to chill\" — interested in hanging out relaxed",
                "\"n\" — \"and\""
            ),
            ambiguityNote = "\"See where it goes\" is open-ended — it could mean just hanging out or potentially getting intimate if you both want to."
        ),
        normalize("npk? pics ok, just lookin 4 nsa fun nearby") to SeededEntry(
            plainLanguage = "They're asking if you're open to sharing photos (possibly explicit) and saying they want a casual, no-strings-attached hookup close by.",
            slangBreakdown = listOf(
                "\"npk\" — informal shorthand sometimes used for \"no pics?\" or \"no problem, k\" depending on chat context",
                "\"lookin 4\" — \"looking for\"",
                "\"nsa\" — \"no strings attached\" (casual encounter without relationship expectations)"
            ),
            ambiguityNote = "Shorthand like \"npk\" can vary by person; ask directly if you're unsure what they meant."
        ),
        normalize("omw 5 mins, park round back & lmk") to SeededEntry(
            plainLanguage = "They're on their way and expect to arrive in about 5 minutes. They want you to park around the back and message them when you get there.",
            slangBreakdown = listOf(
                "\"omw\" — \"on my way\"",
                "\"5 mins\" — \"5 minutes\"",
                "\"lmk\" — \"let me know\""
            ),
            ambiguityNote = "It's slightly ambiguous whether they are asking you to park around back or telling you where they will park."
        )
    )

    private val abbreviationGlossary: Map<String, String> = mapOf(
        "u" to "you",
        "ur" to "your / you're",
        "rn" to "right now",
        "wyd" to "what are you doing",
        "tn" to "tonight",
        "b4" to "before",
        "omw" to "on my way",
        "lmk" to "let me know",
        "pls" to "please",
        "plz" to "please",
        "sum" to "some",
        "dik" to "dick (informal phonetic spelling)",
        "tryna" to "trying to",
        "hostin" to "hosting (having someone over)",
        "dl" to "down low / discreet",
        "nsa" to "no strings attached (casual)",
        "fwb" to "friends with benefits",
        "tbh" to "to be honest",
        "idk" to "I don't know",
        "imo" to "in my opinion",
        "wya" to "where are you at",
        "hmu" to "hit me up (message me)",
        "bb" to "bareback or babe (context-dependent)",
        "vers" to "versatile",
        "np" to "no problem",
        "nvm" to "never mind",
        "brb" to "be right back"
    )

    override suspend fun explain(originalMessage: String): MessageExplanation {
        val trimmed = originalMessage.trim()
        val key = normalize(trimmed)

        seededMessages[key]?.let { seeded ->
            return MessageExplanation(
                originalText = trimmed,
                plainLanguage = seeded.plainLanguage,
                slangBreakdown = seeded.slangBreakdown,
                ambiguityNote = seeded.ambiguityNote,
                providerLabel = providerName,
                isOfflineDeterministic = true
            )
        }

        val tokens = trimmed
            .lowercase()
            .split(Regex("[\\s,?!.;:/]+"))
            .filter { it.isNotBlank() }

        val matchedTerms = tokens
            .distinct()
            .mapNotNull { token ->
                abbreviationGlossary[token]?.let { meaning ->
                    "\"$token\" — $meaning"
                }
            }

        val plainSummary = if (matchedTerms.isNotEmpty()) {
            "This message uses shorthand or informal spelling. Reading it with those terms expanded keeps the sender's direct wording intact without guessing hidden context."
        } else {
            "This message is written in relatively direct wording. No seeded slang pattern matched, so the safest reading is the literal text shown above."
        }

        val ambiguity = if (matchedTerms.isNotEmpty()) {
            "Offline Round 1 explanation expands known shorthand only. If any detail feels unclear, ask the sender directly."
        } else {
            "Offline Round 1 uses deterministic rules rather than a live AI model, so subtle tone or inside jokes are not inferred."
        }

        return MessageExplanation(
            originalText = trimmed,
            plainLanguage = plainSummary,
            slangBreakdown = matchedTerms,
            ambiguityNote = ambiguity,
            providerLabel = providerName,
            isOfflineDeterministic = true
        )
    }

    private fun normalize(text: String): String {
        return text.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
