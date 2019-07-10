package com.github.choelea.pagecache;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Page cache which can be used for page without query parameters. 
 * @author Joe
 *
 */

public abstract class AbstractPageCachingFilter  implements javax.servlet.Filter {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractPageCachingFilter.class);
	
	private String cacheName;
	public AbstractPageCachingFilter() {
	}

	protected abstract Boolean isCacheable(HttpServletRequest httpRequest);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        if(isCacheable(httpRequest)) {
        	if (response.isCommitted()) {
        		throw new ServletException(
        				"Response already committed before doing buildPage.");
        	}
        	try {
        		logRequestHeaders(httpRequest);
        		PageInfo pageInfo = buildPageInfo(httpRequest, httpResponse, filterChain);
        		
        		if (pageInfo.isOk()) {
        			if (response.isCommitted()) {
        				throw new ServletException(
        						"Response already committed after doing buildPage"
        								+ " but before writing response from PageInfo.");
        			}
        			writeResponse(httpRequest, httpResponse, pageInfo);
        		}
        	} catch (Exception e) {
        		LOG.error(e.getMessage(),e);
        	}
        }else {
        	filterChain.doFilter(request, response);
        }	
	}
	
	/**
	 * Can be overridden for some cases.
	 * @param request
	 * @return
	 */
	protected abstract String calculateKey(HttpServletRequest request);

    /**
     * Build page info either using the cache or building the page directly.
     * <p/>
     * Some requests are for page fragments which should never be gzipped, or
     * for other pages which are not gzipped.
     */
    protected PageInfo buildPageInfo(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain chain)
            throws Exception {
    	// Look up the cached page
        final String key = calculateKey(request);
        PageInfo pageInfo = null;
        try {
//            checkNoReentry(request);
        	pageInfo = getPageInfo(getCacheName(), key);
            if (pageInfo == null) {
                try {
                    // Page is not cached - build the response, cache it, and
                    // send to client
                    pageInfo = buildPage(request, response, chain);
                    if (pageInfo.isOk()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("PageInfo ok. Adding to cache " + getCacheName() + " with key " + key);
                        }
                        putPageInfo(getCacheName(), key, pageInfo);
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("PageInfo was not ok(200). Putting null into cache " + getCacheName() + " with key " + key);
                        }
                    }
                } catch (final Throwable throwable) {
                    // Must unlock the cache if the above fails. Will be logged
                    // at Filter
                    throw new Exception(throwable);
                }
            }
        } catch (Exception e) {
            // do not release the lock, because you never acquired it
            throw e;
        } finally {
            // all done building page, reset the re-entrant flag
//            visitLog.clear();
        }
        return pageInfo;
    }

   

	protected abstract void putPageInfo(String cacheName2, String key, PageInfo pageInfo);

	protected abstract PageInfo getPageInfo(String cacheName2, String key);

	/**
     * Builds the PageInfo object by passing the request along the filter chain
     * 
     * @param request
     * @param response
     * @param chain
     * @return a Serializable value object for the page or page fragment
     * @throws AlreadyGzippedException
     *             if an attempt is made to double gzip the body
     * @throws Exception
     */
    protected PageInfo buildPage(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain chain)
            throws AlreadyGzippedException, Exception {

        // Invoke the next entity in the chain
        final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        final GenericResponseWrapper wrapper = new GenericResponseWrapper(
                response, outstr);
        chain.doFilter(request, wrapper);
        wrapper.flush();

        long timeToLiveSeconds = 360000l;

        // Return the page info
        return new PageInfo(wrapper.getStatus(), wrapper.getContentType(),
                wrapper.getCookies(), outstr.toByteArray(), true,
                timeToLiveSeconds, wrapper.getAllHeaders());
    }

    /**
     * Writes the response from a PageInfo object.
     * <p/>
     * Headers are set last so that there is an opportunity to override
     * 
     * @param request
     * @param response
     * @param pageInfo
     * @throws IOException
     * @throws DataFormatException
     * @throws ResponseHeadersNotModifiableException
     * 
     */
    protected void writeResponse(final HttpServletRequest request,
            final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, DataFormatException,
            ResponseHeadersNotModifiableException {
        boolean requestAcceptsGzipEncoding = acceptsGzipEncoding(request);

        setStatus(response, pageInfo);
        setContentType(response, pageInfo);
        setCookies(pageInfo, response);
        // do headers last so that users can override with their own header sets
        setHeaders(pageInfo, requestAcceptsGzipEncoding, response);
        writeContent(request, response, pageInfo);
    }

    /**
     * Set the content type.
     * 
     * @param response
     * @param pageInfo
     */
    protected void setContentType(final HttpServletResponse response,
            final PageInfo pageInfo) {
        String contentType = pageInfo.getContentType();
        if (contentType != null && contentType.length() > 0) {
            response.setContentType(contentType);
        }
    }

    /**
     * Set the serializableCookies
     * 
     * @param pageInfo
     * @param response
     */
    protected void setCookies(final PageInfo pageInfo,
            final HttpServletResponse response) {

        final Collection<SerializableCookie> cookies = pageInfo.getSerializableCookies();
        for (Iterator<SerializableCookie> iterator = cookies.iterator(); iterator.hasNext();) {
            final Cookie cookie = ((SerializableCookie) iterator.next())
                    .toCookie();
            response.addCookie(cookie);
        }
    }

    /**
     * Status code
     * 
     * @param response
     * @param pageInfo
     */
    protected void setStatus(final HttpServletResponse response,
            final PageInfo pageInfo) {
        response.setStatus(pageInfo.getStatusCode());
    }

    /**
     * Set the headers in the response object, excluding the Gzip header
     * 
     * @param pageInfo
     * @param requestAcceptsGzipEncoding
     * @param response
     */
    protected void setHeaders(final PageInfo pageInfo,
            boolean requestAcceptsGzipEncoding,
            final HttpServletResponse response) {

        final Collection<Header<? extends Serializable>> headers = pageInfo
                .getHeaders();

        // Track which headers have been set so all headers of the same name
        // after the first are added
        final TreeSet<String> setHeaders = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);

        for (final Header<? extends Serializable> header : headers) {
            final String name = header.getName();

            switch (header.getType()) {
            case STRING:
                if (setHeaders.contains(name)) {
                    response.addHeader(name, (String) header.getValue());
                } else {
                    setHeaders.add(name);
                    response.setHeader(name, (String) header.getValue());
                }
                break;
            case DATE:
                if (setHeaders.contains(name)) {
                    response.addDateHeader(name, (Long) header.getValue());
                } else {
                    setHeaders.add(name);
                    response.setDateHeader(name, (Long) header.getValue());
                }
                break;
            case INT:
                if (setHeaders.contains(name)) {
                    response.addIntHeader(name, (Integer) header.getValue());
                } else {
                    setHeaders.add(name);
                    response.setIntHeader(name, (Integer) header.getValue());
                }
                break;
            default:
                throw new IllegalArgumentException("No mapping for Header: "
                        + header);
            }
        }
    }

	

    /**
     * Writes the response content. This will be gzipped or non gzipped
     * depending on whether the User Agent accepts GZIP encoding.
     * <p/>
     * If the body is written gzipped a gzip header is added.
     * 
     * @param response
     * @param pageInfo
     * @throws IOException
     */
    protected void writeContent(final HttpServletRequest request,
            final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, ResponseHeadersNotModifiableException {
        byte[] body;

        boolean shouldBodyBeZero = ResponseUtil.shouldBodyBeZero(request,
                pageInfo.getStatusCode());
        if (shouldBodyBeZero) {
            body = new byte[0];
        } else if (acceptsGzipEncoding(request)) {
            body = pageInfo.getGzippedBody();
            if (ResponseUtil.shouldGzippedBodyBeZero(body, request)) {
                body = new byte[0];
            } else {
                ResponseUtil.addGzipHeader(response);
            }

        } else {
            body = pageInfo.getUngzippedBody();
        }

        response.setContentLength(body.length);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(body);
        out.flush();
    }
    
    protected boolean acceptsGzipEncoding(HttpServletRequest request) {
        return acceptsEncoding(request, "gzip");
    }
    /**
     * Logs the request headers, if debug is enabled.
     *
     * @param request
     */
    protected void logRequestHeaders(final HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            Map<String,String> headers = new HashMap<>();
            Enumeration<String> enumeration = request.getHeaderNames();
            StringBuffer logLine = new StringBuffer();
            logLine.append("Request Headers");
            while (enumeration.hasMoreElements()) {
                String name = (String) enumeration.nextElement();
                String headerValue = request.getHeader(name);
                headers.put(name, headerValue);
                logLine.append(": ").append(name).append(" -> ").append(headerValue);
            }
            LOG.debug(logLine.toString());
        }
    }
    /**
     * Checks if request accepts the named encoding.
     */
    protected boolean acceptsEncoding(final HttpServletRequest request, final String name) {
        final boolean accepts = headerContains(request, "Accept-Encoding", name);
        return accepts;
    }
    /**
     * Checks if request contains the header value.
     */
    private boolean headerContains(final HttpServletRequest request, final String header, final String value) {

        logRequestHeaders(request);

        final Enumeration<String> accepted = request.getHeaders(header);
        while (accepted.hasMoreElements()) {
            final String headerValue = (String) accepted.nextElement();
            if (headerValue.indexOf(value) != -1) {
                return true;
            }
        }
        return false;
    }

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	

	@Override
	public void destroy() {
		
	}

	private String getCacheName() {
		return this.cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}
}
