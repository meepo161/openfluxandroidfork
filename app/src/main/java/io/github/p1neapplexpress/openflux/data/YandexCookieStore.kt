package io.github.p1neapplexpress.openflux.data

import java.io.File
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Stores the user's Netscape cookie export in Android's app-private, non-backed-up directory. */
object YandexCookieStore {
    private const val FILE_NAME = "yandex-cookies.txt"
    private const val MAX_BYTES = 1_048_576

    fun existing(privateDir: File): File? =
        File(privateDir, FILE_NAME).takeIf { it.isFile && it.length() > 0 }

    fun save(input: InputStream, privateDir: File): File {
        privateDir.mkdirs()
        val target = File(privateDir, FILE_NAME)
        val temp = File.createTempFile("yandex-cookies-", ".tmp", privateDir)
        try {
            var count = 0
            temp.outputStream().use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    count += size
                    require(count <= MAX_BYTES) { "Cookie file is too large" }
                    output.write(buffer, 0, size)
                }
            }
            require(count > 0) { "Cookie file is empty" }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            return target
        } finally {
            temp.delete()
        }
    }
}
