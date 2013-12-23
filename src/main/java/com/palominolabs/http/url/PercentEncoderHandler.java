package com.palominolabs.http.url;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.CharBuffer;

@NotThreadSafe
public interface PercentEncoderHandler {

    /**
     * Any data not read from the buffer will be discarded after this method invocation.
     *
     * @param buffer buffer to read from
     */
    void onOutputChars(@Nonnull CharBuffer buffer);
}
