package com.palominolabs.http.url;

import com.google.common.base.Strings;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.nio.charset.CharacterCodingException;

public class PercentEncoderBenchmark {

    // safe and unsafe
    static final String TINY_STRING_MIX = "foo bar baz";
    static final String SMALL_STRING_MIX = "small value !@#$%^&*()???????????????!@#$%^&*()";
    // no characters escaped
    static final String SMALL_STRING_ALL_SAFE = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    // all characters escaped
    static final String SMALL_STRING_ALL_UNSAFE = "???????????????????????????????????????????????";

    static final String LARGE_STRING_MIX;
    static final String LARGE_STRING_ALL_SAFE;
    static final String LARGE_STRING_ALL_UNSAFE;

    static {
        LARGE_STRING_MIX = Strings.repeat(SMALL_STRING_MIX, 1000);
        LARGE_STRING_ALL_SAFE = Strings.repeat(SMALL_STRING_ALL_SAFE, 1000);
        LARGE_STRING_ALL_UNSAFE = Strings.repeat(SMALL_STRING_ALL_UNSAFE, 1000);
    }

    @State(Scope.Thread)
    public static class ThreadState {
        PercentEncoder encoder = UrlPercentEncoders.getQueryEncoder();
        PercentEncoderOutputHandler noOpHandler = new NoOpOutputHandler();
        AccumXorOutputHandler accumXorHandler = new AccumXorOutputHandler();
    }

    @Benchmark
    public String testPercentEncodeTinyMix(ThreadState state) throws CharacterCodingException {
        return state.encoder.encode(TINY_STRING_MIX);
    }

    @Benchmark
    public String testPercentEncodeSmallMix(ThreadState state) throws CharacterCodingException {
        return state.encoder.encode(SMALL_STRING_MIX);
    }

    @Benchmark
    public String testPercentEncodeLargeMix(ThreadState state) throws CharacterCodingException {
        return state.encoder.encode(LARGE_STRING_MIX);
    }

    @Benchmark
    public String testPercentEncodeSmallSafe(ThreadState state) throws CharacterCodingException {
        return state.encoder.encode(SMALL_STRING_ALL_SAFE);
    }

    @Benchmark
    public String testPercentEncodeLargeSafe(ThreadState state) throws CharacterCodingException {
        return state.encoder.encode(LARGE_STRING_ALL_SAFE);
    }

    @Benchmark
    public String testPercentEncodeSmallUnsafe(ThreadState state) throws CharacterCodingException {
        return state.encoder.encode(SMALL_STRING_ALL_UNSAFE);
    }

    @Benchmark
    public String testPercentEncodeLargeUnsafe(ThreadState state) throws CharacterCodingException {
        return state.encoder.encode(LARGE_STRING_ALL_UNSAFE);
    }

    @Benchmark
    public void testPercentEncodeSmallNoOpMix(ThreadState state) throws CharacterCodingException {
        state.encoder.encode(SMALL_STRING_MIX, state.noOpHandler);
    }

    @Benchmark
    public void testPercentEncodeLargeNoOpMix(ThreadState state) throws CharacterCodingException {
        state.encoder.encode(LARGE_STRING_MIX, state.noOpHandler);
    }

    @Benchmark
    public char testPercentEncodeSmallAccumXorMix(ThreadState state) throws CharacterCodingException {
        state.encoder.encode(SMALL_STRING_MIX, state.accumXorHandler);
        return state.accumXorHandler.c;
    }

    @Benchmark
    public char testPercentEncodeLargeAccumXorMix(ThreadState state) throws CharacterCodingException {
        state.encoder.encode(LARGE_STRING_MIX, state.accumXorHandler);
        return state.accumXorHandler.c;
    }

    static class NoOpOutputHandler implements PercentEncoderOutputHandler {

        @Override
        public void onOutputChar(char c) {
            // no op
        }
    }

    /**
     * A handler that doesn't allocate, but can't be optimized away
     */
    static class AccumXorOutputHandler implements PercentEncoderOutputHandler {
        char c;

        @Override
        public void onOutputChar(char c) {
            this.c ^= c;
        }
    }
}
