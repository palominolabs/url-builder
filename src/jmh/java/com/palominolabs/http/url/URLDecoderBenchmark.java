package com.palominolabs.http.url;

import org.openjdk.jmh.annotations.Benchmark;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.CharacterCodingException;

import static com.palominolabs.http.url.PercentDecoderBenchmark.LARGE_STRING_ENCODED;
import static com.palominolabs.http.url.PercentDecoderBenchmark.SMALL_STRING_ENCODED;

public class URLDecoderBenchmark {

    @Benchmark
    public String testUrlDecodeSmall() throws CharacterCodingException, UnsupportedEncodingException {
        return URLDecoder.decode(SMALL_STRING_ENCODED, "UTF-8");
    }

    @Benchmark
    public String testUrlDecodeLarge() throws CharacterCodingException, UnsupportedEncodingException {
        return URLDecoder.decode(LARGE_STRING_ENCODED, "UTF-8");
    }
}
