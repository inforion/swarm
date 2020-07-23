package ru.inforion.lab403.swarm.common

import java.io.Serializable

data class Response<R>(val obj: R, val index: Int): Serializable