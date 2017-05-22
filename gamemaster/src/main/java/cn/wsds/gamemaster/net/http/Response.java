package cn.wsds.gamemaster.net.http;

import java.util.List;

/**
 * http request response obj 
 */
public class Response {
	
	/**
	 * default http status code
	 */
	public static final int DEFAULT_HTTP_STATUS_CODE = -1; 
    
	/**
     * headers files
     */
	public final List<RequestProperty> requestProperties;
	
	/**
	 * the body of the HTTP response from the server
	 */
	
	public final byte[] body;
	/**
	 * HTTP status code or {@link DEFAULT_HTTP_STATUS_CODE}
	 */
	public final int code;
	
//	public Response() {
//		this(null, null, DEFAULT_HTTP_STATUS_CODE);
//	}

	public Response (List<RequestProperty> requestProperties,byte[] responseBody, int code) {
		this.requestProperties = requestProperties;
		this.body = responseBody;
		this.code = code;
	}
}