package com.jrm.frap.data

import android.content.Context
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecurityRepository(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "frap_security",
            Context.MODE_PRIVATE
        )

    companion object {

        private const val PASSWORD_HASH = "password_hash"
        private const val PASSWORD_SALT = "password_salt"

        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
    }


    // ======================================================
    // CHECK PASSWORD
    // ======================================================

    fun hasAdminPassword(): Boolean {

        return !preferences
            .getString(PASSWORD_HASH, null)
            .isNullOrBlank()
    }


    // ======================================================
    // CREATE ADMIN PASSWORD
    // ======================================================

    fun setAdminPassword(
        password: String
    ) {

        require(password.length >= 6) {
            "Password must be at least 6 characters."
        }

        val salt =
            ByteArray(16)

        SecureRandom().nextBytes(salt)

        val hash =
            hashPassword(
                password,
                salt
            )

        preferences
            .edit()
            .putString(
                PASSWORD_HASH,
                bytesToHex(hash)
            )
            .putString(
                PASSWORD_SALT,
                bytesToHex(salt)
            )
            .apply()
    }


    // ======================================================
    // VERIFY PASSWORD
    // ======================================================

    fun verifyAdminPassword(
        password: String
    ): Boolean {

        val storedHash =
            preferences
                .getString(
                    PASSWORD_HASH,
                    null
                )
                ?: return false

        val storedSalt =
            preferences
                .getString(
                    PASSWORD_SALT,
                    null
                )
                ?: return false

        return try {

            val salt =
                hexToBytes(
                    storedSalt
                )

            val calculatedHash =
                hashPassword(
                    password,
                    salt
                )

            constantTimeEquals(
                calculatedHash,
                hexToBytes(storedHash)
            )

        } catch (
            exception: Exception
        ) {

            false
        }
    }


    // ======================================================
    // PBKDF2 PASSWORD HASH
    // ======================================================

    private fun hashPassword(
        password: String,
        salt: ByteArray
    ): ByteArray {

        val spec =
            PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            )

        return try {

            SecretKeyFactory
                .getInstance(
                    "PBKDF2WithHmacSHA256"
                )
                .generateSecret(spec)
                .encoded

        } finally {

            spec.clearPassword()
        }
    }


    // ======================================================
    // CONSTANT-TIME COMPARISON
    // ======================================================

    private fun constantTimeEquals(
        first: ByteArray,
        second: ByteArray
    ): Boolean {

        if (first.size != second.size) {
            return false
        }

        var result = 0

        for (index in first.indices) {

            result =
                result or
                        (
                                first[index].toInt()
                                        xor
                                        second[index].toInt()
                                )
        }

        return result == 0
    }


    // ======================================================
    // BYTE → HEX
    // ======================================================

    private fun bytesToHex(
        bytes: ByteArray
    ): String {

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }


    // ======================================================
    // HEX → BYTE
    // ======================================================

    private fun hexToBytes(
        value: String
    ): ByteArray {

        require(
            value.length % 2 == 0
        )

        return ByteArray(
            value.length / 2
        ) { index ->

            value
                .substring(
                    index * 2,
                    index * 2 + 2
                )
                .toInt(16)
                .toByte()
        }
    }
}