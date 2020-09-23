package ru.inforion.lab403.swarm.common

import java.io.Serializable

data class Mail(val sender: Int, val obj: Serializable) {
    inline fun <reified T> objectAs() = obj as T
}