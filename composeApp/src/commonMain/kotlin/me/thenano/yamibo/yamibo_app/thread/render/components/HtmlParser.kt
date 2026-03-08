package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode

object HtmlParser {

    /** Parses raw HTML string into a list of HtmlBlock for Compose to render */
    fun parseHtml(html: String): List<HtmlBlock> {
        // Strip raw newlines from HTML to prevent false double-newlines
        val cleanHtml = html.replace("\r", "").replace("\n", "")
        val document: Document = Ksoup.parseBodyFragment(cleanHtml)
        
        val blocks = mutableListOf<HtmlBlock>()
        val globalBuilder = AnnotatedString.Builder()
        var lastCommitIndex = 0

        fun commitText() {
            if (globalBuilder.length > lastCommitIndex) {
                var textStr = globalBuilder.toAnnotatedString().subSequence(lastCommitIndex, globalBuilder.length)
                
                // Trim trailing newlines manually to avoid overblown spacing
                while (textStr.length > 0 && textStr.lastOrNull() == '\n') {
                    textStr = textStr.subSequence(0, textStr.length - 1)
                }
                
                if (textStr.isNotEmpty()) {
                    blocks.add(HtmlBlock.Text(textStr))
                }
                lastCommitIndex = globalBuilder.length
            }
        }

        fun parseNode(node: com.fleeksoft.ksoup.nodes.Node) {
            when (node) {
                is TextNode -> {
                    val txt = node.text()
                    if (txt.isNotEmpty()) globalBuilder.append(txt)
                }
                is Element -> {
                    val tag = node.tagName().lowercase()
                    when (tag) {
                        "br" -> {
                            val str = globalBuilder.toAnnotatedString()
                            // Avoid adding more than 2 consecutive newlines
                            if (!str.endsWith("\n\n")) {
                                globalBuilder.append("\n")
                            }
                        }
                        "img" -> {
                            commitText()
                            val src = node.attr("src")
                            val alt = node.attr("alt").takeIf { it.isNotBlank() }
                            if (src.isNotBlank()) blocks.add(HtmlBlock.Image(src, alt))
                        }
                        "div" -> {
                            val clazz = node.attr("class")
                            when {
                                clazz.contains("showcollapse_box") -> {
                                    commitText()
                                    val titleNode = node.selectFirst(".showcollapse_title")
                                    val titleText = titleNode?.text()?.takeIf { it.isNotBlank() } ?: "點擊展開 / 收起"
                                    titleNode?.remove() // removed so it doesn't render twice in content blocks
                                    val innerBlocks = parseHtml(node.html())
                                    blocks.add(HtmlBlock.Collapse(title = titleText, contentBlocks = innerBlocks))
                                }
                                clazz.contains("locked-content") -> {
                                    commitText()
                                    val costText = node.select(".locked-tip").text()
                                    val cost = costText.toIntOrNull() ?: 0
                                    val innerBlocks = parseHtml(node.html())
                                    blocks.add(HtmlBlock.Locked(cost = cost, contentBlocks = innerBlocks))
                                }
                                clazz.contains("quote") || clazz.contains("blockquote") -> {
                                    commitText()
                                    val innerBlocks = parseHtml(node.html())
                                    blocks.add(HtmlBlock.Quote(innerBlocks))
                                }
                                clazz.contains("blockcode") -> {
                                    commitText()
                                    blocks.add(HtmlBlock.Code(node.text()))
                                }
                                else -> {
                                    if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                        globalBuilder.append("\n")
                                    }
                                    node.childNodes().forEach { parseNode(it) }
                                    if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                        globalBuilder.append("\n")
                                    }
                                }
                            }
                        }
                        "p", "ul", "ol", "table", "tbody", "tr", "td" -> {
                            if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                globalBuilder.append("\n")
                            }
                            node.childNodes().forEach { parseNode(it) }
                            if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                globalBuilder.append("\n")
                            }
                        }
                        "b", "strong" -> globalBuilder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { node.childNodes().forEach { parseNode(it) } }
                        "i", "em" -> globalBuilder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { node.childNodes().forEach { parseNode(it) } }
                        "u" -> globalBuilder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { node.childNodes().forEach { parseNode(it) } }
                        "s", "strike" -> globalBuilder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { node.childNodes().forEach { parseNode(it) } }
                        "a" -> {
                            val href = node.attr("href")
                            val start = globalBuilder.length
                            node.childNodes().forEach { parseNode(it) }
                            val end = globalBuilder.length
                            if (href.isNotBlank() && start < end) {
                                globalBuilder.addStringAnnotation("URL", href, start, end)
                                globalBuilder.addStyle(SpanStyle(color = Color(0xFF007BFF), textDecoration = TextDecoration.Underline), start, end)
                            }
                        }
                        "font" -> {
                            val colorAttr = node.attr("color")
                            var color: Color? = null
                            if (colorAttr.isNotEmpty()) {
                                color = try {
                                    if (colorAttr.startsWith("#")) Color(colorAttr.removePrefix("#").toLong(16) or 0xFF000000)
                                    else Color.Unspecified
                                } catch (_: Exception) { Color.Unspecified }
                            }
                            val style = if (color != null && color != Color.Unspecified) SpanStyle(color = color) else SpanStyle()
                            globalBuilder.withStyle(style) { node.childNodes().forEach { parseNode(it) } }
                        }
                        "span" -> {
                            val styleAttr = node.attr("style")
                            var color: Color? = null
                            var bgColor: Color? = null
                            if (styleAttr.contains("color:")) {
                                val match = Regex("color:\\s*#([0-9a-fA-F]{6})").find(styleAttr)
                                if (match != null) color = Color(match.groupValues[1].toLong(16) or 0xFF000000)
                            }
                            if (styleAttr.contains("background-color:")) {
                                val match = Regex("background-color:\\s*#([0-9a-fA-F]{6})").find(styleAttr)
                                if (match != null) bgColor = Color(match.groupValues[1].toLong(16) or 0xFF000000)
                            }
                            val style = SpanStyle(color = color ?: Color.Unspecified, background = bgColor ?: Color.Unspecified)
                            globalBuilder.withStyle(style) { node.childNodes().forEach { parseNode(it) } }
                        }
                        else -> { node.childNodes().forEach { parseNode(it) } }
                    }
                }
                else -> { /* Other node types, ignore */ }
            }
        }

        document.body().childNodes().forEach { parseNode(it) }
        commitText()

        return blocks
    }
}
