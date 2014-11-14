package com.palominolabs.http.url;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A PercentEncoderHandler implementation that accumulates chars in a buffer.
 */
@NotThreadSafe
public final class StringBuilderPercentEncoderHandler implements PercentEncoderHandler {

    private final StringBuilder stringBuilder;

    /**
     * Create a new handler with a default size StringBuilder.
     */
    public StringBuilderPercentEncoderHandler() {
        stringBuilder = new StringBuilder();
    }

    /**
     * @return A string containing the chars accumulated since the last call to reset()
     */
    @Nonnull
    public String getContents() {
        return stringBuilder.toString();
    }

    /**
     * Clear the buffer.
     */
    public void reset() {
        stringBuilder.setLength(0);
    }

    public void ensureCapacity(int length) {
        stringBuilder.ensureCapacity(length);
    }

    @Override
    public void onOutputChar(char c) {
        stringBuilder.append(c);
    }
}
