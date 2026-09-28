package com.example

import com.example.data.remote.GeminiApiService
import com.example.data.util.CsvDatasetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MindCareLogicTest {

    private val geminiService = GeminiApiService()

    @Test
    fun testCrisisSafetyDetection() {
        val crisisText = "I want to end my life, please help"
        val risk = geminiService.detectSafetyRisk(crisisText)
        assertEquals("crisis", risk)

        val moderateText = "I'm having a panic attack and feel hopeless"
        val moderateRisk = geminiService.detectSafetyRisk(moderateText)
        assertEquals("moderate_risk", moderateRisk)

        val safeText = "Can we practice mindful breathing together?"
        val safeRisk = geminiService.detectSafetyRisk(safeText)
        assertEquals("safe", safeRisk)
    }

    @Test
    fun testEmotionClassification() {
        assertEquals("anxiety", geminiService.detectEmotion("I am so anxious about tomorrow"))
        assertEquals("sadness", geminiService.detectEmotion("I feel so depressed and lonely"))
        assertEquals("stress", geminiService.detectEmotion("I am so stressed out with deadlines"))
        assertEquals("joy", geminiService.detectEmotion("I feel so grateful and happy today"))
        assertEquals("neutral", geminiService.detectEmotion("Hello MindCare"))
    }

    @Test
    fun testCsvDatasetParser() {
        val sampleCsv = """
            prompt,response,category,emotion,safety_level
            "How do I calm down?","Try the 5-4-3-2-1 technique",grounding,anxiety,safe
            "I feel overwhelmed","Take a 5-minute pause and breathe",burnout,stress,safe
        """.trimIndent()

        val result = CsvDatasetParser.parseCsvText(sampleCsv, "test.csv")
        assertEquals(2, result.validRows)
        assertEquals(2, result.items.size)
        assertEquals("grounding", result.items[0].category)
        assertEquals("anxiety", result.items[0].emotion)
    }
}
