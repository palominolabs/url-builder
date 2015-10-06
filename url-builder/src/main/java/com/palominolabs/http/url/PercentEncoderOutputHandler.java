package com.palominolabs.http.url;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface PercentEncoderOutputHandler {
    /**
     * Called on each character output by a PercentEncoder.
     *
     * @param c output character that's either in the calling PercentEncoder's safe char set or part of a
     *          percent-hex-encoded triple, e.g. "%FF".
     */
    void onOutputChar(char c);
}
