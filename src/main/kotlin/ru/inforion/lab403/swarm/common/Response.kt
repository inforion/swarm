@file:Suppress("NOTHING_TO_INLINE")

package ru.inforion.lab403.swarm.common

import java.io.Serializable

/**
 * Class represent serializable value with indexed
 * It will be good to used Kotlin [IndexedValue] but it is not serializable
 *
 * @property value the underlying value
 * @property index the index of the value in some collection or sequence
 */
internal data class Response<out R> constructor(val value: R, val index: Int): Serializable