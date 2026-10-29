package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RickyCyan

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onCitationClick: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current

    // Parse code blocks vs regular text
    val parts = splitCodeBlocks(content)

    Column(modifier = modifier) {
        for (part in parts) {
            if (part.isCode) {
                CodeBlock(code = part.text, language = part.language)
            } else {
                RenderRichText(
                    text = part.text,
                    textColor = textColor,
                    onCitationClick = onCitationClick
                )
            }
        }
    }
}

private data class ContentSegment(
    val text: String,
    val isCode: Boolean,
    val language: String = ""
)

private fun splitCodeBlocks(text: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val lines = text.lines()
    var inCode = false
    var currentLang = ""
    val buffer = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCode) {
                // End code block
                segments.add(ContentSegment(text = buffer.toString().trimEnd(), isCode = true, language = currentLang))
                buffer.clear()
                inCode = false
                currentLang = ""
            } else {
                // Start code block
                if (buffer.isNotEmpty()) {
                    segments.add(ContentSegment(text = buffer.toString(), isCode = false))
                    buffer.clear()
                }
                currentLang = line.trim().removePrefix("```").trim()
                inCode = true
            }
        } else {
            buffer.append(line).append("\n")
        }
    }

    if (buffer.isNotEmpty()) {
        segments.add(ContentSegment(text = buffer.toString(), isCode = inCode, language = currentLang))
    }

    return segments
}

@Composable
private fun RenderRichText(
    text: String,
    textColor: Color,
    onCitationClick: ((Int) -> Unit)?
) {
    val lines = text.lines()
    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("### ") -> {
                Text(
                    text = trimmed.removePrefix("### "),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RickyCyan,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            trimmed.startsWith("## ") -> {
                Text(
                    text = trimmed.removePrefix("## "),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RickyCyan,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }
            trimmed.startsWith("# ") -> {
                Text(
                    text = trimmed.removePrefix("# "),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RickyCyan,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
            }
            trimmed.startsWith("> ") -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(24.dp)
                            .background(RickyCyan, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trimmed.removePrefix("> "),
                        fontStyle = FontStyle.Italic,
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
            trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val bulletText = trimmed.replace(Regex("^[•\\-*]\\s+"), "")
                Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                    Text("•", color = RickyCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                    FormatInlineText(bulletText, textColor, onCitationClick)
                }
            }
            trimmed.isNotBlank() -> {
                FormatInlineText(line, textColor, onCitationClick)
            }
        }
    }
}

@Composable
private fun FormatInlineText(
    rawText: String,
    textColor: Color,
    onCitationClick: ((Int) -> Unit)?
) {
    val annotated = buildAnnotatedString {
        var cursor = 0
        // Match bold **...**, inline code `...`, and citations [1]
        val regex = Regex("(\\*\\*.*?\\*\\*|`.*?`|\\[\\d+\\])")
        val matches = regex.findAll(rawText)

        for (match in matches) {
            val range = match.range
            if (range.first > cursor) {
                append(rawText.substring(cursor, range.first))
            }
            val matchText = match.value
            when {
                matchText.startsWith("**") && matchText.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(matchText.removeSurrounding("**"))
                    }
                }
                matchText.startsWith("`") && matchText.endsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x3300E5FF),
                            color = RickyCyan,
                            fontSize = 12.sp
                        )
                    ) {
                        append(matchText.removeSurrounding("`"))
                    }
                }
                matchText.matches(Regex("\\[\\d+\\]")) -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = RickyCyan,
                            fontSize = 11.sp
                        )
                    ) {
                        append(matchText)
                    }
                }
                else -> append(matchText)
            }
            cursor = range.last + 1
        }
        if (cursor < rawText.length) {
            append(rawText.substring(cursor))
        }
    }

    Text(
        text = annotated,
        color = textColor,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
