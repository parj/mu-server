package io.muserver;

import io.netty.handler.codec.http.cookie.DefaultCookie;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A cookie that is sent from the server to the client.
 * <p>To create cookies, you can create a builder by calling {@link #builder()}.</p>
 */
public class Cookie {

    final DefaultCookie nettyCookie;


    private Cookie(String name, String value) {
        nettyCookie = new DefaultCookie(name, value);
    }

    Cookie(String name, String value, String domain, String path, long maxAge, boolean secure, boolean httpOnly) {
        nettyCookie = new DefaultCookie(name, value);
        nettyCookie.setDomain(domain);
        nettyCookie.setPath(path);
        nettyCookie.setMaxAge(maxAge);
        nettyCookie.setSecure(secure);
        nettyCookie.setHttpOnly(httpOnly);
    }

    public String name() {
        return nettyCookie.name();
    }

    public String value() {
        return nettyCookie.value();
    }

    public String domain() {
        return nettyCookie.domain();
    }

    public String path() {
        return nettyCookie.path();
    }

    public long maxAge() {
        return nettyCookie.maxAge();
    }

    public boolean isSecure() {
        return nettyCookie.isSecure();
    }

    /**
     * @return Returns the HTTPOnly value
     */
    public boolean isHttpOnly() {
        return nettyCookie.isHttpOnly();
    }

    public int hashCode() {
        return nettyCookie.hashCode();
    }

    public boolean equals(Object o) {
        return (this == o) || ((o instanceof Cookie) && nettyCookie.equals(o));
    }

    public String toString() {
        return nettyCookie.toString();
    }

    static List<Cookie> nettyToMu(Set<io.netty.handler.codec.http.cookie.Cookie> originals) {
        return originals.stream().map(n -> new Cookie(n.name(), n.value())).collect(Collectors.toList());
    }

    /**
     * Creates a new cookie builder with secure and httpOnly selected.
     * @return A new builder
     */
    public static CookieBuilder builder() {
        return CookieBuilder.newSecureCookie();
    }
}
