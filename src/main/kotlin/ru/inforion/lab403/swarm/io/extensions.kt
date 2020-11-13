package ru.inforion.lab403.swarm.io

import java.io.*
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * {RU}
 * Обернуть выходной поток в [GZIPOutputStream], если включено сжатие данных
 *
 * @param enabled включено или нет сжатие
 * {RU}
 */
private fun OutputStream.gzip(enabled: Boolean) = if (enabled) GZIPOutputStream(this) else this

/**
 * {RU}
 * Обернуть входной поток в [GZIPInputStream], если включено сжатие данных
 *
 * @param enabled включено или нет сжатие
 * {RU}
 */
private fun InputStream.gzip(enabled: Boolean) = if (enabled) GZIPInputStream(this) else this

/**
 * {RU}
 * Сериализовать объект в заданный поток
 *
 * @param output поток, в который будет сериализован объект
 * @param compress сжимать или нет сериализованные данные
 *
 * ВНИМАНИЕ: этот метод закрывает входной поток, поэтому после использования метода
 *   запись в поток больше недопустима. Это связано с использованием возможности сжатия
 *   с помощью GZIP-потока. Для корректной записи данных GZIP-потоком необходим вызов
 *   метода [GZIPOutputStream.finish], который может быть вызван в методе [OutputStream.close]
 * {RU}
 */
private fun <T: Serializable> T.serialize(output: OutputStream, compress: Boolean) =
    output.gzip(compress).use { ObjectOutputStream(it).writeUnshared(this) }

/**
 * {RU}
 * Вычислить размер объекта после сериализации путем его фиктивной "записи" в массив
 * В реальности запись данных в память не происходит, а только смещается указатель в потоке [DummyOutputStream]
 *
 * @param compress вычислить размер данных с учетом того, что будет использовано сжатие или нет
 * {RU}
 */
fun <T: Serializable> T.calcObjectSize(compress: Boolean) =
    DummyOutputStream().apply { serialize(this, compress) }.written

/**
 * {RU}
 * Сериализовать объект в буфер. В качестве того, куда будет сериализоваться объект выбран именно [ByteBuffer]
 * для того, чтобы его можно было передавать в нативные библиотеки, например в [MPI].
 *
 * @param directed возможна ли передача буфера в нативные библиотеки
 * @param compress сжимать или нет сериализованные данные
 * {RU}
 */
fun <T: Serializable> T.serialize(directed: Boolean, compress: Boolean): ByteBuffer {
//    TODO: code for serialization verification, make it configurable
//    val stream1 = DummyOutputStream(verifiable = true).apply { serialize(this, compress) }
//    val stream2 = DummyOutputStream(stream1, verifiable = true).apply { serialize(this, compress) }
    val size = calcObjectSize(compress)
    return BufferOutputStream(size, directed).apply { serialize(this, compress) }.buffer
}

/**
 * {RU}
 * Десериализует объект из потока [InputStream]
 * {RU}
 */
@Suppress("UNCHECKED_CAST")
fun <T: Serializable, S: InputStream> S.deserialize(compress: Boolean) =
    ObjectInputStream(gzip(compress)).readUnshared() as T

/**
 * {RU}
 * Десериализует объект из массива [ByteArray]
 * {RU}
 */
fun <T: Serializable> ByteArray.deserialize(compress: Boolean): T = inputStream().deserialize(compress)
