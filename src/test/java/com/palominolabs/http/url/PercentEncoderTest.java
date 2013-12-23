/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.BitSet;

import static com.google.common.base.Charsets.UTF_16BE;
import static com.google.common.base.Charsets.UTF_8;
import static java.nio.charset.CodingErrorAction.REPLACE;
import static org.junit.Assert.assertEquals;

public final class PercentEncoderTest {

    private PercentEncoder alnum;
    private PercentEncoder alnum16;

    @Before
    public void setUp() {
        BitSet bs = new BitSet();
        for (int i = 'a'; i <= 'z'; i++) {
            bs.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            bs.set(i);
        }
        for (int i = '0'; i <= '9'; i++) {
            bs.set(i);
        }

        this.alnum = new PercentEncoder(bs, UTF_8.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
        this.alnum16 = new PercentEncoder(bs, UTF_16BE.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
    }

    @Test
    public void testDoesntEncodeSafe() throws CharacterCodingException {
        BitSet set = new BitSet();
        for (int i = 'a'; i <= 'z'; i++) {
            set.set(i);
        }

        PercentEncoder pe = new PercentEncoder(set, UTF_8.newEncoder().onMalformedInput(REPLACE)
            .onUnmappableCharacter(REPLACE));
        assertEquals("abcd%41%42%43%44", pe.encode("abcdABCD"));
    }

    @Test
    public void testEncodeInBetweenSafe() throws MalformedInputException, UnmappableCharacterException {
        assertEquals("abc%20123", alnum.encode("abc 123"));
    }

    @Test
    public void testEncodeUtf8() throws CharacterCodingException {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%E2%98%83", alnum.encode("snowman\u2603"));
    }

    @Test
    public void testEncodeUtf8SurrogatePair() throws CharacterCodingException {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%F0%9D%84%9E", alnum.encode("clef\ud834\udd1e"));
    }

    @Test
    public void testEncodeUtf16() throws CharacterCodingException {
        // 1 UTF-16 char (unicode snowman)
        assertEquals("snowman%26%03", alnum16.encode("snowman\u2603"));
    }

    @Test
    public void testUrlEncodedUtf16SurrogatePair() throws CharacterCodingException {
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        assertEquals("clef%D8%34%DD%1E", alnum16.encode("clef\ud834\udd1e"));
    }
}
