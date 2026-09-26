package com.minimo.launcher.utils

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FuzzySearchTest {
    @Test
    fun findsTyposAndOtherAlphabet() {
        assertNotNull(FuzzySearch.distance("Telegram", "телеграмм"))
        assertNotNull(FuzzySearch.distance("Telegram", "телеграм"))
        assertNotNull(FuzzySearch.distance("WhatsApp", "whatsap"))
        assertNotNull(FuzzySearch.distance("WhatsApp", "ватсап"))
        assertNotNull(FuzzySearch.distance("YouTube", "ютуб"))
        assertNotNull(FuzzySearch.distance("Instagram", "instgram"))
        assertNotNull(FuzzySearch.distance("Google Maps", "maps"))
        assertNotNull(FuzzySearch.distance("Chrome", "chorme")) // swapped letters
    }

    @Test
    fun ignoresUnrelated() {
        assertNull(FuzzySearch.distance("Telegram", "камера"))
        assertNull(FuzzySearch.distance("Calculator", "tele"))
        assertNull(FuzzySearch.distance("YouTube", "yo")) // too short to guess
    }
}
