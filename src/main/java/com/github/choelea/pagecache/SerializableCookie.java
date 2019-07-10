package com.github.choelea.pagecache;


import javax.servlet.http.Cookie;
import java.io.Serializable;

 
public class SerializableCookie implements Serializable {

    private static final long serialVersionUID = 8628587700329421486L;

    private String name;
    private String value;
    private String comment;
    private String domain;
    private int maxAge;
    private String path;
    private boolean secure;
    private int version;

    public SerializableCookie(final Cookie cookie) {
        name = cookie.getName();
        value = cookie.getValue();
        comment = cookie.getComment();
        domain = cookie.getDomain();
        maxAge = cookie.getMaxAge();
        path = cookie.getPath();
        secure = cookie.getSecure();
        version = cookie.getVersion();
    }

    
    public Cookie toCookie() {
        final Cookie cookie = new Cookie(name, value);
        cookie.setComment(comment);
        if (domain != null) {
            cookie.setDomain(domain);
        }
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        cookie.setSecure(secure);
        cookie.setVersion(version);
        return cookie;
    }
}
