package me.thenano.yamibo.yamibo_app.thread.reader.components.post

import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlBlock
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlParserRubyTest {
    @Test
    fun rubyIsRenderedAsInlineFallbackWithoutOverlayRubies() {
        val blocks = HtmlParser.parseHtml(
            """
            <p>原文中使用了<ruby>性騷擾<rt>Sexual Harassment</rt></ruby>。</p>
            """.trimIndent(),
        )

        val textBlocks = blocks.filterIsInstance<HtmlBlock.Text>()

        assertTrue(textBlocks.isNotEmpty())
        assertEquals(emptyList(), textBlocks.flatMap { it.rubies })
        assertEquals(
            "原文中使用了性騷擾(Sexual Harassment)。",
            textBlocks.joinToString(separator = "") { it.annotatedString.text }.trim(),
        )
    }

    @Test
    fun rubyParenthesisTagsAreNotDuplicated() {
        val blocks = HtmlParser.parseHtml("<ruby>帆華<rp>(</rp><rt>Hanaho</rt><rp>)</rp></ruby>")
        val text = blocks.filterIsInstance<HtmlBlock.Text>().joinToString(separator = "") { it.annotatedString.text }

        assertEquals("帆華(Hanaho)", text)
    }
}
