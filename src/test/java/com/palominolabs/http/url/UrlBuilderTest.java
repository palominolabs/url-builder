/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url;

import com.google.common.base.Throwables;
import org.junit.Ignore;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;

import static com.palominolabs.http.url.UrlBuilder.forHost;
import static com.palominolabs.http.url.UrlBuilder.fromUrl;
import static org.junit.Assert.assertEquals;

public final class UrlBuilderTest {

    @Test
    public void testNoUrlParts() {
        assertUrlEquals("http://foo.com", forHost("http", "foo.com").toUrlString());
    }

    @Test
    public void testWithPort() {
        assertUrlEquals("http://foo.com:33", forHost("http", "foo.com", 33).toUrlString());
    }

    @Test
    public void testSimplePath() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.pathSegment("seg1").pathSegment("seg2");
        assertUrlEquals("http://foo.com/seg1/seg2", ub.toUrlString());
    }

    @Test
    public void testPathWithReserved() {
        // RFC 1738 S3.3
        UrlBuilder ub = forHost("http", "foo.com");
        ub.pathSegment("seg/;?ment").pathSegment("seg=&2");
        assertUrlEquals("http://foo.com/seg%2F%3B%3Fment/seg=&2", ub.toUrlString());
    }

    @Test
    public void testPathSegments() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.pathSegments("seg1", "seg2", "seg3");
        assertUrlEquals("http://foo.com/seg1/seg2/seg3", ub.toUrlString());
    }

    @Test
    public void testMatrixWithoutPathHasLeadingSlash() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.matrixParam("foo", "bar");
        assertUrlEquals("http://foo.com/;foo=bar", ub.toUrlString());
    }

    @Test
    public void testMatrixWithReserved() {
        UrlBuilder ub = forHost("http", "foo.com")
            .pathSegment("foo")
            .matrixParam("foo", "bar")
            .matrixParam("res;=?#/erved", "value")
            .pathSegment("baz");
        assertUrlEquals("http://foo.com/foo;foo=bar;res%3B%3D%3F%23%2Ferved=value/baz", ub.toUrlString());
    }

    @Test
    public void testUrlEncodedPathSegmentUtf8() {
        // 1 UTF-16 char
        UrlBuilder ub = forHost("http", "foo.com");
        ub.pathSegment("snowman").pathSegment("\u2603");
        assertUrlEquals("http://foo.com/snowman/%E2%98%83", ub.toUrlString());
    }

    @Test
    public void testUrlEncodedPathSegmentUtf8SurrogatePair() {
        UrlBuilder ub = forHost("http", "foo.com");
        // musical G clef: 1d11e, has to be represented in surrogate pair form
        ub.pathSegment("clef").pathSegment("\ud834\udd1e");
        assertUrlEquals("http://foo.com/clef/%F0%9D%84%9E", ub.toUrlString());
    }

    @Test
    public void testQueryParamNoPath() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.queryParam("foo", "bar");
        String s = ub.toUrlString();
        assertUrlEquals("http://foo.com?foo=bar", s);
    }

    @Test
    public void testQueryParamsDuplicated() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.queryParam("foo", "bar");
        ub.queryParam("foo", "bar2");
        ub.queryParam("baz", "quux");
        ub.queryParam("baz", "quux2");
        assertUrlEquals("http://foo.com?foo=bar&foo=bar2&baz=quux&baz=quux2", ub.toUrlString());
    }

    @Test
    public void testEncodeQueryParams() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.queryParam("foo", "bar&=#baz");
        ub.queryParam("foo", "bar?/2");
        assertUrlEquals("http://foo.com?foo=bar%26%3D%23baz&foo=bar?/2", ub.toUrlString());
    }

    @Test
    public void testEncodeQueryParamWithSpaceAndPlus() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.queryParam("foo", "spa ce");
        ub.queryParam("fo+o", "plus+");
        assertUrlEquals("http://foo.com?foo=spa%20ce&fo%2Bo=plus%2B", ub.toUrlString());
    }

    @Test
    public void testPlusInVariousParts() {
        UrlBuilder ub = forHost("http", "foo.com");

        ub.pathSegment("has+plus")
            .matrixParam("plusMtx", "pl+us")
            .queryParam("plusQp", "pl+us")
            .fragment("plus+frag");

        assertUrlEquals("http://foo.com/has+plus;plusMtx=pl+us?plusQp=pl%2Bus#plus+frag", ub.toUrlString());
    }

    @Test
    public void testFragment() {
        UrlBuilder ub = forHost("http", "foo.com");
        ub.queryParam("foo", "bar");
        ub.fragment("#frag/?");
        assertUrlEquals("http://foo.com?foo=bar#%23frag/?", ub.toUrlString());
    }

    @Test
    public void testAllParts() {
        UrlBuilder ub = forHost("https", "foo.bar.com", 3333);
        ub.pathSegment("foo");
        ub.pathSegment("bar");
        ub.matrixParam("mtx1", "val1");
        ub.matrixParam("mtx2", "val2");
        ub.queryParam("q1", "v1");
        ub.queryParam("q2", "v2");
        ub.fragment("zomg it's a fragment");

        assertEquals("https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2?q1=v1&q2=v2#zomg%20it's%20a%20fragment",
            ub.toUrlString());
    }

    @Test
    public void testIPv4Literal() {
        UrlBuilder ub = forHost("http", "127.0.0.1");
        assertUrlEquals("http://127.0.0.1", ub.toUrlString());
    }

    @Test
    public void testBadIPv4LiteralDoesntChoke() {
        UrlBuilder ub = forHost("http", "300.100.50.1");
        assertUrlEquals("http://300.100.50.1", ub.toUrlString());
    }

    @Test
    public void testIPv6LiteralLocalhost() {
        UrlBuilder ub = forHost("http", "[::1]");
        assertUrlEquals("http://[::1]", ub.toUrlString());
    }

    @Test
    public void testIPv6Literal() {
        UrlBuilder ub = forHost("http", "[2001:db8:85a3::8a2e:370:7334]");
        assertUrlEquals("http://[2001:db8:85a3::8a2e:370:7334]", ub.toUrlString());
    }

    @Test
    public void testEncodedRegNameSingleByte() {
        UrlBuilder ub = forHost("http", "host?name;");
        assertUrlEquals("http://host%3Fname;", ub.toUrlString());
    }

    @Test
    public void testEncodedRegNameMultiByte() {
        UrlBuilder ub = forHost("http", "snow\u2603man");
        assertUrlEquals("http://snow%E2%98%83man", ub.toUrlString());
    }

    @Test
    public void testForceTrailingSlash() {
        UrlBuilder ub = forHost("https", "foo.com").forceTrailingSlash().pathSegments("a", "b", "c");

        assertUrlEquals("https://foo.com/a/b/c/", ub.toUrlString());
    }

    @Test
    public void testForceTrailingSlashWithQueryParams() {
        UrlBuilder ub =
            forHost("https", "foo.com").forceTrailingSlash().pathSegments("a", "b", "c").queryParam("foo", "bar");

        assertUrlEquals("https://foo.com/a/b/c/?foo=bar", ub.toUrlString());
    }

    @Test
    public void testForceTrailingSlashNoPathSegmentsWithMatrixParams() {
        UrlBuilder ub = forHost("https", "foo.com").forceTrailingSlash().matrixParam("m1", "v1");

        assertUrlEquals("https://foo.com/;m1=v1/", ub.toUrlString());
    }

    @Test
    public void testIntermingledMatrixParamsAndPathSegments() {

        UrlBuilder ub = forHost("http", "foo.com")
            .pathSegments("seg1", "seg2")
            .matrixParam("m1", "v1")
            .pathSegment("seg3")
            .matrixParam("m2", "v2");

        assertUrlEquals("http://foo.com/seg1/seg2;m1=v1/seg3;m2=v2", ub.toUrlString());
    }

    @Test
    public void testFromUrlWithEverything() throws MalformedURLException, CharacterCodingException {
        String orig =
            "https://foo.bar.com:3333/foo/bar;mtx1=val1;mtx2=val2/seg3;m2=v2?q1=v1&q2=v2#zomg%20it's%20a%20fragment";
        UrlBuilder ub = fromUrl(new URL(orig));

        assertUrlEquals(orig, ub.toUrlString());
    }

    @Test
    @Ignore
    public void testFromUrlWithEmptyPath() {

    }

    @Test
    @Ignore

    public void testFromUrlWithPort() {

    }

    @Test
    @Ignore

    public void testFromUrlWithEmptyPathSegent() {

    }

    @Test
    @Ignore

    public void testFromUrlWithEncodedHost() {

    }

    @Test
    @Ignore

    public void testFromUrlWithEncodedPathSegment() {

    }

    @Test
    @Ignore

    public void testFromUrlWithEncodedMatrixParam() {

    }

    @Test
    @Ignore

    public void testFromUrlWithEncodedQueryParam() {

    }

    @Test
    @Ignore

    public void testFromUrlWithEncodedFragment() {

    }

    @Test
    @Ignore

    public void testFromUrlWithMalformedMatrixPair() {

    }

    @Test
    @Ignore

    public void testFromUrlWithEmptyPathSegmentWithMatrixParams() {

    }

    @Test
    @Ignore

    public void testFromUrlWithPathSegmentEndingWithSemicolon() {

    }

    @Test
    @Ignore

    public void testPercentDecodeInvalidPair() {

    }

    private static void assertUrlEquals(String expected, String actual) {
        assertEquals(expected, actual);
        try {
            assertEquals(expected, new URI(actual).toString());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }
}
