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
