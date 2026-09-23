package io.github.p1neapplexpress.openflux.data

import java.io.ByteArrayInputStream
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class YandexCookieStoreTest {
    @Test
    fun `saves cookie file in private directory`() {
        val dir = Files.createTempDirectory("cookies-test").toFile()
        val bytes = ".yandex.ru\tTRUE\t/\tTRUE\t0\tsession\tvalue\n".toByteArray()
        val saved = YandexCookieStore.save(ByteArrayInputStream(bytes), dir)
        assertEquals(dir, saved.parentFile)
        assertArrayEquals(bytes, saved.readBytes())
        assertEquals(saved, YandexCookieStore.existing(dir))
    }

    @Test
    fun `invalid replacement keeps existing cookie file`() {
        val dir = Files.createTempDirectory("cookies-test").toFile()
        val first = ".yandex.ru\tTRUE\t/\tTRUE\t0\tsession\tfirst\n".toByteArray()
        YandexCookieStore.save(ByteArrayInputStream(first), dir)
        assertThrows(IllegalArgumentException::class.java) {
            YandexCookieStore.save(ByteArrayInputStream(ByteArray(0)), dir)
        }
        assertArrayEquals(first, YandexCookieStore.existing(dir)!!.readBytes())
    }

    @Test
    fun `rejects oversized file`() {
        val dir = Files.createTempDirectory("cookies-test").toFile()
        assertThrows(IllegalArgumentException::class.java) {
            YandexCookieStore.save(ByteArrayInputStream(ByteArray(1_048_577)), dir)
        }
    }
}
