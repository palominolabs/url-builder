/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.BitSet;

import static java.lang.Character.isHighSurrogate;
import static java.lang.Character.isLowSurrogate;

/**
 * Encodes unsafe characters as a sequence of %XX hex-encoded bytes.
 *
 * This is typically done when encoding components of URLs. See {@link UrlPercentEncoders} for pre-configured
 * PercentEncoder instances.
 */
@NotThreadSafe
public final class PercentEncoder {

    private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();

    private final BitSet safeChars;
    private final CharsetEncoder encoder;
    /**
     * Pre-allocate a string handler to make the common case of encoding to a string faster
     */
    private final StringBuilderPercentEncoderHandler stringHandler = new StringBuilderPercentEncoderHandler();
    private final ByteBuffer encodedBytes;
    private final CharBuffer unsafeCharsToEncode;

    /**
     * @param safeChars      the set of chars to NOT encode, stored as a bitset with the int positions corresponding to
     *                       those chars set to true. Treated as read only.
     * @param charsetEncoder charset encoder to encode characters with. Make sure to not re-use CharsetEncoder instances
     *                       across threads.
     */
    public PercentEncoder(@Nonnull BitSet safeChars, @Nonnull CharsetEncoder charsetEncoder) {
        this.safeChars = safeChars;
        this.encoder = charsetEncoder;

        // why is this a float? sigh.
        int maxBytesPerChar = 1 + (int) encoder.maxBytesPerChar();
        // need to handle surrogate pairs, so need to be able to handle 2 chars worth of stuff at once
        encodedBytes = ByteBuffer.allocate(maxBytesPerChar * 2);
        unsafeCharsToEncode = CharBuffer.allocate(2);
    }

    public void encode(@Nonnull CharSequence input, @Nonnull PercentEncoderHandler handler) throws
        MalformedInputException, UnmappableCharacterException {

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            if (safeChars.get(c)) {
                handler.onOutputChar(c);
                continue;
            }

            // not a safe char
            unsafeCharsToEncode.clear();
            unsafeCharsToEncode.append(c);
            if (isHighSurrogate(c)) {
                if (input.length() > i + 1) {
                    // get the low surrogate as well
                    char lowSurrogate = input.charAt(i + 1);
                    if (isLowSurrogate(lowSurrogate)) {
                        unsafeCharsToEncode.append(lowSurrogate);
                        i++;
                    } else {
                        throw new IllegalArgumentException(
                            "Invalid UTF-16: Char " + (i) + " is a high surrogate (\\u" + Integer
                                .toHexString(c) + "), but char " + (i + 1) + " is not a low surrogate (\\u" + Integer
                                .toHexString(lowSurrogate) + ")");
                    }
                } else {
                    throw new IllegalArgumentException(
                        "Invalid UTF-16: The last character in the input string was a high surrogate (\\u" + Integer
                            .toHexString(c) + ")");
                }
            }

            flushUnsafeCharBuffer(handler);
        }
    }

    /**
     * @param input input string
     * @return the input string with every character that's not in safeChars turned into its byte representation via the
     * instance's encoder and then percent-encoded
     * @throws MalformedInputException      if encoder is configured to report errors and malformed input is detected
     * @throws UnmappableCharacterException if encoder is configured to report errors and an unmappable character is
     *                                      detected
     */
    @Nonnull
    public String encode(@Nonnull CharSequence input) throws MalformedInputException, UnmappableCharacterException {
        stringHandler.reset();
        stringHandler.ensureCapacity(input.length());
        encode(input, stringHandler);
        return stringHandler.getContents();
    }

    /**
     * Encode unsafeCharsToEncode to bytes as per charsetEncoder, then percent-encode those bytes into output.
     *
     * Side effects: unsafeCharsToEncode will be read from and cleared. encodedBytes will be cleared and written to.
     *
     * @param handler where the encoded versions of the contents of unsafeCharsToEncode will be written
     */
    private void flushUnsafeCharBuffer(PercentEncoderHandler handler) throws MalformedInputException,
        UnmappableCharacterException {
        // need to read from the char buffer, which was most recently written to
        unsafeCharsToEncode.flip();

        encodedBytes.clear();

        encoder.reset();
        CoderResult result = encoder.encode(unsafeCharsToEncode, encodedBytes, true);
        checkResult(result);
        result = encoder.flush(encodedBytes);
        checkResult(result);

        // read contents of bytebuffer
        encodedBytes.flip();

        while (encodedBytes.hasRemaining()) {
            byte b = encodedBytes.get();

            handler.onOutputChar('%');
            handler.onOutputChar(HEX_CODE[b >> 4 & 0xF]);
            handler.onOutputChar(HEX_CODE[b & 0xF]);
        }
    }

    /**
     * @param result result to check
     * @throws IllegalStateException        if result is overflow
     * @throws MalformedInputException      if result represents malformed input
     * @throws UnmappableCharacterException if result represents an unmappable character
     */
    private static void checkResult(CoderResult result) throws MalformedInputException, UnmappableCharacterException {
        if (result.isOverflow()) {
            throw new IllegalStateException("Byte buffer overflow; this should not happen.");
        }
        if (result.isMalformed()) {
            throw new MalformedInputException(result.length());
        }
        if (result.isUnmappable()) {
            throw new UnmappableCharacterException(result.length());
        }
    }
}
