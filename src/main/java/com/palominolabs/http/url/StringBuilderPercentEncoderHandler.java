package com.palominolabs.http.url;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A PercentEncoderHandler implementation that accumulates chars in a buffer.
 */
@NotThreadSafe
public final class StringBuilderPercentEncoderHandler implements PercentEncoderHandler {

    private final StringBuilder buffer;

    /**
     * Create a new handler with a default size StringBuilder.
     */
    public StringBuilderPercentEncoderHandler() {
        buffer = new StringBuilder();
    }

    @Override
    public void onEncodedChar(char c) {
        buffer.append(c);
    }

    /**
     * @return A string containing the chars accumulated since the last call to reset()
     */
    @Nonnull
    public String getContents() {
        return buffer.toString();
    }

    /**
     * Clear the buffer.
     */
    public void reset() {
        buffer.setLength(0);
    }

    public void ensureCapacity(int length) {
        buffer.ensureCapacity(length);
    }
}
