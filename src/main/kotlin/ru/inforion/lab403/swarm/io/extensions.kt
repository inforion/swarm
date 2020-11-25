package ru.inforion.lab403.swarm.io

import java.io.*
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Wraps [this] output stream in [GZIPOutputStream] if data compression is enabled otherwise returns [this]
 *
 * @param enabled if true wraps otherwise return original stream
 */
private fun OutputStream.gzip(enabled: Boolean) = if (enabled) GZIPOutputStream(this) else this

/**
 * Wraps [this] input stream in [GZIPInputStream] if data compression is enabled otherwise returns [this]
 *
 * @param enabled if true wraps otherwise return original stream
 */
private fun InputStream.gzip(enabled: Boolean) = if (enabled) GZIPInputStream(this) else this

/**
 * Serialize [this] object in specified [output] stream
 *
 * @param output is output stream to store serialized object
 * @param compress if true stream will be wrapped with GZIP
 *
 * WARNING: this method closes the input stream, so after using the method,
 *   writing to the stream is no longer allowed. This is due to the use of
 *   the GZIP stream compression feature. To write data correctly with a
 *   GZIP stream, call the [GZIPOutputStream.finish] method required,
 *   which can be called in the [OutputStream.close] method
 */
private fun <T: Serializable> T.serialize(output: OutputStream, compress: Boolean) =
    output.gzip(compress).use { ObjectOutputStream(it).writeUnshared(this) }

/**
 * Calculate the size of an object after serialization by dummy "writing" it to an array
 * In fact data is not written to memory, but only the pointer is shifted in the [DummyOutputStream]
 *
 * @param compress if true calculate size for compressed with GZIP
 */
fun <T: Serializable> T.calcObjectSize(compress: Boolean) =
    DummyOutputStream().apply { serialize(this, compress) }.written

/**
 * Serialize an object to a buffer. [ByteBuffer] is chosen as the destination for the object
 * to be serialized so that it can be transferred to native libraries, for example, to [MPI].
 *
 * @param directed required or not buffer to be directed
 * @param compress if true serialized data will be GZIPed
 */
fun <T: Serializable> T.serialize(directed: Boolean, compress: Boolean): ByteBuffer {
//    TODO: code for serialization verification, make it configurable
//    val stream1 = DummyOutputStream(verifiable = true).apply { serialize(this, compress) }
//    val stream2 = DummyOutputStream(stream1, verifiable = true).apply { serialize(this, compress) }
    val size = calcObjectSize(compress)
    return BufferOutputStream(size, directed).apply { serialize(this, compress) }.buffer
}

/**
 * Deserializes object from [this] stream
 */
@Suppress("UNCHECKED_CAST")
fun <T: Serializable, S: InputStream> S.deserialize(compress: Boolean) =
    ObjectInputStream(gzip(compress)).readUnshared() as T

/**
 * Deserializes object from [this] bytes
 */
fun <T: Serializable> ByteArray.deserialize(compress: Boolean): T = inputStream().deserialize(compress)
