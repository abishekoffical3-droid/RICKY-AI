package com.example.data.search

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class WebPageReader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchPageText(url: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 RICKY-AI-Research/1.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                val title = extractTagContent(body, "title").ifBlank { url }
                val cleanText = extractReadableText(body)
                Pair(title, cleanText.take(3500))
            }
        } catch (e: Exception) {
            Log.w("WebPageReader", "Unable to read web page: $url (${e.message})")
            null
        }
    }

    private fun extractTagContent(html: String, tag: String): String {
        val pattern = Pattern.compile("<$tag[^>]*>(.*?)</$tag>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        return if (matcher.find()) {
            matcher.group(1)?.replace(Regex("<[^>]*>"), "")?.trim().orEmpty()
        } else {
            ""
        }
    }

    private fun extractReadableText(html: String): String {
        // Strip scripts and styles
        var text = html.replace(Regex("(?i)<script[^>]*>[\\s\\S]*?</script>"), " ")
        text = text.replace(Regex("(?i)<style[^>]*>[\\s\\S]*?</style>"), " ")
        // Extract paragraph texts
        val paragraphs = mutableListOf<String>()
        val pPattern = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val pMatcher = pPattern.matcher(text)
        while (pMatcher.find()) {
            val rawP = pMatcher.group(1)?.replace(Regex("<[^>]*>"), " ")?.trim()
            if (!rawP.isNullOrBlank() && rawP.length > 25) {
                // Decode common HTML entities
                val cleanP = rawP
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&nbsp;", " ")
                paragraphs.add(cleanP)
            }
        }

        return if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            // Fallback: strip all tags
            text.replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
