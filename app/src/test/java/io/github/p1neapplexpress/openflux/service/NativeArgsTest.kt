package io.github.p1neapplexpress.openflux.service

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeArgsTest {

    private val socks = "127.0.0.1:4000"

    @Test
    fun `old payload gets client role loopback socks5 and keeps the rest`() {
        val payload = listOf("--client", "--transport", "yandex", "--url", "https://d", "--debug")
        assertEquals(
            listOf(
                "--role", "client", "--inbound", "socks5", "--socks5", socks,
                "--transport", "yandex", "--url", "https://d", "--debug",
            ),
            NativeArgs.build(payload, socks, keyFile = null),
        )
    }

    @Test
    fun `flags owned by the app are replaced`() {
        val payload = listOf(
            "--role=exit", "--socks5", ":1080", "--encryption-key-file=/sdcard/key",
            "-i", "tun", "--transport", "mailru", "--url", "a/b",
        )
        assertEquals(
            listOf(
                "--role", "client", "--inbound", "socks5", "--socks5", socks,
                "--encryption-key-file", "/data/key", "--transport", "mailru", "--url", "a/b",
            ),
            NativeArgs.build(payload, socks, keyFile = "/data/key"),
        )
    }

    @Test
    fun `url values containing equals signs survive`() {
        val payload = listOf("--transport", "cupsonline", "--url=https://x/?rooms=abc==")
        assertEquals(
            listOf(
                "--role", "client", "--inbound", "socks5", "--socks5", socks,
                "--transport", "cupsonline", "--url=https://x/?rooms=abc==",
            ),
            NativeArgs.build(payload, socks, keyFile = null),
        )
    }

    @Test
    fun `redact hides the max token`() {
        assertEquals(
            listOf("--maxToken", "***", "--maxUid", "1"),
            NativeArgs.redact(listOf("--maxToken", "secret", "--maxUid", "1")),
        )
        assertEquals(listOf("--maxToken=***"), NativeArgs.redact(listOf("--maxToken=secret")))
    }

    @Test
    fun `vyandex uses only app owned cookie file`() {
        val payload = listOf("--transport", "vyandex", "--url", "https://d", "--yandex-cookies-file=/sdcard/untrusted")
        assertEquals(
            listOf(
                "--role", "client", "--inbound", "socks5", "--socks5", socks,
                "--yandex-cookies-file", "/data/private/cookies.txt",
                "--transport", "vyandex", "--url", "https://d",
            ),
            NativeArgs.build(payload, socks, keyFile = null, cookieFile = "/data/private/cookies.txt"),
        )
    }

    @Test
    fun `other transports do not receive Yandex cookie file`() {
        val payload = listOf("--transport", "mailru", "--url", "https://d", "--yandex-cookies-file", "/sdcard/untrusted")
        assertEquals(
            listOf("--role", "client", "--inbound", "socks5", "--socks5", socks, "--transport", "mailru", "--url", "https://d"),
            NativeArgs.build(payload, socks, keyFile = null, cookieFile = "/data/private/cookies.txt"),
        )
    }
}
