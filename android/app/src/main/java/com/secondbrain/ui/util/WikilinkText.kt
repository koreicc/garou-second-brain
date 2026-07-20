package com.secondbrain.ui.util

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withAnnotation

/**
 * Renders text that may contain [[wikilink]] references.
 *
 * Regular text is displayed normally; [[wikilinks]] are rendered with a distinct
 * link style (primary color, bold) and trigger [onWikilinkClick] with the link
 * target (the text between the double brackets).
 *
 * Use this in note/task/person detail views to render body content that may
 * contain cross-entity references.
 *
 * @param text The raw text, possibly with [[wikilink]] markers.
 * @param onWikilinkClick Called with the wikilink target (e.g. "Title" from [[Title]]).
 * @param modifier Modifier for the container.
 * @param style Text styling for regular (non-link) portions.
 */
@Composable
fun WikilinkText(
    text: String,
    onWikilinkClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedString = remember(text, linkColor) { buildWikilinkAnnotatedString(text, linkColor) }

    ClickableText(
        text = annotatedString,
        style = style,
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations("wikilink", offset, offset).firstOrNull()?.let {
                onWikilinkClick(it.item)
            }
        }
    )
}

/**
 * Builds an AnnotatedString from raw text, marking [[wikilink]] regions with
 * a "wikilink" annotation and applying link styling.
 */
@OptIn(ExperimentalTextApi::class)
private fun buildWikilinkAnnotatedString(
    text: String,
    linkColor: Color
): AnnotatedString {
    val linkStyle = SpanStyle(
        color = linkColor,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Normal
    )

    return buildAnnotatedString {
        val regex = Regex("""\[\[([^\]]+)]]""")
        var lastEnd = 0

        for (match in regex.findAll(text)) {
            // Append plain text before this match
            if (match.range.first > lastEnd) {
                append(text.substring(lastEnd, match.range.first))
            }

            val linkTarget = match.groupValues[1]
            val linkDisplay = "[[$linkTarget]]"

            // Push annotation for click handling
            withAnnotation("wikilink", linkTarget) {
                pushStyle(linkStyle)
                append(linkDisplay)
                pop()
            }

            lastEnd = match.range.last + 1
        }

        // Append remaining text
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
}
