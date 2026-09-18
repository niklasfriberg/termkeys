package com.github.niklasfriberg.termkeys

import java.io.ByteArrayOutputStream

/**
 * Decodes a human-readable sequence into the bytes to send to the terminal.
 * Supported escapes: \n \r \t \0 \e (ESC) \\ and \xHH (two hex digits). Any other text is UTF-8 encoded.
 * An unrecognized or malformed escape is emitted as a literal backslash so nothing is silently dropped.
 */
object KeySequenceCodec {

    fun decode(input: String): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c != '\\' || i + 1 >= input.length) {
                out.write(c.toString().toByteArray(Charsets.UTF_8))
                i++
                continue
            }
            when (input[i + 1]) {
                'n' -> { out.write(0x0A); i += 2 }
                'r' -> { out.write(0x0D); i += 2 }
                't' -> { out.write(0x09); i += 2 }
                '0' -> { out.write(0x00); i += 2 }
                'e' -> { out.write(0x1B); i += 2 }
                '\\' -> { out.write('\\'.code); i += 2 }
                'x' -> {
                    val hex = input.substring(i + 2, minOf(i + 4, input.length))
                    val value = if (hex.length == 2) hex.toIntOrNull(16) else null
                    if (value != null) {
                        out.write(value); i += 4
                    } else {
                        out.write('\\'.code); i++
                    }
                }
                else -> { out.write('\\'.code); i++ }
            }
        }
        return out.toByteArray()
    }

    /** "0a 1b 5b 41" style preview of the decoded bytes, for the settings dialog. */
    fun toHexPreview(input: String): String =
        decode(input).joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }
}
