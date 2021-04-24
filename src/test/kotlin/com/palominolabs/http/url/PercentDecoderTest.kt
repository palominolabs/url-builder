package com.palominolabs.http.url

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Character.isHighSurrogate
import java.lang.Character.isLowSurrogate
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Random
import kotlin.streams.asSequence
import kotlin.test.assertEquals
import kotlin.test.fail

class PercentDecoderTest {
    private lateinit var decoder: PercentDecoder

    @BeforeEach
    fun setUp() {
        decoder = PercentDecoder(UTF_8.newDecoder())
    }

    @Test
    fun testDecodesWithoutPercents() {
        assertEquals("asdf", decoder.decode("asdf"))
    }

    @Test
    fun testDecodeSingleByte() {
        assertEquals("#", decoder.decode("%23"))
    }

    @Test
    fun testIncompletePercentPairNoNumbers() {
        val e = assertThrows<IllegalArgumentException> { decoder.decode("%") }
        assertEquals("Could not percent decode <%>: incomplete %-pair at position 0", e.message)
    }

    @Test
    fun testIncompletePercentPairOneNumber() {
        val e = assertThrows<IllegalArgumentException> { decoder.decode("%2") }
        assertEquals("Could not percent decode <%2>: incomplete %-pair at position 0", e.message)
    }

    @Test
    fun testInvalidHex() {
        val e = assertThrows<IllegalArgumentException> { decoder.decode("%xz") }
        assertEquals("Invalid %-tuple <%xz>", e.message)
    }

    @Test
    fun testRandomStrings() {
        val encoder = UrlPercentEncoders.getUnstructuredQueryEncoder()
        val rand = Random()

        val seed = rand.nextLong()
        rand.setSeed(seed)

        val charBuf = CharArray(2)
        val codePoints = mutableListOf<Int>()
        val buf = StringBuilder()

        repeat(10_000) {
            buf.setLength(0)
            codePoints.clear()

            randString(buf, codePoints, charBuf, rand, 1 + rand.nextInt(1000))

            val origBytes = buf.toString().encodeToByteArray()
            val codePointsHex = codePoints.map { i -> Integer.toHexString(i) }

            val decodedBytes =
                try {
                    decoder.decode(encoder.encode(buf.toString())).encodeToByteArray()
                } catch (e: IllegalArgumentException) {
                    val charHex = buf.toString()
                        .chars()
                        .asSequence()
                        .map { Integer.toHexString(it) }
                        .toList()
                    fail("seed: $seed code points: $codePointsHex chars $charHex $e.message")
                }

            assertEquals(toHex(origBytes), toHex(decodedBytes), "Seed: $seed Code points: $codePointsHex")
        }
    }

    /**
     * Generate a random string
     * @param buf buffer to write into
     * @param codePoints list of code points to write into
     * @param charBuf char buf for temporary char wrangling (size 2)
     * @param rand random source
     * @param length max string length
     */
    private fun randString(
        buf: StringBuilder,
        codePoints: MutableList<Int>,
        charBuf: CharArray,
        rand: Random,
        length: Int
    ) {
        while (buf.length < length) {
            // pick something in the range of all 17 unicode planes
            val codePoint = rand.nextInt(17 * 65536)
            if (Character.isDefined(codePoint)) {
                val res = Character.toChars(codePoint, charBuf, 0)

                if (res == CODE_POINT_IN_BMP && (isHighSurrogate(charBuf[0]) || isLowSurrogate(charBuf[0]))) {
                    // isDefined is true even if it's a standalone surrogate in the D800-DFFF range, but those are not legal
                    // single unicode code units (that is, a single char)
                    continue
                }

                buf.append(charBuf[0])
                // whether it's a pair or not, we want the only char (or high surrogate)
                codePoints.add(codePoint)
                if (res == CODE_POINT_IN_SUPPLEMENTARY) {
                    // it's a surrogate pair, so we care about the second char
                    buf.append(charBuf[1])
                }
            }
        }
    }
}

/**
 * @param bytes
 * @return list of hex strings
 */
private fun toHex(bytes: ByteArray): List<String> = bytes.map { Integer.toHexString(it.toInt().and(0xFF)) }

private const val CODE_POINT_IN_SUPPLEMENTARY = 2
private const val CODE_POINT_IN_BMP = 1
