/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.BitSet;

/**
 * Encodes characters that aren't in the specified safe set as a sequence of %(hex) encoded bytes as determined by the
 * specified charset.
 */
@NotThreadSafe
public final class PercentEncoder {

    private static final Logger logger = LoggerFactory.getLogger(PercentEncoder.class);

    /**
     * Amount to add to a lowercase ascii alpha char to make it an uppercase. Used for hex encoding.
     */
    private static final int UPPER_CASE_DIFF = 'a' - 'A';

    private final BitSet safeChars;
    private final CharsetEncoder encoder;

    /**
     * @param safeChars the set of chars to NOT encode, stored as a bitset with the int positions corresponding to those
     *                  chars set to true. Treated as read only.
     * @param charset   charset to use to encode unsafe chars into bytes
     */
    PercentEncoder(BitSet safeChars, Charset charset) {
        this.safeChars = safeChars;
        // TODO perhaps allow this policy to be customized?
        this.encoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    /**
     * @param input input string
     * @return the input string with every character that's not in safeChars turned into its byte representation via
     * encoder and then percent-encoded
     */
    public String encode(String input) {
        // output buf will be at least as long as the input
        StringBuilder stringBuilder = new StringBuilder(input.length());

        // need to handle surrogate pairs, so need to be able to handle 2 chars worth of stuff at once

        // why is this a float? sigh.
        int maxBytes = 1 + (int) encoder.maxBytesPerChar();
        ByteBuffer byteBuffer = ByteBuffer.allocate(maxBytes * 2);
        CharBuffer charBuffer = CharBuffer.allocate(2);

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            if (safeChars.get(c)) {
                stringBuilder.append(c);
                continue;
            }

            // not a safe char
            charBuffer.clear();
            byteBuffer.clear();
            charBuffer.append(c);
            if (isHighSurrogate(c)) {
                if (input.length() > i + 1) {
                    // get the low surrogate as well
                    char lowSurrogate = input.charAt(i + 1);
                    if (isLowSurrogate(lowSurrogate)) {
                        charBuffer.append(lowSurrogate);
                        i++;
                    } else {
                        // TODO what to do for high surrogate followed by non-low-surrogate?
                        logger.warn(
                            "Invalid UTF-16: Char " + (i - 1) + " was a high surrogate (\\u" + Integer
                                .toHexString(c) + ", but char " + i + " was not a low surrogate (\\u" + Integer
                                .toHexString(lowSurrogate));
                    }
                } else {
                    // TODO what to do for high surrogoate with no low surrogate?
                    logger.warn(
                        "Invalid UTF-16: The last character in the input string was a high surrogate (\\u" + Integer
                            .toHexString(c) + ")");
                }
            }
            addEncodedChars(stringBuilder, byteBuffer, charBuffer, encoder);
        }

        return stringBuilder.toString();
    }

    static boolean isHighSurrogate(char c) {
        return c >= 0xD800 && c <= 0xDBFF;
    }

    static boolean isLowSurrogate(char c) {
        return c >= 0xDC00 && c <= 0xDFFF;
    }

    /**
     * Encode c to bytes as per charsetEncoder, then percent-encode those bytes into output.
     *
     * @param output         where the encoded versions of the contents of charBuffer will be written
     * @param byteBuffer     encoded chars buffer in write state. Will be written to, flipped, and fully read from.
     * @param charBuffer     unencoded chars buffer containing one or two chars in write mode. Will be flipped to and
     *                       fully read from.
     * @param charsetEncoder encoder
     */
    private static void addEncodedChars(StringBuilder output, ByteBuffer byteBuffer, CharBuffer charBuffer,
        CharsetEncoder charsetEncoder) {
        // need to read from the char buffer, which was most recently written to
        charBuffer.flip();

        charsetEncoder.reset();
        CoderResult result = charsetEncoder.encode(charBuffer, byteBuffer, true);
        checkResult(result);
        result = charsetEncoder.flush(byteBuffer);
        checkResult(result);

        // read contents of bytebuffer
        byteBuffer.flip();

        while (byteBuffer.hasRemaining()) {
            byte b = byteBuffer.get();

            int msbits = (b >> 4) & 0xF;
            int lsbits = b & 0xF;

            char msbitsChar = Character.forDigit(msbits, 16);
            char lsbitsChar = Character.forDigit(lsbits, 16);

            output.append('%');
            output.append(capitalizeIfLetter(msbitsChar));
            output.append(capitalizeIfLetter(lsbitsChar));
        }
    }

    private static void checkResult(CoderResult result) {
        if (result.isOverflow()) {
            throw new IllegalStateException("Somehow got byte buffer overflow");
        }
    }

    /**
     * @param c ascii lowercase alphabetic or numeric char
     * @return char uppercased if a letter
     */
    private static char capitalizeIfLetter(char c) {
        if (Character.isLetter(c)) {
            return (char) (c - UPPER_CASE_DIFF);
        }

        return c;
    }
}
