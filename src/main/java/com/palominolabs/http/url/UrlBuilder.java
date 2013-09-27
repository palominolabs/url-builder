/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static com.palominolabs.http.url.UrlPercentEncoders.getFragmentEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getMatrixEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getPathEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getQueryEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getRegNameEncoder;

/**
 * Builder for urls with url-encoding applied to path, query param, etc.
 *
 * Escaping rules are from RFC 3986, RFC 1738 and the HTML 4 spec. This means that this diverges from the canonical
 * URI/URL rules for the sake of being what you want to actually make HTTP-useful URLs.
 */
@NotThreadSafe
public final class UrlBuilder {

    /**
     * IPv6 address, cribbed from http://stackoverflow.com/questions/46146/what-are-the-java-regular-expressions-for-matching-ipv4-and-ipv6-strings
     */
    private static final Pattern IPV6_PATTERN = Pattern
        .compile(
            "\\A\\[((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)]\\z");

    /**
     * IPv4 dotted quad
     */
    private static final Pattern IPV4_PATTERN = Pattern
        .compile("\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z");

    @Nonnull
    private final String scheme;

    @Nonnull
    private final String host;

    @Nullable
    private final Integer port;

    private final List<Pair<String, String>> queryParams = Lists.newArrayList();

    private final List<String> pathSegments = Lists.newArrayList();

    private final List<Pair<String, String>> matrixParams = Lists.newArrayList();

    private final PercentEncoder pathEncoder = getPathEncoder();
    private final PercentEncoder regNameEncoder = getRegNameEncoder();
    private final PercentEncoder matrixEncoder = getMatrixEncoder();
    private final PercentEncoder queryEncoder = getQueryEncoder();
    private final PercentEncoder fragmentEncoder = getFragmentEncoder();

    @Nullable
    private String fragment;

    private boolean forceTrailingSlash = false;

    /**
     * Create a URL with an null port and UTF-8 encoding.
     *
     * @param scheme scheme (e.g. http)
     * @param host   host in any of the valid syntaxes: reg-name ( a dns name), ipv4 literal (1.2.3.4), ipv6 literal
     *               ([::1]), excluding IPvFuture since no one uses that in practice
     * @return a url builder
     * @see UrlBuilder#forHost(String scheme, String host, int port)
     */
    public static UrlBuilder forHost(@Nonnull String scheme, @Nonnull String host) {
        return new UrlBuilder(scheme, host, null);
    }

    /**
     * @param scheme scheme (e.g. http)
     * @param host   host in any of the valid syntaxes: reg-name ( a dns name), ipv4 literal (1.2.3.4), ipv6 literal
     *               ([::1]), excluding IPvFuture since no one uses that in practice
     * @param port   port
     * @return a url builder
     */
    public static UrlBuilder forHost(@Nonnull String scheme, @Nonnull String host, int port) {
        return new UrlBuilder(scheme, host, port);
    }

    /**
     * Create a URL with UTF-8 encoding.
     *
     * @param scheme scheme (e.g. http)
     * @param host   host (e.g. foo.com or 1.2.3.4 or [::1])
     * @param port   null or a positive integer
     */
    private UrlBuilder(@Nonnull String scheme, @Nonnull String host, @Nullable Integer port) {
        this.host = host;
        this.scheme = scheme;
        this.port = port;
    }

    @Nonnull
    public UrlBuilder pathSegment(String segment) {
        pathSegments.add(segment);
        return this;
    }

    @Nonnull
    public UrlBuilder pathSegments(String... segments) {
        for (String segment : segments) {
            pathSegment(segment);
        }

        return this;
    }

    @Nonnull
    public UrlBuilder queryParam(@Nonnull String key, @Nonnull String value) {
        queryParams.add(Pair.of(key, value));
        return this;
    }

    @Nonnull
    public UrlBuilder matrixParam(@Nonnull String key, @Nonnull String value) {
        matrixParams.add(Pair.of(key, value));
        return this;
    }

    @Nonnull
    public UrlBuilder fragment(String fragment) {
        this.fragment = fragment;
        return this;
    }

    public UrlBuilder forceTrailingSlash() {
        forceTrailingSlash = true;
        return this;
    }

    public String toUrlString() {
        StringBuilder buf = new StringBuilder();

        buf.append(scheme);
        buf.append("://");

        buf.append(encodeHost(host));
        if (port != null) {
            buf.append(':');
            buf.append(port);
        }

        for (String pathSegment : pathSegments) {
            buf.append('/');
            buf.append(pathEncoder.encode(pathSegment));
        }

        if (forceTrailingSlash) {
            buf.append('/');
        }

        if (pathSegments.isEmpty() && !matrixParams.isEmpty()) {
            // matrix is technically part of path, so if the path segment exists, it must start with a / per RFC 3986 S3
            buf.append('/');
        }
        for (Pair<String, String> matrixParam : matrixParams) {
            buf.append(';');
            buf.append(matrixEncoder.encode(matrixParam.getKey()));
            buf.append('=');
            buf.append(matrixEncoder.encode(matrixParam.getValue()));
        }

        if (!queryParams.isEmpty()) {
            buf.append("?");
            Iterator<Pair<String, String>> qpIter = queryParams.iterator();
            while (qpIter.hasNext()) {
                Pair<String, String> queryParam = qpIter.next();
                buf.append(queryEncoder.encode(queryParam.getKey()));
                buf.append('=');
                buf.append(queryEncoder.encode(queryParam.getValue()));
                if (qpIter.hasNext()) {
                    buf.append('&');
                }
            }
        }

        if (fragment != null) {
            buf.append('#');
            buf.append(fragmentEncoder.encode(fragment));
        }

        return buf.toString();
    }

    /**
     * @param host original host string
     * @return host encoded as in RFC 3986 secion 3.2.2
     */
    @Nonnull
    private String encodeHost(String host) {
        // matching order: IP-literal, IPv4, reg-name
        if (IPV4_PATTERN.matcher(host).matches() || IPV6_PATTERN.matcher(host).matches()) {
            return host;
        }

        // it's a reg-name, which MUST be encoded as UTF-8 (regardless of the rest of the URL)
        return regNameEncoder.encode(host);
    }
}
