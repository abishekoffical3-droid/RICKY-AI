package com.example

import com.example.core.identity.AnswerStyle
import com.example.core.identity.RickyIdentity
import com.example.core.identity.UserProfile
import com.example.data.search.ResearchEngine
import com.example.feature.tools.CsvAnalyzer
import com.example.feature.tools.SafeCalculator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSafeCalculator_basicOperations() {
        val result = SafeCalculator.evaluate("2 + 3 * 4")
        assertTrue(result is SafeCalculator.CalcResult.Success)
        val success = result as SafeCalculator.CalcResult.Success
        assertEquals(14.0, success.value, 0.0001)
        assertEquals("14", success.formattedResult)
    }

    @Test
    fun testSafeCalculator_parenthesesAndFunctions() {
        val result = SafeCalculator.evaluate("sqrt(144) + (2^3) * 5")
        assertTrue(result is SafeCalculator.CalcResult.Success)
        val success = result as SafeCalculator.CalcResult.Success
        assertEquals(52.0, success.value, 0.0001)
        assertEquals("52", success.formattedResult)
    }

    @Test
    fun testSafeCalculator_divisionByZero() {
        val result = SafeCalculator.evaluate("100 / 0")
        assertTrue(result is SafeCalculator.CalcResult.Error)
    }

    @Test
    fun testCsvAnalyzer_parsesAndSummarizes() {
        val csv = """
            Item, Price, Quantity
            Apple, 1.50, 10
            Banana, 0.75, 20
            Cherry, 3.00, 5
        """.trimIndent()

        val result = CsvAnalyzer.analyze(csv)
        assertTrue(result.isSuccess)
        val data = result.getOrNull()!!
        assertEquals(3, data.rowCount)
        assertEquals(listOf("Item", "Price", "Quantity"), data.columnNames)
        assertTrue(data.numericalSummaries.containsKey("Price"))
        val priceStats = data.numericalSummaries["Price"]!!
        assertEquals(3, priceStats.count)
        assertEquals(5.25, priceStats.sum, 0.01)
        assertEquals(0.75, priceStats.min, 0.01)
        assertEquals(3.00, priceStats.max, 0.01)
    }

    @Test
    fun testResearchEngine_heuristics() {
        val engine = ResearchEngine()
        assertFalse(engine.shouldResearch("hi"))
        assertFalse(engine.shouldResearch("2 + 2"))
        assertTrue(engine.shouldResearch("What is the capital of Nepal?"))
        assertTrue(engine.shouldResearch("latest release of Kotlin"))
    }

    @Test
    fun testRickyIdentity_systemPromptIncludesUserDetails() {
        val identity = RickyIdentity()
        val profile = UserProfile(
            displayName = "Abhishek Bhusal",
            region = "Nepal",
            answerStyle = AnswerStyle.CONCISE
        )
        val prompt = identity.getSystemPrompt(profile, "Gemini", "gemini-3.8-flash")
        assertTrue(prompt.contains("RICKY AI"))
        assertTrue(prompt.contains("Abhishek Bhusal"))
        assertTrue(prompt.contains("Nepal"))
        assertTrue(prompt.contains("Nepali"))
    }
}
