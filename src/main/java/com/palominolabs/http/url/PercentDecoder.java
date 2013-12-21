package com.palominolabs.http.url;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import static java.nio.charset.CoderResult.OVERFLOW;
import static java.nio.charset.CoderResult.UNDERFLOW;

@NotThreadSafe
final class PercentDecoder {

    /**
     * bytes represented by the current sequence of %-triples
     */
    private ByteBuffer encodedBuf;

    /**
     * Written to with decoded chars by decoder
     */
    private final CharBuffer decodedCharBuf;
    private final CharsetDecoder decoder;

    /**
     * The decoded string for the current input
     */
    private final StringBuilder outputBuf = new StringBuilder();

    /**
     * Construct a new PercentDecoder with default buffer sizes.
     *
     * @param charsetDecoder Charset to decode bytes into chars with
     */
    PercentDecoder(@Nonnull CharsetDecoder charsetDecoder) {
        this(charsetDecoder, 16, 16);
    }

    /**
     * @param charsetDecoder            Charset to decode bytes into chars with
     * @param initialEncodedByteBufSize Initial size of buffer that holds encoded bytes
     * @param decodedCharBufSize        Size of buffer that encoded bytes are decoded into
     */
    PercentDecoder(@Nonnull CharsetDecoder charsetDecoder, int initialEncodedByteBufSize, int decodedCharBufSize) {
        encodedBuf = ByteBuffer.allocate(initialEncodedByteBufSize);
        decodedCharBuf = CharBuffer.allocate(decodedCharBufSize);
        decoder = charsetDecoder;
    }

    /**
     * @param input Input with %-encoded representation of characters in this instance's configured character set
     * @return Corresponding string with %-encoded data decoded and converted to their corresponding characters
     * @throws CharacterCodingException if character decoding fails
     */
    @Nonnull
    String decode(@Nonnull CharSequence input) throws CharacterCodingException {
        outputBuf.setLength(0);
        // this is almost always an underestimate of the size needed:
        // only a 4-byte encoding (which is 12 characters input) would case this to be an overestimate
        outputBuf.ensureCapacity(input.length() / 8);
        encodedBuf.clear();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c != '%') {
                handleEncodedBytes();

                outputBuf.append(c);
                continue;
            }

            if (i + 2 >= input.length()) {
                throw new IllegalArgumentException(
                    "Could not percent decode <" + input + ">: incomplete %-pair at position " + i);
            }

            // grow the byte buf if needed
            if (encodedBuf.remaining() == 0) {
                ByteBuffer largerBuf = ByteBuffer.allocate(encodedBuf.capacity() * 2);
                encodedBuf.flip();
                largerBuf.put(encodedBuf);
                encodedBuf = largerBuf;
            }

            // note that we advance i here as we consume chars
            int msBits = Character.digit(input.charAt(++i), 16);
            int lsBits = Character.digit(input.charAt(++i), 16);

            if (msBits == -1 || lsBits == -1) {
                throw new IllegalArgumentException("Invalid %-tuple <" + input.subSequence(i - 2, i + 1) + ">");
            }

            msBits <<= 4;
            msBits |= lsBits;

            // msBits can only have 8 bits set, so cast is safe
            encodedBuf.put((byte) msBits);
        }

        handleEncodedBytes();

        return outputBuf.toString();
    }

    /**
     * Decode any buffered encoded bytes and write them to the output buf.
     */
    private void handleEncodedBytes() throws CharacterCodingException {
        if (encodedBuf.position() == 0) {
            // nothing to do
            return;
        }

        decoder.reset();
        CoderResult coderResult;

        // switch to reading mode
        encodedBuf.flip();

        // loop while we're filling up the decoded char buf, or there's any encoded bytes
        // decode() in practice seems to only consume bytes when it can decode an entire char...
        do {
            decodedCharBuf.clear();
            coderResult = decoder.decode(encodedBuf, decodedCharBuf, false);
            throwIfError(coderResult);
            appendDecodedChars();
        } while (coderResult == OVERFLOW && encodedBuf.hasRemaining());

        // final decode with end-of-input flag
        decodedCharBuf.clear();
        coderResult = decoder.decode(encodedBuf, decodedCharBuf, true);
        throwIfError(coderResult);

        if (encodedBuf.hasRemaining()) {
            throw new IllegalStateException("Final decode didn't error, but didn't consume remaining input bytes");
        }
        if (coderResult != UNDERFLOW) {
            throw new IllegalStateException("Expected underflow, but instead final decode returned " + coderResult);
        }

        appendDecodedChars();

        // we've finished the input, wrap it up
        encodedBuf.clear();
        flush();
    }

    /**
     * Must only be called when the input encoded bytes buffer is empty
     *
     * @throws CharacterCodingException
     */
    private void flush() throws CharacterCodingException {
        CoderResult coderResult;
        decodedCharBuf.clear();

        coderResult = decoder.flush(decodedCharBuf);
        appendDecodedChars();

        throwIfError(coderResult);

        if (coderResult != UNDERFLOW) {
            throw new IllegalStateException("Decoder flush resulted in " + coderResult);
        }
    }

    /**
     * If coderResult is considered an error (i.e. not overflow or underflow), throw the corresponding
     * CharacterCodingException.
     *
     * @param coderResult result to check
     * @throws CharacterCodingException
     */
    private void throwIfError(CoderResult coderResult) throws CharacterCodingException {
        if (coderResult.isError()) {
            coderResult.throwException();
        }
    }

    /**
     * Flip the decoded char buf and append it to the string bug
     */
    private void appendDecodedChars() {
        decodedCharBuf.flip();
        outputBuf.append(decodedCharBuf);
    }
}
