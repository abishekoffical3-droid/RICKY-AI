package com.example.data.search

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun search(
        query: String,
        provider: String = "DUCKDUCKGO",
        apiKey: String = ""
    ): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResultItem>()

        // 1. First, search Wikipedia API for factual, encyclopedic, organization, and person data
        try {
            val wikiResults = searchWikipedia(query)
            results.addAll(wikiResults)
        } catch (e: Exception) {
            Log.w("SearchService", "Wikipedia query failed: ${e.message}")
        }

        // 2. DuckDuckGo Instant Answers & Web Results
        try {
            val ddgResults = searchDuckDuckGo(query)
            for (item in ddgResults) {
                if (results.none { it.url.equals(item.url, ignoreCase = true) }) {
                    results.add(item)
                }
            }
        } catch (e: Exception) {
            Log.w("SearchService", "DuckDuckGo query failed: ${e.message}")
        }

        // 3. If custom/Tavily API key provided, also fetch Tavily results
        if (apiKey.isNotBlank() && (provider == "TAVILY" || provider == "SERPER")) {
            try {
                val apiResults = searchWithApiKey(query, provider, apiKey)
                for (item in apiResults) {
                    if (results.none { it.url.equals(item.url, ignoreCase = true) }) {
                        results.add(0, item) // prioritize custom API
                    }
                }
            } catch (e: Exception) {
                Log.w("SearchService", "API provider $provider query failed: ${e.message}")
            }
        }

        results.take(6)
    }

    private fun searchWikipedia(query: String): List<SearchResultItem> {
        val items = mutableListOf<SearchResultItem>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json&utf8=1&srlimit=3"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RICKY-AI-Assistant/1.0 (Android)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val root = JSONObject(body)
            val queryObj = root.optJSONObject("query") ?: return emptyList()
            val searchArr = queryObj.optJSONArray("search") ?: return emptyList()

            for (i in 0 until searchArr.length()) {
                val item = searchArr.getJSONObject(i)
                val title = item.getString("title")
                val snippetHtml = item.optString("snippet", "")
                val snippet = snippetHtml.replace(Regex("<[^>]*>"), "").replace("&quot;", "\"")
                val pageUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                val timestamp = item.optString("timestamp", "")
                val date = if (timestamp.length >= 10) timestamp.substring(0, 10) else null

                items.add(
                    SearchResultItem(
                        title = title,
                        snippet = snippet,
                        url = pageUrl,
                        source = "Wikipedia",
                        date = date
                    )
                )
            }
        }
        return items
    }

    private fun searchDuckDuckGo(query: String): List<SearchResultItem> {
        val items = mutableListOf<SearchResultItem>()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        // Instant Answer API
        try {
            val iaUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(iaUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) RICKY-AI/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val heading = root.optString("Heading", "")
                        val abstractText = root.optString("AbstractText", "")
                        val abstractUrl = root.optString("AbstractURL", "")
                        val abstractSource = root.optString("AbstractSource", "DuckDuckGo")

                        if (heading.isNotBlank() && abstractText.isNotBlank()) {
                            items.add(
                                SearchResultItem(
                                    title = heading,
                                    snippet = abstractText,
                                    url = abstractUrl.ifBlank { "https://duckduckgo.com/?q=$encodedQuery" },
                                    source = abstractSource
                                )
                            )
                        }

                        val relatedTopics = root.optJSONArray("RelatedTopics")
                        if (relatedTopics != null) {
                            for (i in 0 until minOf(relatedTopics.length(), 3)) {
                                val topic = relatedTopics.optJSONObject(i) ?: continue
                                val text = topic.optString("Text", "")
                                val firstUrl = topic.optString("FirstURL", "")
                                if (text.isNotBlank() && firstUrl.isNotBlank()) {
                                    val parts = text.split(" - ", limit = 2)
                                    val title = parts.firstOrNull() ?: heading
                                    val snip = if (parts.size > 1) parts[1] else text
                                    items.add(
                                        SearchResultItem(
                                            title = title,
                                            snippet = snip,
                                            url = firstUrl,
                                            source = "DuckDuckGo"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SearchService", "DDG Instant Answer error: ${e.message}")
        }

        // Also fetch HTML search results for comprehensive queries if items is sparse
        if (items.size < 3) {
            try {
                val htmlUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
                val request = Request.Builder()
                    .url(htmlUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string().orEmpty()
                        val resultPattern = Pattern.compile(
                            "<a class=\"result__url\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>[\\s\\S]*?<a class=\"result__snippet\"[^>]*>([\\s\\S]*?)</a>",
                            Pattern.CASE_INSENSITIVE
                        )
                        val matcher = resultPattern.matcher(html)
                        while (matcher.find() && items.size < 6) {
                            var rawUrl = matcher.group(1).orEmpty()
                            // DDG redirect unpacker
                            if (rawUrl.contains("uddg=")) {
                                val uddg = rawUrl.substringAfter("uddg=").substringBefore("&")
                                rawUrl = java.net.URLDecoder.decode(uddg, "UTF-8")
                            }
                            val rawTitle = matcher.group(2).orEmpty().replace(Regex("<[^>]*>"), "").trim()
                            val rawSnippet = matcher.group(3).orEmpty().replace(Regex("<[^>]*>"), "").trim()
                            if (rawUrl.startsWith("http") && rawTitle.isNotBlank()) {
                                val domain = try { java.net.URI(rawUrl).host.removePrefix("www.") } catch (_: Exception) { "Web" }
                                items.add(
                                    SearchResultItem(
                                        title = rawTitle,
                                        snippet = rawSnippet,
                                        url = rawUrl,
                                        source = domain
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SearchService", "DDG HTML query error: ${e.message}")
            }
        }

        return items
    }

    private fun searchWithApiKey(query: String, provider: String, apiKey: String): List<SearchResultItem> {
        val items = mutableListOf<SearchResultItem>()
        if (provider.equals("TAVILY", ignoreCase = true)) {
            val jsonPayload = JSONObject().apply {
                put("query", query)
                put("api_key", apiKey)
                put("max_results", 4)
            }
            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .header("Content-Type", "application/json")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return emptyList()
                    val root = JSONObject(body)
                    val results = root.optJSONArray("results") ?: return emptyList()
                    for (i in 0 until results.length()) {
                        val r = results.getJSONObject(i)
                        items.add(
                            SearchResultItem(
                                title = r.getString("title"),
                                snippet = r.optString("content", ""),
                                url = r.getString("url"),
                                source = "Tavily"
                            )
                        )
                    }
                }
            }
        }
        return items
    }
}
