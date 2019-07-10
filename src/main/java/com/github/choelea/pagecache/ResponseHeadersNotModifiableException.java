package com.github.choelea.pagecache;

public class ResponseHeadersNotModifiableException extends Exception {

    
	private static final long serialVersionUID = 8358888691143786054L;

	
    public ResponseHeadersNotModifiableException() {
        super();
    }

    
    public ResponseHeadersNotModifiableException(String message) {
        super(message);
    }
}

