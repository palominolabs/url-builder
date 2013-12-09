package com.palominolabs.http.url;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

@NotThreadSafe
final class PercentDecoder {

    private final ByteBuffer encodedBuf;

    private final CharBuffer decodedCharBuf;
    private final CharsetDecoder decoder;

    PercentDecoder(Charset charset) {
        encodedBuf = ByteBuffer.allocate(1 + (int) charset.newEncoder().maxBytesPerChar());
        decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        decodedCharBuf =
            CharBuffer.allocate((1 + (int) decoder.maxCharsPerByte()) * encodedBuf.capacity());
    }

    private String percentDecode(CharSequence encoded, CharBuffer dest, Charset charset, ByteBuffer decodeBuf) {
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != '%') {
                dest.append(c);
                continue;
            }

            // read as many percent-encoded triples as there are, up to max bytes per char
            encodedBuf.reset();
            decodedCharBuf.reset();

            if (i + 2 >= encoded.length()) {
                throw new IllegalArgumentException(
                    "Could not percent decode <" + encoded + ">: invalid %-pair at position " + i);
            }

            int msBits = Character.digit(encoded.charAt(i + 1), 16);
            int lsBits = Character.digit(encoded.charAt(i + 2), 16);

            msBits <<= 4;
            msBits &= lsBits;

            // msBits can only have 8 bits set, so cast is safe
            if (PercentEncoder.isHighSurrogate((char) msBits)) {

            }
        }
    }
}
