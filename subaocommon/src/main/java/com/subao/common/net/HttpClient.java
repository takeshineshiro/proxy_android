package com.subao.common.net;

import android.os.AsyncTask;

import com.subao.common.Misc;
import com.subao.common.thread.ThreadPool;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * http公共入口（包括https） Created by hujd on 16-7-1.
 */
public class HttpClient {
	private static final int HTTP_TIMEOUT = 10 * 1000;

	/**
	 * Http相关接口初始化
	 */
	public static  void init(Http.StrictVerifier strictVerifier) {
		Http.setStrictVerifier(strictVerifier);
	}

	/**
	 * Perform a HTTP GET request and track the Android Context which initiated
	 * the request with customized headers
	 * 
	 * @param headers
	 *            set headers only for this request
	 * @param callback
	 *            the response handler instance that should handle the response.
	 * @param url
	 *            the URL to send the request to.
	 * @return
	 */
	public static void get(List<RequestProperty> headers, ResponseCallback callback, String url) {
		Requestor.createAndExecute(headers, callback, url, Http.Method.GET, null);
	}

	/**
	 * 向服务器发送post请求
	 * 
	 * @param headers
	 *            请求头
	 * @param callback
	 *            回调
	 * @param url
	 *            url
	 * @param postData
	 *            body
	 * @return
	 */
	public static void post(List<RequestProperty> headers, ResponseCallback callback, String url, byte[] postData) {
		Requestor.createAndExecute(headers, callback, url, Http.Method.POST, postData);
	}

	/**
	 * 向服务器发送delete请求
	 *
	 * @param headers
	 *            请求头
	 * @param callback
	 *            回调
	 * @param url
	 *            url
	 */
	public static void delete(List<RequestProperty> headers, ResponseCallback callback, String url) {
		Requestor.createAndExecute(headers, callback, url, Http.Method.DELETE, null);
	}

	static final class Requestor extends AsyncTask<Void, Void, Http.Response> {

		private ResponseCallback callback;

		private final String url;
		private final Http.Method method;
		private final byte[] postData;
		private final List<RequestProperty> headers;

		public Requestor(ResponseCallback callback, String url, Http.Method method, byte[] postData,
			List<RequestProperty> headers) {
			if (method == null) {
				throw new NullPointerException("method must not be null");
			}
			if (callback == null) {
				throw new NullPointerException("callback must not be null");
			}
			if (url == null) {
				throw new NullPointerException("url must not be null.");
			}
			this.callback = callback;
			this.url = url;
			this.method = method;
			this.postData = postData;
			this.headers = headers;
		}

		/**
		 * 相关的AsyncTask并启动它
		 * 
		 * @param callback
		 *            回调
		 * @param url
		 *            URL
		 * @param method
		 *            Http方法
		 * @param postData
		 *            要Post到服务器的数据
		 * @return
		 * @throws IllegalArgumentException
		 *             method must not be null , ResponseHandler must not be
		 *             null,url must not be null.
		 */
        private static void createAndExecute(
            List<RequestProperty> headers,
            ResponseCallback callback,
            String url,
            Http.Method method,
            byte[] postData
        ) {
            new Requestor(callback, url, method, postData, headers).executeOnExecutor(ThreadPool.getExecutor());
        }

		@Override
		protected Http.Response doInBackground(Void... params) {
			try {
				return httpHandle();
			} catch (IOException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Http.Response response) {
			if (response != null) {
				callback.onResponse(response);
			} else {
				callback.doIOException();
			}
		}

		Http.Response httpHandle() throws IOException {
			HttpURLConnection conn = null;
			try {
				conn = new Http(HTTP_TIMEOUT, HTTP_TIMEOUT).createHttpUrlConnection(Http.createURL(url), method, Http.ContentType.JSON.str);
				if (headers != null) {
					for (RequestProperty rq : headers) {
						conn.addRequestProperty(rq.field, rq.newValue);
					}
				}
				if (postData != null && postData.length > 0) {
					conn.setDoOutput(true);
					conn.setFixedLengthStreamingMode(postData.length);
					OutputStream output = null;
					try {
						output = conn.getOutputStream();
						output.write(postData);
						output.flush();
					} finally {
						Misc.close(output);
					}
				}
				return Http.readDataFromURLConnection(conn);
			} catch (RuntimeException e) {
				// 如果权限被禁用，会抛出RuntimeException
				throw new NetIOException();
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
		}

	}
}
