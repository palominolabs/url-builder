package com.palominolabs.http.url;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface PercentEncoderHandler {
    void onOutputChar(char c);
}
