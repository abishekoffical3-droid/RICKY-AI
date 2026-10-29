package com.example.data.search

import android.util.Log

class ResearchEngine(
    private val searchService: SearchService = SearchService(),
    private val pageReader: WebPageReader = WebPageReader()
) {

    /**
     * Determines whether fresh online web research is beneficial for the user query.
     */
    fun shouldResearch(prompt: String): Boolean {
        val lower = prompt.lowercase()
        // Fast skip for simple greetings or small talk
        if (lower.length < 4 || lower in listOf("hi", "hello", "namaste", "hey", "who are you", "what is your name")) {
            return false
        }
        // Fast skip for simple pure math expressions
        if (lower.matches(Regex("^[0-9+\\-*/^().\\s]+$"))) {
            return false
        }
        // Check for indicators of recent news, live data, documentation, people, release versions, facts
        val indicators = listOf(
            "who is", "who was", "what is", "where is", "when did", "how to", "latest", "recent",
            "news", "today", "yesterday", "current", "release", "version", "nepal", "kathmandu",
            "weather", "score", "price", "stock", "update", "research", "search", "lookup", "citation",
            "source", "documentation", "docs", "api", "library", "gemini", "android", "kotlin", "openai"
        )
        return indicators.any { lower.contains(it) } || prompt.endsWith("?")
    }

    /**
     * Executes real research workflow:
     * 1. Search search providers
     * 2. Inspect top accessible web pages for rich excerpt
     * 3. Construct structured report with citations
     */
    suspend fun conductResearch(
        query: String,
        provider: String = "DUCKDUCKGO",
        apiKey: String = ""
    ): ResearchReport {
        val results = searchService.search(query, provider, apiKey)

        if (results.isEmpty()) {
            return ResearchReport(
                query = query,
                results = emptyList(),
                citations = emptyList(),
                verifiedFacts = emptyList(),
                synthesisNotes = "No accessible search results found for query: '$query'.",
                isInsufficientEvidence = true
            )
        }

        // Build citations list
        val citations = results.mapIndexed { index, item ->
            Citation(
                index = index + 1,
                title = item.title,
                url = item.url,
                snippet = item.snippet
            )
        }

        // Retrieve deep text for the top 1 or 2 results if accessible
        val deepSnippets = StringBuilder()
        var deepReadCount = 0
        for (item in results.take(2)) {
            try {
                val pageData = pageReader.fetchPageText(item.url)
                if (pageData != null && pageData.second.isNotBlank()) {
                    deepReadCount++
                    deepSnippets.appendLine("--- Source [${deepReadCount}] (${item.title} - ${item.url}) ---")
                    deepSnippets.appendLine(pageData.second.take(1200))
                    deepSnippets.appendLine()
                }
            } catch (e: Exception) {
                Log.w("ResearchEngine", "Page reading error: ${e.message}")
            }
        }

        val facts = results.map { item ->
            "${item.title} (${item.source}): ${item.snippet.take(160)}"
        }

        val synthesis = buildString {
            appendLine("Web Research Results for '$query':")
            citations.forEach { c ->
                appendLine("[${c.index}] \"${c.title}\" (${c.url})")
                appendLine("Snippet: ${c.snippet}")
            }
            if (deepSnippets.isNotBlank()) {
                appendLine()
                appendLine("Deep Content Extracts:")
                appendLine(deepSnippets.toString())
            }
        }

        return ResearchReport(
            query = query,
            results = results,
            citations = citations,
            verifiedFacts = facts,
            synthesisNotes = synthesis,
            isInsufficientEvidence = false
        )
    }
}
