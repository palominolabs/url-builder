/*
 * Copyright (c) 2012 Palomino Labs, Inc.
 */

package com.palominolabs.http.url;

import com.google.common.collect.Lists;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.lang3.tuple.Pair;

import static com.google.common.base.Charsets.UTF_8;
import static com.palominolabs.http.url.UrlPercentEncoders.getFragmentEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getMatrixEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getPathEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getQueryParamEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getRegNameEncoder;
import static com.palominolabs.http.url.UrlPercentEncoders.getUnstructuredQueryEncoder;

/**
 * Builder for urls with url-encoding applied to path, query param, etc.
 *
 * Escaping rules are from RFC 3986, RFC 1738 and the HTML 4 spec (http://www.w3.org/TR/html401/interact/forms.html#form-content-type).
 * This means that this diverges from the canonical URI/URL rules for the sake of being what you want to actually make
 * HTTP-useful URLs.
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

    /**
     * If this is non-null, queryParams must be empty, and vice versa.
     */
    @Nullable
    private String unstructuredQuery;

    private final List<PathSegment> pathSegments = Lists.newArrayList();

    private final PercentEncoder pathEncoder = getPathEncoder();
    private final PercentEncoder regNameEncoder = getRegNameEncoder();
    private final PercentEncoder matrixEncoder = getMatrixEncoder();
    private final PercentEncoder queryParamEncoder = getQueryParamEncoder();
    private final PercentEncoder unstructuredQueryEncoder = getUnstructuredQueryEncoder();
    private final PercentEncoder fragmentEncoder = getFragmentEncoder();

    @Nullable
    private String fragment;

    private boolean forceTrailingSlash = false;

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

    /**
     * Create a URL with an null port and UTF-8 encoding.
     *
     * @param scheme scheme (e.g. http)
     * @param host   host in any of the valid syntaxes: reg-name (a dns name), ipv4 literal (1.2.3.4), ipv6 literal
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
     * Calls {@link UrlBuilder#fromUrl(URL, CharsetDecoder)} with a UTF-8 CharsetDecoder. The same semantics about the
     * query string apply.
     *
     * @param url url to initialize builder with
     * @return a UrlBuilder containing the host, path, etc. from the url
     * @throws CharacterCodingException if char decoding fails
     * @see UrlBuilder#fromUrl(URL, CharsetDecoder)
     */
    @Nonnull
    public static UrlBuilder fromUrl(@Nonnull URL url) throws CharacterCodingException {
        return fromUrl(url, UTF_8.newDecoder());
    }

    /**
     * Create a UrlBuilder initialized with the contents of a {@link URL}.
     *
     * The query string will be parsed into HTML4 query params if it can be separated into a
     * <code>&amp;</code>-separated sequence of <code>key=value</code> pairs. The sequence of query params can then be
     * appended to by continuing to call {@link UrlBuilder#queryParam(String, String)}. The concept of query params is
     * only part of the HTML spec (and common HTTP usage), though, so it's perfectly legal to have a query string that
     * is in some other form. To represent this case, if the aforementioned param-parsing attempt fails, the query
     * string will be treated as just a monolithic, unstructured, string. In this case, calls to {@link
     * UrlBuilder#queryParam(String, String)} on the resulting instance will throw IllegalStateException, and only calls
     * to {@link UrlBuilder#unstructuredQuery(String)}}, which replaces the entire query string, are allowed.
     *
     * @param url            url to initialize builder with
     * @param charsetDecoder the decoder to decode encoded bytes with (except for reg names, which are always UTF-8)
     * @return a UrlBuilder containing the host, path, etc. from the url
     * @throws CharacterCodingException if decoding percent-encoded bytes fails and charsetDecoder is configured to
     *                                  report errors
     * @see UrlBuilder#fromUrl(URL, CharsetDecoder)
     */
    @Nonnull
    public static UrlBuilder fromUrl(@Nonnull URL url, @Nonnull CharsetDecoder charsetDecoder) throws
            CharacterCodingException {

        PercentDecoder decoder = new PercentDecoder(charsetDecoder);
        // reg names must be encoded UTF-8
        PercentDecoder regNameDecoder;
        if (charsetDecoder.charset().equals(UTF_8)) {
            regNameDecoder = decoder;
        } else {
            regNameDecoder = new PercentDecoder(UTF_8.newDecoder());
        }

        Integer port = url.getPort();
        if (port == -1) {
            port = null;
        }

        UrlBuilder builder = new UrlBuilder(url.getProtocol(), regNameDecoder.decode(url.getHost()), port);

        buildFromPath(builder, decoder, url);

        buildFromQuery(builder, decoder, url);

        if (url.getRef() != null) {
            builder.fragment(decoder.decode(url.getRef()));
        }

        return builder;
    }

    /**
     * Add a path segment.
     *
     * @param segment a path segment
     * @return this
     */
    @Nonnull
    public UrlBuilder pathSegment(@Nonnull String segment) {
        pathSegments.add(new PathSegment(segment));
        return this;
    }

    /**
     * Add multiple path segments. Equivalent to successive calls to {@link UrlBuilder#pathSegment(String)}.
     *
     * @param segments path segments
     * @return this
     */
    @Nonnull
    public UrlBuilder pathSegments(String... segments) {
        for (String segment : segments) {
            pathSegment(segment);
        }

        return this;
    }

    /**
     * Add an HTML query parameter. Query parameters will be encoded in the order added.
     *
     * Using query strings to encode key=value pairs is not part of the URI/URL specification; it is specified by
     * http://www.w3.org/TR/html401/interact/forms.html#form-content-type.
     *
     * If you use this method to build a query string, or created this builder from a url with a query string that can
     * successfully be parsed into query param pairs, you cannot subsequently use {@link
     * UrlBuilder#unstructuredQuery(String)}. See {@link UrlBuilder#fromUrl(URL, CharsetDecoder)}.
     *
     * @param name  param name
     * @param value param value
     * @return this
     */
    @Nonnull
    public UrlBuilder queryParam(@Nonnull String name, @Nonnull String value) {
        if (unstructuredQuery != null) {
            throw new IllegalStateException(
                    "Cannot call queryParam() when this already has an unstructured query specified");
        }

        queryParams.add(Pair.of(name, value));
        return this;
    }

    /**
     * Set the complete query string of arbitrary structure. This is useful when you want to specify a query string that
     * is not of key=value format. If the query has previously been set via this method, subsequent calls will overwrite
     * that query.
     *
     * If you use this method, or create a builder from a URL whose query is not parseable into query param pairs, you
     * cannot subsequently use {@link UrlBuilder#queryParam(String, String)}. See {@link UrlBuilder#fromUrl(URL,
     * CharsetDecoder)}.
     *
     * @param query Complete URI query, as specified by https://tools.ietf.org/html/rfc3986#section-3.4
     * @return this
     */
    @Nonnull
    public UrlBuilder unstructuredQuery(@Nonnull String query) {
        if (!queryParams.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot call unstructuredQuery() when this already has queryParam pairs specified");
        }

        unstructuredQuery = query;

        return this;
    }

    /**
     * Clear the unstructured query and any query params.
     *
     * Since the query / query param situation is a little complicated, this method will let you remove all query
     * information and start again from scratch. This may be useful when taking an existing url, parsing it into a
     * builder, and then re-doing its query params, for instance.
     *
     * @return this
     */
    @Nonnull
    public UrlBuilder clearQuery() {
        queryParams.clear();
        unstructuredQuery = null;

        return this;
    }

    /**
     * Add a matrix param to the last added path segment. If no segments have been added, the param will be added to the
     * root. Matrix params will be encoded in the order added.
     *
     * @param name  param name
     * @param value param value
     * @return this
     */
    @Nonnull
    public UrlBuilder matrixParam(@Nonnull String name, @Nonnull String value) {
        if (pathSegments.isEmpty()) {
            // create an empty path segment to represent a matrix param applied to the root
            pathSegment("");
        }

        PathSegment seg = pathSegments.get(pathSegments.size() - 1);
        seg.matrixParams.add(Pair.of(name, value));
        return this;
    }

    /**
     * Set the fragment.
     *
     * @param fragment fragment string
     * @return this
     */
    @Nonnull
    public UrlBuilder fragment(@Nonnull String fragment) {
        this.fragment = fragment;
        return this;
    }

    /**
     * Force the generated URL to have a trailing slash at the end of the path.
     *
     * @return this
     */
    @Nonnull
    public UrlBuilder forceTrailingSlash() {
        forceTrailingSlash = true;
        return this;
    }

    private void appendPath(StringBuilder buf) throws CharacterCodingException {
        for (PathSegment pathSegment : pathSegments) {
            buf.append('/');
            buf.append(pathEncoder.encode(pathSegment.segment));

            for (Pair<String, String> matrixParam : pathSegment.matrixParams) {
                buf.append(';');
                buf.append(matrixEncoder.encode(matrixParam.getKey()));
                buf.append('=');
                buf.append(matrixEncoder.encode(matrixParam.getValue()));
            }
        }

        if (forceTrailingSlash) {
            buf.append('/');
        }
    }

    private void appendQuery(StringBuilder buf) throws CharacterCodingException {
        if (!queryParams.isEmpty()) {
            buf.append("?");
            Iterator<Pair<String, String>> qpIter = queryParams.iterator();
            while (qpIter.hasNext()) {
                Pair<String, String> queryParam = qpIter.next();
                buf.append(queryParamEncoder.encode(queryParam.getKey()));
                buf.append('=');
                buf.append(queryParamEncoder.encode(queryParam.getValue()));
                if (qpIter.hasNext()) {
                    buf.append('&');
                }
            }
        } else if (unstructuredQuery != null) {
            buf.append("?");
            buf.append(unstructuredQueryEncoder.encode(unstructuredQuery));
        }
    }

    private void appendFragment(StringBuilder buf) throws CharacterCodingException {
        if (fragment != null) {
            buf.append('#');
            buf.append(fragmentEncoder.encode(fragment));
        }
    }

    public URL toUrl() throws CharacterCodingException {
        StringBuilder buf = new StringBuilder();

        appendPath(buf);
        appendQuery(buf);
        appendFragment(buf);

        String path = buf.toString();

        try {
            if (port != null) {
                return new URL(scheme, host, port, path);
            } else {
                return new URL(scheme, host, path);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unknown scheme specified", e);
        }
    }

    /**
     * Encode the current builder state into a URL string.
     *
     * @return a well-formed URL string
     * @throws CharacterCodingException if character encoding fails and the encoder is configured to report errors
     */
    public String toUrlString() throws CharacterCodingException {
        StringBuilder buf = new StringBuilder();

        buf.append(scheme);
        buf.append("://");

        buf.append(encodeHost(host));
        if (port != null) {
            buf.append(':');
            buf.append(port);
        }

        appendPath(buf);
        appendQuery(buf);
        appendFragment(buf);

        return buf.toString();
    }

    /**
     * Populate a url builder based on the query of a url
     *
     * @param builder builder
     * @param decoder decoder
     * @param url     url
     * @throws CharacterCodingException
     */
    private static void buildFromQuery(UrlBuilder builder, PercentDecoder decoder, URL url) throws
            CharacterCodingException {
        if (url.getQuery() != null) {
            String q = url.getQuery();

            // try to parse into &-separated key=value pairs
            List<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>();
            boolean parseOk = true;

            for (String queryChunk : q.split("&")) {
                String[] queryParamChunks = queryChunk.split("=");

                if (queryParamChunks.length != 2) {
                    parseOk = false;
                    break;
                }

                pairs.add(Pair.of(decoder.decode(queryParamChunks[0]),
                        decoder.decode(queryParamChunks[1])));
            }

            if (parseOk) {
                for (Pair<String, String> pair : pairs) {
                    builder.queryParam(pair.getKey(), pair.getValue());
                }
            } else {
                builder.unstructuredQuery(decoder.decode(q));
            }
        }
    }

    /**
     * Populate the path segments of a url builder from a url
     *
     * @param builder builder
     * @param decoder decoder
     * @param url     url
     * @throws CharacterCodingException
     */
    private static void buildFromPath(UrlBuilder builder, PercentDecoder decoder, URL url) throws
            CharacterCodingException {
        for (String pathChunk : url.getPath().split("/")) {
            if (pathChunk.equals("")) {
                continue;
            }

            if (pathChunk.charAt(0) == ';') {
                builder.pathSegment("");
                // empty path segment, but matrix params
                for (String matrixChunk : pathChunk.substring(1).split(";")) {
                    buildFromMatrixParamChunk(decoder, builder, matrixChunk);
                }

                continue;
            }

            // otherwise, path chunk is non empty and does not start with a ';'

            String[] matrixChunks = pathChunk.split(";");

            // first chunk is always the path segment. If there is a trailing ; and no matrix params, the ; will
            // not be included in the final url.
            builder.pathSegment(decoder.decode(matrixChunks[0]));

            // if there any other chunks, they're matrix param pairs
            for (int i = 1; i < matrixChunks.length; i++) {
                buildFromMatrixParamChunk(decoder, builder, matrixChunks[i]);
            }
        }
    }

    private static void buildFromMatrixParamChunk(PercentDecoder decoder, UrlBuilder ub, String pathMatrixChunk) throws
            CharacterCodingException {
        String[] mtxPair = pathMatrixChunk.split("=");
        if (mtxPair.length != 2) {
            throw new IllegalArgumentException("Malformed matrix param: <" + pathMatrixChunk + ">");
        }

        String mtxName = mtxPair[0];
        String mtxVal = mtxPair[1];
        ub.matrixParam(decoder.decode(mtxName), decoder.decode(mtxVal));
    }

    /**
     * @param host original host string
     * @return host encoded as in RFC 3986 section 3.2.2
     */
    @Nonnull
    private String encodeHost(String host) throws CharacterCodingException {
        // matching order: IP-literal, IPv4, reg-name
        if (IPV4_PATTERN.matcher(host).matches() || IPV6_PATTERN.matcher(host).matches()) {
            return host;
        }

        // it's a reg-name, which MUST be encoded as UTF-8 (regardless of the rest of the URL)
        return regNameEncoder.encode(host);
    }

    /**
     * Bundle of a path segment name and any associated matrix params.
     */
    private static class PathSegment {
        private final String segment;
        private final List<Pair<String, String>> matrixParams = Lists.newArrayList();

        PathSegment(String segment) {
            this.segment = segment;
        }
    }
}
