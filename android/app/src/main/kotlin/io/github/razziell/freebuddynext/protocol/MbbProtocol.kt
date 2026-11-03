package io.github.razziell.freebuddynext.protocol

import java.io.ByteArrayOutputStream

data class MbbCommand(
    val serviceId: Int,
    val commandId: Int,
    val args: Map<Int, ByteArray> = emptyMap(),
) {
    fun toPayload(): ByteArray {
        val data = ByteArrayOutputStream()
        args.forEach { (key, value) ->
            require(key in 0..255 && value.size <= 255)
            data.write(key)
            data.write(value.size)
            data.write(value)
        }
        val dataBytes = data.toByteArray()
        val body = byteArrayOf(
            0x5A, 0x00, (dataBytes.size + 3).toByte(), 0x00,
            serviceId.toByte(), commandId.toByte(),
        ) + dataBytes
        val crc = crc16Xmodem(body)
        return body + byteArrayOf((crc ushr 8).toByte(), crc.toByte())
    }

    companion object {
        fun parse(payload: ByteArray): MbbCommand {
            require(payload.size >= 8) { "MBB packet is too short" }
            require(payload[0].u() == 0x5A && payload[1].u() == 0 && payload[3].u() == 0) {
                "Invalid MBB header"
            }
            require(payload.size == payload[2].u() + 5) { "Invalid MBB length" }
            val expected = crc16Xmodem(payload.copyOf(payload.size - 2))
            val actual = (payload[payload.lastIndex - 1].u() shl 8) or payload.last().u()
            require(expected == actual) { "Invalid MBB checksum" }

            val args = linkedMapOf<Int, ByteArray>()
            var offset = 6
            val end = payload.size - 2
            while (offset < end) {
                require(offset + 2 <= end) { "Truncated MBB argument" }
                val id = payload[offset++].u()
                val length = payload[offset++].u()
                require(offset + length <= end) { "Invalid MBB argument length" }
                args[id] = payload.copyOfRange(offset, offset + length)
                offset += length
            }
            return MbbCommand(payload[4].u(), payload[5].u(), args)
        }
    }
}

class MbbStreamParser {
    private var pending = ByteArray(0)

    fun append(bytes: ByteArray): List<MbbCommand> {
        if (bytes.isNotEmpty()) {
            pending = if (pending.isEmpty()) bytes.copyOf() else pending + bytes
        }
        val result = mutableListOf<MbbCommand>()
        var offset = 0
        val end = pending.size
        while (end - offset >= 3) {
            var magicIndex = -1
            for (i in offset until end) {
                if (pending[i].u() == 0x5A) {
                    magicIndex = i
                    break
                }
            }
            if (magicIndex < 0) {
                offset = end
                break
            }
            offset = magicIndex
            if (end - offset < 3) break
            val packetLength = pending[offset + 2].u() + 5
            if (packetLength !in 8..260) {
                offset += 1
                continue
            }
            if (end - offset < packetLength) break
            val packet = pending.copyOfRange(offset, offset + packetLength)
            offset += packetLength
            runCatching { MbbCommand.parse(packet) }
                .onSuccess { if (!(it.serviceId == 10 && it.commandId == 13)) result += it }
        }
        pending = when {
            offset >= end -> ByteArray(0)
            offset > 0 -> pending.copyOfRange(offset, end)
            pending.size > 4096 -> pending.copyOfRange(pending.size - 260, pending.size)
            else -> pending
        }
        return result
    }

    fun clear() { pending = ByteArray(0) }
}

/** Stable, human-readable dump used by the diagnostics clipboard export. */
fun MbbCommand.toDiagnosticString(): String {
    val tlvs = args.entries.joinToString(" ") { (id, value) ->
        "$id=${value.toHex()}"
    }
    return buildString {
        append(serviceId).append('/').append(commandId)
        append(" packet=").append(toPayload().toHex())
        if (tlvs.isNotEmpty()) append(" tlv[").append(tlvs).append(']')
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

private fun Byte.u() = toInt() and 0xFF
fun ByteArray.firstUOrNull() = firstOrNull()?.toInt()?.and(0xFF)
fun intBytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

fun crc16Xmodem(data: ByteArray): Int {
    var crc = 0
    for (byte in data) {
        crc = crc xor (byte.u() shl 8)
        repeat(8) {
            crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
            crc = crc and 0xFFFF
        }
    }
    return crc
}
