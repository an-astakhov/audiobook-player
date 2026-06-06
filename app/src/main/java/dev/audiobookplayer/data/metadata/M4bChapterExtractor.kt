package dev.audiobookplayer.data.metadata

import android.content.ContentResolver
import android.net.Uri
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M4bChapterExtractor(
    private val contentResolver: ContentResolver,
) {
    suspend fun extract(uri: Uri): List<ImportedChapterMetadata> = withContext(Dispatchers.IO) {
        contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
            FileInputStream(parcelFileDescriptor.fileDescriptor).channel.use { channel ->
                ChapterMp4Reader(channel).extractChapters()
            }
        } ?: emptyList()
    }

    private class ChapterMp4Reader(
        private val channel: FileChannel,
    ) {
        private val fileSize = channel.size()

        fun extractChapters(): List<ImportedChapterMetadata> {
            val moov = findChild(
                parent = AtomRange(0L, fileSize),
                type = "moov",
            ) ?: return emptyList()
            val tracks = readTracks(moov)
            val audioTrack = tracks.firstOrNull { it.handlerType == "soun" && it.chapterTrackIds.isNotEmpty() }
                ?: return emptyList()
            val chapterTrack = audioTrack.chapterTrackIds
                .asSequence()
                .mapNotNull { chapterTrackId -> tracks.firstOrNull { it.trackId == chapterTrackId } }
                .firstOrNull { it.handlerType == "text" }
                ?: return emptyList()

            return chapterTrack.toImportedChapters()
        }

        private fun readTracks(moov: Atom): List<TrackInfo> {
            return findChildren(moov.contentRange, "trak").mapNotNull { trak ->
                val tkhd = findChild(trak.contentRange, "tkhd") ?: return@mapNotNull null
                val mdia = findChild(trak.contentRange, "mdia") ?: return@mapNotNull null
                val mdhd = findChild(mdia.contentRange, "mdhd") ?: return@mapNotNull null
                val hdlr = findChild(mdia.contentRange, "hdlr") ?: return@mapNotNull null
                val minf = findChild(mdia.contentRange, "minf") ?: return@mapNotNull null
                val stbl = findChild(minf.contentRange, "stbl") ?: return@mapNotNull null
                val tref = findChild(trak.contentRange, "tref")

                TrackInfo(
                    trackId = readTrackId(tkhd),
                    handlerType = readHandlerType(hdlr),
                    timescale = readTimescale(mdhd),
                    chapterTrackIds = tref?.let(::readChapterTrackIds).orEmpty(),
                    stbl = stbl,
                )
            }
        }

        private fun TrackInfo.toImportedChapters(): List<ImportedChapterMetadata> {
            if (timescale <= 0L) return emptyList()

            val sampleDurations = readSampleDurations(stbl, timescale)
            val sampleSizes = readSampleSizes(stbl)
            val sampleOffsets = readSampleOffsets(stbl, sampleSizes)
            val sampleCount = min(sampleDurations.size, min(sampleSizes.size, sampleOffsets.size))
            if (sampleCount == 0) return emptyList()

            val chapters = ArrayList<ImportedChapterMetadata>(sampleCount)
            var currentStartMs = 0L
            repeat(sampleCount) { index ->
                val sampleBytes = readBytes(sampleOffsets[index], sampleSizes[index].toInt())
                val title = decodeChapterTitle(sampleBytes)
                    .ifBlank { "Chapter ${index + 1}" }
                chapters += ImportedChapterMetadata(
                    title = title,
                    startPositionMs = currentStartMs,
                )
                currentStartMs += sampleDurations[index]
            }
            return chapters
        }

        private fun readSampleDurations(
            stbl: Atom,
            timescale: Long,
        ): List<Long> {
            val stts = findChild(stbl.contentRange, "stts") ?: return emptyList()
            val entryCount = readInt(stts.contentStart + 4L)
            var cursor = stts.contentStart + 8L
            val durations = mutableListOf<Long>()

            repeat(entryCount) {
                val sampleCount = readInt(cursor)
                val sampleDelta = readInt(cursor + 4L).toLong()
                repeat(sampleCount) {
                    durations += scaleToMillis(sampleDelta, timescale)
                }
                cursor += 8L
            }
            return durations
        }

        private fun readSampleSizes(stbl: Atom): List<Int> {
            val stsz = findChild(stbl.contentRange, "stsz") ?: return emptyList()
            val defaultSampleSize = readInt(stsz.contentStart + 4L)
            val sampleCount = readInt(stsz.contentStart + 8L)
            if (sampleCount <= 0) return emptyList()

            if (defaultSampleSize > 0) {
                return List(sampleCount) { defaultSampleSize }
            }

            var cursor = stsz.contentStart + 12L
            return List(sampleCount) {
                val sampleSize = readInt(cursor)
                cursor += 4L
                sampleSize
            }
        }

        private fun readSampleOffsets(
            stbl: Atom,
            sampleSizes: List<Int>,
        ): List<Long> {
            val chunkOffsets = readChunkOffsets(stbl)
            val stsc = findChild(stbl.contentRange, "stsc") ?: return emptyList()
            if (chunkOffsets.isEmpty()) return emptyList()

            val entries = readSampleToChunkEntries(stsc)
            if (entries.isEmpty()) return emptyList()

            val sampleOffsets = ArrayList<Long>(sampleSizes.size)
            var sampleIndex = 0

            entries.forEachIndexed { index, entry ->
                val lastChunk = if (index + 1 < entries.size) {
                    entries[index + 1].firstChunk - 1
                } else {
                    chunkOffsets.size
                }

                for (chunkNumber in entry.firstChunk..lastChunk) {
                    if (chunkNumber <= 0 || chunkNumber > chunkOffsets.size) continue

                    var chunkSampleOffset = chunkOffsets[chunkNumber - 1]
                    repeat(entry.samplesPerChunk) {
                        if (sampleIndex >= sampleSizes.size) return sampleOffsets
                        sampleOffsets += chunkSampleOffset
                        chunkSampleOffset += sampleSizes[sampleIndex].toLong()
                        sampleIndex += 1
                    }
                }
            }

            return sampleOffsets
        }

        private fun readChunkOffsets(stbl: Atom): List<Long> {
            val stco = findChild(stbl.contentRange, "stco")
            if (stco != null) {
                val entryCount = readInt(stco.contentStart + 4L)
                var cursor = stco.contentStart + 8L
                return List(entryCount) {
                    val offset = readInt(cursor).toLong() and 0xffffffffL
                    cursor += 4L
                    offset
                }
            }

            val co64 = findChild(stbl.contentRange, "co64") ?: return emptyList()
            val entryCount = readInt(co64.contentStart + 4L)
            var cursor = co64.contentStart + 8L
            return List(entryCount) {
                val offset = readLong(cursor)
                cursor += 8L
                offset
            }
        }

        private fun readSampleToChunkEntries(stsc: Atom): List<SampleToChunkEntry> {
            val entryCount = readInt(stsc.contentStart + 4L)
            var cursor = stsc.contentStart + 8L
            return List(entryCount) {
                val entry = SampleToChunkEntry(
                    firstChunk = readInt(cursor),
                    samplesPerChunk = readInt(cursor + 4L),
                )
                cursor += 12L
                entry
            }
        }

        private fun readChapterTrackIds(tref: Atom): List<Int> {
            val chapterReference = findChild(tref.contentRange, "chap") ?: return emptyList()
            val ids = mutableListOf<Int>()
            var cursor = chapterReference.contentStart
            while (cursor + 4L <= chapterReference.endOffset) {
                ids += readInt(cursor)
                cursor += 4L
            }
            return ids
        }

        private fun readTrackId(tkhd: Atom): Int {
            val version = readByte(tkhd.contentStart)
            val trackIdOffset = if (version == 1) {
                tkhd.contentStart + 20L
            } else {
                tkhd.contentStart + 12L
            }
            return readInt(trackIdOffset)
        }

        private fun readTimescale(mdhd: Atom): Long {
            val version = readByte(mdhd.contentStart)
            val timescaleOffset = if (version == 1) {
                mdhd.contentStart + 16L
            } else {
                mdhd.contentStart + 12L
            }
            return readInt(timescaleOffset).toLong() and 0xffffffffL
        }

        private fun readHandlerType(hdlr: Atom): String {
            return readAscii(hdlr.contentStart + 8L, 4)
        }

        private fun findChild(
            parent: AtomRange,
            type: String,
        ): Atom? {
            return iterateChildren(parent).firstOrNull { it.type == type }
        }

        private fun findChildren(
            parent: AtomRange,
            type: String,
        ): List<Atom> {
            return iterateChildren(parent).filter { it.type == type }
        }

        private fun iterateChildren(parent: AtomRange): List<Atom> {
            val atoms = mutableListOf<Atom>()
            var offset = parent.startOffset
            while (offset + ATOM_HEADER_BYTES <= parent.endOffset) {
                val atom = readAtom(offset, parent.endOffset) ?: break
                atoms += atom
                offset = atom.endOffset
            }
            return atoms
        }

        private fun readAtom(
            offset: Long,
            maxOffset: Long,
        ): Atom? {
            if (offset + ATOM_HEADER_BYTES > maxOffset) return null

            val headerBuffer = readBuffer(offset, ATOM_HEADER_BYTES.toInt())
            val rawSize = headerBuffer.int.toLong() and 0xffffffffL
            val type = ByteArray(4).also(headerBuffer::get).decodeToString()
            var size = rawSize
            var headerSize = ATOM_HEADER_BYTES

            if (rawSize == 1L) {
                if (offset + 16L > maxOffset) return null
                size = readLong(offset + 8L)
                headerSize = 16L
            } else if (rawSize == 0L) {
                size = maxOffset - offset
            }

            if (size < headerSize) return null
            val endOffset = offset + size
            if (endOffset > maxOffset) return null

            return Atom(
                type = type,
                startOffset = offset,
                size = size,
                headerSize = headerSize,
            )
        }

        private fun readInt(offset: Long): Int {
            return readBuffer(offset, 4).int
        }

        private fun readLong(offset: Long): Long {
            return readBuffer(offset, 8).long
        }

        private fun readByte(offset: Long): Int {
            return readBuffer(offset, 1).get().toInt() and 0xff
        }

        private fun readAscii(
            offset: Long,
            byteCount: Int,
        ): String {
            return readBytes(offset, byteCount).decodeToString()
        }

        private fun readBytes(
            offset: Long,
            byteCount: Int,
        ): ByteArray {
            if (byteCount <= 0) return ByteArray(0)
            val buffer = ByteBuffer.allocate(byteCount)
            channel.readFully(buffer, offset)
            return buffer.array()
        }

        private fun readBuffer(
            offset: Long,
            byteCount: Int,
        ): ByteBuffer {
            val buffer = ByteBuffer.allocate(byteCount).order(ByteOrder.BIG_ENDIAN)
            channel.readFully(buffer, offset)
            buffer.flip()
            return buffer
        }

        private fun scaleToMillis(
            delta: Long,
            timescale: Long,
        ): Long {
            return (delta * MILLIS_PER_SECOND) / timescale
        }

        private fun decodeChapterTitle(sampleBytes: ByteArray): String {
            if (sampleBytes.isEmpty()) return ""

            if (sampleBytes.size >= 2) {
                val declaredLength = ((sampleBytes[0].toInt() and 0xff) shl 8) or
                    (sampleBytes[1].toInt() and 0xff)
                if (declaredLength > 0 && declaredLength <= sampleBytes.size - 2) {
                    return sampleBytes.decodeToString(
                        startIndex = 2,
                        endIndex = 2 + declaredLength,
                    ).trim()
                }
            }

            val payloadStart = if (sampleBytes.size > 2) 2 else 0
            val payload = sampleBytes.copyOfRange(payloadStart, sampleBytes.size)
            val endIndex = payload.indexOfFirst { it == 0.toByte() }.let { zeroIndex ->
                if (zeroIndex >= 0) zeroIndex else payload.size
            }
            return payload.decodeToString(endIndex = endIndex).trim()
        }

        private data class TrackInfo(
            val trackId: Int,
            val handlerType: String,
            val timescale: Long,
            val chapterTrackIds: List<Int>,
            val stbl: Atom,
        )

        private data class Atom(
            val type: String,
            val startOffset: Long,
            val size: Long,
            val headerSize: Long,
        ) {
            val contentStart: Long
                get() = startOffset + headerSize

            val endOffset: Long
                get() = startOffset + size

            val contentRange: AtomRange
                get() = AtomRange(contentStart, endOffset)
        }

        private data class AtomRange(
            val startOffset: Long,
            val endOffset: Long,
        )

        private data class SampleToChunkEntry(
            val firstChunk: Int,
            val samplesPerChunk: Int,
        )

        private companion object {
            const val ATOM_HEADER_BYTES = 8L
            const val MILLIS_PER_SECOND = 1_000L
        }
    }
}

private fun FileChannel.readFully(
    buffer: ByteBuffer,
    position: Long,
) {
    var totalRead = 0
    while (buffer.hasRemaining()) {
        val read = read(buffer, position + totalRead)
        if (read < 0) {
            throw IllegalStateException("Unexpected end of MP4 stream while reading chapter metadata.")
        }
        totalRead += read
    }
}
