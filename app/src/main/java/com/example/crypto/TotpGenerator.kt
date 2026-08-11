package com.example.crypto

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpGenerator {

    /**
     * Generates a 6-digit TOTP code for the given Base32 secret string.
     */
    fun generateTotp(secret: String, timeMs: Long = System.currentTimeMillis(), timeStepSeconds: Int = 30): String {
        val cleanSecret = secret.replace(" ", "").uppercase()
        if (cleanSecret.isEmpty()) return ""

        val keyBytes = try {
            decodeBase32(cleanSecret)
        } catch (e: Exception) {
            return "INVALID"
        }

        if (keyBytes.isEmpty()) return "INVALID"

        val timeIndex = timeMs / 1000 / timeStepSeconds
        val data = ByteBuffer.allocate(8).putLong(timeIndex).array()

        return try {
            val mac = Mac.getInstance("HmacSHA1")
            val signKey = SecretKeySpec(keyBytes, "HmacSHA1")
            mac.init(signKey)
            val hash = mac.doFinal(data)

            val offset = (hash[hash.size - 1].toInt() and 0x0F)
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % 10.toDouble().pow(6.0).toInt()
            String.format("%06d", otp)
        } catch (e: Exception) {
            "ERROR"
        }
    }

    /**
     * Returns remaining seconds in current TOTP 30-second window (0..30).
     */
    fun getRemainingSeconds(timeMs: Long = System.currentTimeMillis(), timeStepSeconds: Int = 30): Int {
        val secondsCurrentWindow = (timeMs / 1000) % timeStepSeconds
        return (timeStepSeconds - secondsCurrentWindow).toInt()
    }

    private fun decodeBase32(base32: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.trimEnd('=')
        var buffer = 0
        var bitsLeft = 0
        val result = mutableListOf<Byte>()

        for (char in clean) {
            val valIndex = base32Chars.indexOf(char)
            if (valIndex < 0) continue
            buffer = (buffer shl 5) or valIndex
            bitsLeft += 5
            if (bitsLeft >= 8) {
                result.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return result.toByteArray()
    }
}
