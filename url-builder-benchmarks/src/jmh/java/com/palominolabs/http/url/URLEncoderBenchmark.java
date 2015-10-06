package com.palominolabs.http.url;

import org.openjdk.jmh.annotations.Benchmark;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.CharacterCodingException;

import static com.palominolabs.http.url.PercentEncoderBenchmark.LARGE_STRING_MIX;
import static com.palominolabs.http.url.PercentEncoderBenchmark.SMALL_STRING_MIX;

public class URLEncoderBenchmark {

    @Benchmark
    public String testUrlEncodeSmall() throws CharacterCodingException, UnsupportedEncodingException {
        return URLEncoder.encode(SMALL_STRING_MIX, "UTF-8");
    }

    @Benchmark
    public String testUrlEncodeLarge() throws CharacterCodingException, UnsupportedEncodingException {
        return URLEncoder.encode(LARGE_STRING_MIX, "UTF-8");
    }
}
