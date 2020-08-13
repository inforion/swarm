package ru.inforion.lab403.swarm.common

import java.io.Serializable

internal data class Response<R>(val data: R, val index: Int): Serializable