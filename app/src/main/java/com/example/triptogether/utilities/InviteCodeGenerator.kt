package com.example.triptogether.utilities

import kotlin.random.Random

object InviteCodeGenerator {
    fun generateCode(): String {
        val charset = Constants.InviteCode.CHARSET
        val codeLength = Constants.InviteCode.CODE_LENGTH

        return (1..codeLength)
            .map { charset[Random.nextInt(charset.length)] }
            .joinToString("")
    }
}
