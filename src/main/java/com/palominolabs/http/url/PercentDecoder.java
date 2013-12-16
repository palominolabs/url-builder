package com.palominolabs.http.url;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

@NotThreadSafe
final class PercentDecoder {

    /**
     * Contains the bytes represented by the current few %-triples
     */
    private ByteBuffer encodedBuf;

    /**
     * Written to with decoded chars
     */
    private final CharBuffer decodedCharBuf;
    private final CharsetDecoder decoder;

    private final StringBuilder buf = new StringBuilder();

    PercentDecoder(Charset charset) {
        CharsetEncoder encoder = charset.newEncoder();
        encodedBuf = ByteBuffer.allocate(16);
        decodedCharBuf = CharBuffer.allocate(16);
        decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    String percentDecode(CharSequence encoded) {
        buf.setLength(0);
        // this is almost always an underestimate of the size needed:
        // only a 4-byte encoding (which is 12 characters encoded) would case this to be an overestimate
        buf.ensureCapacity(encoded.length() / 8);

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != '%') {
                buf.append(c);

                if (encodedBuf.position() == 0) {
                    // this is NOT the first non-%-triple input char, so we didn't just finish an encoded sequence
                    continue;
                }

                // if there's anything to decode, then decode it
                CoderResult coderResult = decoder.decode(encodedBuf, decodedCharBuf, true);
                if (coderResult == CoderResult.UNDERFLOW) {
                    // we're done, but still have to flush
                    decodedCharBuf.flip();
                    buf.append(decodedCharBuf);
                    decodedCharBuf.clear();
                    CoderResult flushResult = decoder.flush(decodedCharBuf);
                    if (flushResult != CoderResult.UNDERFLOW) {
                        throw new IllegalStateException("Got flush result " + flushResult);
                    }
                    decodedCharBuf.flip();
                    buf.append(decodedCharBuf);
                    decodedCharBuf.clear();
                } else if (coderResult == CoderResult.OVERFLOW) {
                    decodedCharBuf.flip();
                    buf.append(decodedCharBuf);
                    decodedCharBuf.clear();
                } else {
                    throw new IllegalStateException("Got unexpected coder result " + coderResult);
                }

                checkResult(coderResult);
            }

            if (i + 2 >= encoded.length()) {
                throw new IllegalArgumentException(
                    "Could not percent decode <" + encoded + ">: invalid %-pair at position " + i);
            }

            // grow the byte buf if needed
            if (encodedBuf.remaining() == 0) {
                ByteBuffer largerBuf = ByteBuffer.allocate(encodedBuf.capacity() * 2);
                encodedBuf.flip();
                largerBuf.put(encodedBuf);
                encodedBuf = largerBuf;
            }

            int msBits = Character.digit(encoded.charAt(++i), 16);
            int lsBits = Character.digit(encoded.charAt(++i), 16);

            msBits <<= 4;
            msBits |= lsBits;

            // msBits can only have 8 bits set, so cast is safe
            encodedBuf.put((byte) msBits);
        }

        return buf.toString();
    }

    private static void checkResult(CoderResult result) {
        if (result.isOverflow()) {
            throw new IllegalStateException("Somehow got byte buffer overflow");
        }
    }
}
