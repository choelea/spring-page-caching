package com.github.choelea.pagecache;


import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class GenericResponseWrapper extends HttpServletResponseWrapper implements Serializable {

    private static final long serialVersionUID = -5976708169031065497L;

    private static final Logger LOG = LoggerFactory.getLogger(GenericResponseWrapper.class);
    private int statusCode = SC_OK;
    private int contentLength;
    private String contentType;
    private final Map<String, List<Serializable>> headersMap = new TreeMap<String, List<Serializable>>(String.CASE_INSENSITIVE_ORDER);
    private final List<Cookie> cookies = new ArrayList<>();
    private ServletOutputStream outstr;
    private PrintWriter writer;
    private boolean disableFlushBuffer = true;

    /**
     * Creates a GenericResponseWrapper
     * @param response
     * @param outstr 
     */
    public GenericResponseWrapper(final HttpServletResponse response, final OutputStream outstr) {
        super(response);
        this.outstr = new FilterServletOutputStream(outstr);
    }

    /**
     * Gets the outputstream.
     */
    public ServletOutputStream getOutputStream() {
        return outstr;
    }

    /**
     * Sets the status code for this response.
     */
    public void setStatus(final int code) {
        statusCode = code;
        super.setStatus(code);
    }

    /**
     * Send the error. If the response is not ok, most of the logic is bypassed and the error is sent raw
     * Also, the content is not cached.
     *
     * @param i      the status code
     * @param string the error message
     * @throws IOException
     */
    public void sendError(int i, String string) throws IOException {
        statusCode = i;
        super.sendError(i, string);
    }

    /**
     * Send the error. If the response is not ok, most of the logic is bypassed and the error is sent raw
     * Also, the content is not cached.
     *
     * @param i the status code
     * @throws IOException
     */
    public void sendError(int i) throws IOException {
        statusCode = i;
        super.sendError(i);
    }

    /**
     * Send the redirect. If the response is not ok, most of the logic is bypassed and the error is sent raw.
     * Also, the content is not cached.
     *
     * @param string the URL to redirect to
     * @throws IOException
     */
    public void sendRedirect(String string) throws IOException {
        statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
        super.sendRedirect(string);
    }

    /**
     * Sets the status code for this response.
     */
    public void setStatus(final int code, final String msg) {
        statusCode = code;
        LOG.warn("Discarding message because this method is deprecated.");
        super.setStatus(code);
    }

    /**
     * Returns the status code for this response.
     */
    public int getStatus() {
        return statusCode;
    }

    /**
     * Sets the content length.
     */
    public void setContentLength(final int length) {
        this.contentLength = length;
        super.setContentLength(length);
    }

    /**
     * Gets the content length.
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Sets the content type.
     */
    public void setContentType(final String type) {
        this.contentType = type;
        super.setContentType(type);
    }

    /**
     * Gets the content type.
     */
    public String getContentType() {
        return contentType;
    }


    /**
     * Gets the print writer.
     */
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(outstr, getCharacterEncoding()), true);
        }
        return writer;
    }



    /**
     * @see javax.servlet.http.HttpServletResponseWrapper#addHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void addHeader(String name, String value) {
        List<Serializable> values = this.headersMap.get(name);
        if (values == null) {
            values = new LinkedList<Serializable>();
            this.headersMap.put(name, values);
        }
        values.add(value);
        
        super.addHeader(name, value);
    }

    /**
     * @see javax.servlet.http.HttpServletResponseWrapper#setHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void setHeader(String name, String value) {
        final LinkedList<Serializable> values = new LinkedList<Serializable>();
        values.add(value);
        this.headersMap.put(name, values);
        
        super.setHeader(name, value);
    }

    /**
     * @see javax.servlet.http.HttpServletResponseWrapper#addDateHeader(java.lang.String, long)
     */
    @Override
    public void addDateHeader(String name, long date) {
        List<Serializable> values = this.headersMap.get(name);
        if (values == null) {
            values = new LinkedList<Serializable>();
            this.headersMap.put(name, values);
        }
        values.add(date);
        
        super.addDateHeader(name, date);
    }

    /**
     * @see javax.servlet.http.HttpServletResponseWrapper#setDateHeader(java.lang.String, long)
     */
    @Override
    public void setDateHeader(String name, long date) {
        final LinkedList<Serializable> values = new LinkedList<Serializable>();
        values.add(date);
        this.headersMap.put(name, values);
        
        super.setDateHeader(name, date);
    }

    /**
     * @see javax.servlet.http.HttpServletResponseWrapper#addIntHeader(java.lang.String, int)
     */
    @Override
    public void addIntHeader(String name, int value) {
        List<Serializable> values = this.headersMap.get(name);
        if (values == null) {
            values = new LinkedList<Serializable>();
            this.headersMap.put(name, values);
        }
        values.add(value);
        
        super.addIntHeader(name, value);
    }

    /**
     * @see javax.servlet.http.HttpServletResponseWrapper#setIntHeader(java.lang.String, int)
     */
    @Override
    public void setIntHeader(String name, int value) {
        final LinkedList<Serializable> values = new LinkedList<Serializable>();
        values.add(value);
        this.headersMap.put(name, values);
        
        super.setIntHeader(name, value);
    }
    
    

    /**
     * @return All of the headersMap set/added on the response
     */
    public Collection<Header<? extends Serializable>> getAllHeaders() {
        final List<Header<? extends Serializable>> headers = new LinkedList<Header<? extends Serializable>>();
        
        for (final Map.Entry<String, List<Serializable>> headerEntry : this.headersMap.entrySet()) {
            final String name = headerEntry.getKey();
            for (final Serializable value : headerEntry.getValue()) {
                final Header.Type type = Header.Type.determineType(value.getClass());
                switch (type) {
                    case STRING:
                        headers.add(new Header<String>(name, (String)value));
                    break;
                    case DATE:
                        headers.add(new Header<Long>(name, (Long)value));
                    break;
                    case INT:
                        headers.add(new Header<Integer>(name, (Integer)value));
                    break;
                    default:
                        throw new IllegalArgumentException("No mapping for Header.Type: " + type);
                }
            }
        }
        
        return headers;
    }

    /**
     * Adds a cookie.
     */
    public void addCookie(final Cookie cookie) {
        cookies.add(cookie);
        super.addCookie(cookie);
    }

    /**
     * Gets all the cookies.
     */
    public Collection<Cookie> getCookies() {
        return cookies;
    }

    /**
     * Flushes buffer and commits response to client.
     */
    public void flushBuffer() throws IOException {
        flush();
        
        // doing this might leads to response already committed exception
        // when the PageInfo has not yet built but the buffer already flushed
        // Happens in Weblogic when a servlet forward to a JSP page and the forward
        // method trigger a flush before it forwarded to the JSP
        // disableFlushBuffer for that purpose is 'true' by default
        // EHC-447
        if (!disableFlushBuffer) {
            super.flushBuffer();
        }
    }

    /**
     * Resets the response.
     */
    public void reset() {
        super.reset();
        cookies.clear();
        headersMap.clear();
        statusCode = SC_OK;
        contentType = null;
        contentLength = 0;
    }

    /**
     * Resets the buffers.
     */
    public void resetBuffer() {
        super.resetBuffer();
    }

    /**
     * Flushes all the streams for this response.
     */
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        outstr.flush();
    }

    /**
     * Is the wrapped reponse's buffer flushing disabled?
     * @return true if the wrapped reponse's buffer flushing disabled
     */
    public boolean isDisableFlushBuffer() {
        return disableFlushBuffer;
    }

    /**
     * Set if the wrapped reponse's buffer flushing should be disabled.
     * @param disableFlushBuffer true if the wrapped reponse's buffer flushing should be disabled
     */
    public void setDisableFlushBuffer(boolean disableFlushBuffer) {
        this.disableFlushBuffer = disableFlushBuffer;
    }
}


