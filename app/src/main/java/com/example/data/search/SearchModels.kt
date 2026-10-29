package com.example.data.search

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String,
    val date: String? = null
)

data class Citation(
    val index: Int,
    val title: String,
    val url: String,
    val snippet: String
)

data class ResearchReport(
    val query: String,
    val results: List<SearchResultItem>,
    val citations: List<Citation>,
    val verifiedFacts: List<String>,
    val synthesisNotes: String,
    val isInsufficientEvidence: Boolean = false
)
