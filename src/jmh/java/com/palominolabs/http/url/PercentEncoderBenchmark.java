/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

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
        PercentEncoderHandler noOpHandler = new NoOpHandler();
        AccumXorHandler accumXorHandler = new AccumXorHandler();
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

    static class NoOpHandler implements PercentEncoderHandler {

        @Override
        public void onOutputChar(char c) {
            // no op
        }
    }

    /**
     * A handler that doesn't allocate, but can't be optimized away
     */
    static class AccumXorHandler implements PercentEncoderHandler {
        char c;

        @Override
        public void onOutputChar(char c) {
            this.c ^= c;
        }
    }
}
