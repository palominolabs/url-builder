package com.palominolabs.http.url;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import static com.palominolabs.http.url.PercentEncoderBenchmark.LARGE_STRING_MIX;
import static com.palominolabs.http.url.PercentEncoderBenchmark.SMALL_STRING_MIX;

public class PercentDecoderBenchmark {

    static final String SMALL_STRING_ENCODED;
    static final String LARGE_STRING_ENCODED;

    static {
        PercentEncoder encoder = UrlPercentEncoders.getUnstructuredQueryEncoder();
        try {
            SMALL_STRING_ENCODED = encoder.encode(SMALL_STRING_MIX);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
        try {
            LARGE_STRING_ENCODED = encoder.encode(LARGE_STRING_MIX);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {
        PercentDecoder decoder = new PercentDecoder(StandardCharsets.UTF_8.newDecoder());
    }

    @Benchmark
    public String testPercentDecodeSmall(ThreadState state) throws CharacterCodingException {
        return state.decoder.decode(SMALL_STRING_ENCODED);
    }

    @Benchmark
    public String testPercentDecodeLarge(ThreadState state) throws CharacterCodingException {
        return state.decoder.decode(LARGE_STRING_ENCODED);
    }
}
