@file:OptIn(ExperimentalForeignApi::class)
package ru.denis0001dev.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.EOF
import platform.posix._IO_FILE
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs

class IOException(message: String): Exception(message)

enum class Mode(val mode: String) {
    Read("r"),
    Write("w"),
    Append("a"),
    ReadExtended("r+"),
    WriteExtended("w+"),
    AppendExtended("a+")
}

typealias IOFile = _IO_FILE
typealias IOFilePointer = CPointer<IOFile>

data class File(
    val path: String
) {
    private var handle: IOFilePointer? = null
    private var buffer: CArrayPointer<ByteVar>? = null

    var isOpen: Boolean = false
        private set

    fun open(mode: Mode): File {
        handle =
            fopen(path, Mode.Write.mode) ?:
            throw IOException("Cannot open output file $path")
        isOpen = true
        return this
    }

    fun read(scope: MemScope, chars: Int = 64 * 1024): String? {
        with(scope) {
            val buffer = buffer ?: allocArray<ByteVar>(chars)
            return fgets(buffer, chars, handle)?.toKString()
        }
    }

    fun readAll(): String {
        val returnBuffer = StringBuilder()
        memScoped {
            var line = read(this)
            while (line != null) {
                returnBuffer += line
                line = read(this)
            }
        }
        return returnBuffer.toString()
    }

    fun write(content: String) {
        if (fputs(content, handle) == EOF) {
            throw IOException("File write error")
        }
    }

    fun close() {
        fclose(handle)
        isOpen = false
    }
}