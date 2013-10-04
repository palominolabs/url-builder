Use this library to safely create valid, correctly encoded URL strings with a fluent API.

# Usage

Add this to the `dependencies` block in your `build.gradle`:

    compile 'com.palominolabs.http:url-builder:VERSION'

where `VERSION` is the latest released version.  If you're using Maven, know that your life could be greatly improved by switching to Gradle and use this dependency block:

    <dependency>
        <groupId>com.palominolabs.http</groupId>
        <artifactId>url-builder</artifactId>
        <version>VERSION</version>
    </dependency>

# Example

```
// showcase the different encoding rules used on different URL components
UrlBuilder.forHost("http", "foo.com")
    .pathSegment("with spaces")
    .pathSegments("path", "with", "varArgs")
    .pathSegment("&=?/")
    .queryParam("fancy + name", "fancy?=value")
    .matrixParam("matrix", "param?")
    .fragment("#?=")
    .toUrlString()

// produces:
// http://foo.com/with%20spaces/path/with/varArgs/&=%3F%2F;matrix=param%3F?fancy%20%2B%20name=fancy?%3Dvalue#%23?=

```

# Motivation

See [this blog post](http://blog.palominolabs.com/2013/10/03/creating-urls-correctly-and-safely/) for a thorough explanation.

Ideally, the Java SDK would provide a good way to build properly encoded URLs. Unfortunately, it does not.

[`URLEncoder`](http://docs.oracle.com/javase/7/docs/api/java/net/URLEncoder.html) seems like a thing that you want to use, but amazingly enough it actually does HTML form encoding, not URL encoding.

URL encoding is also not something that can be done once you've formed a complete URL string. If your URL is already correctly encoded, you do not need to do anything. If it is not, it is impossible to parse it into its constituent parts for subsequent encoding. You must construct a url piece by piece, correctly encoding each piece as you go, to end up with a valid URL string. The encoding rules are also different for different parts of the URL (path, query param, etc.)

 Since the URLs that we use in practice for HTTP have somewhat different rules than "generic" URLs, UrlBuilder errs on the side of usefulness for HTTP-specific URLs. Notably, this means that '+' is percent-encoded to avoid being interpreted as a space.
