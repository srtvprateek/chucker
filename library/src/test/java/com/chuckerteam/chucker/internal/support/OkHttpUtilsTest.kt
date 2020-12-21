package com.chuckerteam.chucker.internal.support

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf
import okhttp3.Response
import okio.Buffer
import okio.BufferedSource
import okio.GzipSink
import okio.buffer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class OkHttpUtilsTest {

    @Test
    fun isChunked_withNotChunked() {
        val mockResponse = mockk<Response>()
        every { mockResponse.header("Transfer-Encoding") } returns "gzip"

        assertThat(mockResponse.isChunked).isFalse()
    }

    @Test
    fun isChunked_withNoTransferEncoding() {
        val mockResponse = mockk<Response>()
        every { mockResponse.header("Content-Length") } returns null
        every { mockResponse.header("Transfer-Encoding") } returns null

        assertThat(mockResponse.isChunked).isFalse()
    }

    @Test
    fun isChunked_withChunked() {
        val mockResponse = mockk<Response>()
        every { mockResponse.header("Transfer-Encoding") } returns "chunked"

        assertThat(mockResponse.isChunked).isTrue()
    }

    @Test
    fun uncompressSource_withGzippedContent() {
        val content = "Hello there!"
        val source = Buffer()
        GzipSink(source).buffer().use { it.writeUtf8(content) }

        val result = source.uncompress(headersOf("Content-Encoding", "gzip"))
            .buffer()
            .use(BufferedSource::readUtf8)

        assertThat(result).isEqualTo(content)
    }

    @Test
    fun uncompressSource_withPlainTextContent() {
        val content = "Hello there!"
        val source = Buffer().writeUtf8(content)

        val result = source.uncompress(headersOf())
            .buffer()
            .use(BufferedSource::readUtf8)

        assertThat(result).isEqualTo(content)
    }

    @ParameterizedTest(name = "\"{0}\" must be supported: {1}")
    @MethodSource("supportedEncodingSource")
    @DisplayName("Check if body encoding is supported")
    fun headersHaveSupportedEncoding(headers: Headers, isSupported: Boolean) {
        val result = headers.hasSupportedContentEncoding

        assertThat(result).isEqualTo(isSupported)
    }

    companion object {
        @JvmStatic
        fun supportedEncodingSource(): Stream<Arguments> = Stream.of(
            "" to true,
            "identity" to true,
            "gzip" to true,
            "other" to false,
        ).map { (encoding, result) ->
            Arguments.of(headersOf("Content-Encoding", encoding), result)
        }
    }
}
