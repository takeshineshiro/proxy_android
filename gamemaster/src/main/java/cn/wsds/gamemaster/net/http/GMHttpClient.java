package cn.wsds.gamemaster.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executor;

import android.os.AsyncTask;
import android.os.SystemClock;
import cn.wsds.gamemaster.data.SessionInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.service.HttpApiService;

import com.subao.common.Misc;
import com.subao.common.net.Http;
import com.subao.common.utils.StringUtils;

/**
 * Created by lidahe on 15/12/20.
 */
public class GMHttpClient {
    public static final String TAG = "GMHttpClient";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_RANGE = "Content-Range";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    public static final int DEFAULT_MAX_CONNECTIONS = 10;
    public static final int DEFAULT_SOCKET_TIMEOUT = 10 * 1000;
    public static final int DEFAULT_MAX_RETRIES = 5;
    public static final int DEFAULT_RETRY_SLEEP_TIME_MILLIS = 1500;
    public static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;
    
    public static boolean get(ResponseHandler callback, URL url) {
    	List<RequestProperty> headers = XwsseRequestPropertiesCreater.create();
    	return get(headers, callback, url);
    }
    
    /**
     * Perform a HTTP GET request and track the Android Context which initiated the request with
     * customized headers
     *
     * @param headers         set headers only for this request
     * @param callback the response handler instance that should handle the response.
     * @param url             the URL to send the request to.
     * @return 
     */
    public static boolean get(List<RequestProperty> headers,ResponseHandler callback, URL url) {
    	 return Requestor.createAndExecute(headers,callback, url, Http.Method.GET);
    }
    public static boolean post(ResponseHandler callback, URL url) {
		return post(callback, url, null);
	}
    
    public static boolean post(ResponseHandler callback, URL url,byte[] postData) {
		List<RequestProperty> headers = XwsseRequestPropertiesCreater.create();
		return post(headers, callback, url, postData);
	}
    
    /**
     * Perform a HTTP POST request and track the Android Context which initiated the request. Set
     * headers only for this request
     *
     * @param headers         set headers only for this request
     * @param callback  the response handler instance that should handle the response.
     * @param url             the URL to send the request to.
     * @param postData  post http data
     * @return 
     */
    public static boolean post(List<RequestProperty> headers,ResponseHandler callback, URL url, byte[] postData) {
    	 return Requestor.createAndExecute(headers,callback, url, Http.Method.POST, postData);
    }
    
    public static boolean delete(ResponseHandler callback, URL url) {
    	List<RequestProperty> headers = XwsseRequestPropertiesCreater.create();
		return delete(headers, callback, url);
	}
    
    public static boolean delete(List<RequestProperty> headers,ResponseHandler callback, URL url) {
		return Requestor.createAndExecute(headers,callback, url, Http.Method.DELETE);
	}

	/**
	 * 帐号系统相关的AsyncTask
	 */
	public static final class Requestor extends AsyncTask<Void, Void, Response> {
	
		private ResponseHandler callback;
		private final URL url;
		private final Http.Method method;
		private final byte[] postData;
		private final List<RequestProperty> headers;
		
		/**
		 * 创建一个帐号系统相关的AsyncTask并启动它
		 *
		 * @param callback
		 *            回调
		 * @param url
		 *            URL
		 * @param method
		 *            Http方法
		 * @return
		 * @throws IllegalArgumentException 
		 *     method must not be null , ResponseHandler must not be null,url must not be null. 
		 */
		public static boolean createAndExecute(List<RequestProperty> headers,ResponseHandler callback, URL url, Http.Method method) {
			return createAndExecute(headers,callback, url, method, null);
		}
	
		/**
		 * 创建一个帐号系统相关的AsyncTask并启动它
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
		 *     method must not be null , ResponseHandler must not be null,url must not be null. 
		 */
		public static boolean createAndExecute(List<RequestProperty> headers,ResponseHandler callback, URL url, Http.Method method, byte[] postData) {
			if (method == null) {
	            throw new IllegalArgumentException("method must not be null");
	        }
			
			if(callback == null){
				throw new IllegalArgumentException("callback must not be null");
			}
			
			if (url == null) {
				throw new IllegalArgumentException("url must not be null.");
			}
			Requestor task = new Requestor(headers,callback, url, method, postData);
			task.executeOnExecutor(SerialExecutor.instance);
			return true;
		}
	
		private Requestor(List<RequestProperty> headers, ResponseHandler callback, URL url, Http.Method method, byte[] postData) {
			this.headers = headers;
			this.callback = callback;
			this.url = url;
			this.method = method;
			this.postData = postData;
		}
	
		@Override
		protected void onPreExecute() {
			if (callback != null) {
				callback.start();
			}
		}
	
		@Override
		protected Response doInBackground(Void... params) {
//			Log.i(TAG, "request url:"+url.toString());
			HttpURLConnection conn = null;
			try {
				conn = createHttpUrlConnection(url, method, 10000, 10000);
				if(headers!=null){
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
				return readDataFromURLConnection(conn);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
			return null;
		}
	
		public static Response readDataFromURLConnection(HttpURLConnection connection) {
			int code = 0;
			try {
				code = connection.getResponseCode();
				//Logger.d(LogTag.NET, "Response Code: " + code);
			} catch (IOException e) {
				e.printStackTrace();
				// 某些手机 发生 IOException “No authentication challenges found” 需要再次重读下即可
				try {
					code = connection.getResponseCode();
				}catch(IOException io) {
					io.printStackTrace();
					return null;
				}
			} catch (RuntimeException e) {
				// 在某些手机上 抛出了IndexOutOfBoundsException，还有手机抛NumberFormatException				
				e.printStackTrace();
				return null;
			}
	
			byte[] data = getBodyData(connection);
			if(code==0 && data == null){
				return null;
			}else{
				return new Response(null,data,code);
			}
		}

		private static byte[] getBodyData(HttpURLConnection connection) {
			byte[] data = null;
			InputStream in = null;
			try {
				in = connection.getInputStream();
				data = Misc.readStreamToByteArray(in);
			} catch (IOException e) {
				//e.printStackTrace();
			} finally {
				Misc.close(in);
			}
			
			if(data != null){
				return data;
			}
			
			try {
				in = connection.getErrorStream();
				data = Misc.readStreamToByteArray(in);
			} catch (IOException e) {
				//e.printStackTrace();
			} finally {
				Misc.close(in);
			}
			return data;
		}
	
		@Override
		protected void onPostExecute(Response result) {
//			Log.i(TAG, url.toString() + " response code:"+(result==null?-1:result.code));
			if(callback!=null){
				callback.finish(result);
				callback = null;
			}
		}
		
		private HttpURLConnection createHttpUrlConnection(URL url, Http.Method method, int connectTimeout, int readTimeout) throws IOException {
			if (url == null) {
				throw new NullPointerException("url is null");
			}
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if (method != null) {
				conn.setRequestMethod(method.str);
			}
			conn.setConnectTimeout(connectTimeout);
			conn.setReadTimeout(readTimeout);
			conn.setDoInput(true);
			conn.setRequestProperty("Cache-Control", "no-cache");
//			conn.setRequestProperty("Content-Type", "application/x-protobuf");
			conn.setRequestProperty("Content-Type", "application/octet-stream;charset=utf-8");
			conn.setUseCaches(false);
			conn.setInstanceFollowRedirects(true);
			return conn;
		}
	
	}
	
	private static final class SessionRequestPropertiesCreater {
		
		private static final String HTTP_HEAD_FIELD_ACCESS_TOKEN = "accessToken";
		private static final String HTTP_HEAD_FIELD_USER_ID = "userId";
		
		private SessionRequestPropertiesCreater() {}
		
		/**
		 * 在给定的列表里加入Session信息
		 * @param list
		 */
		static List<RequestProperty> appendTo(List<RequestProperty> list) {
			SessionInfo sessionInfo = UserSession.getInstance().getSessionInfo();
	        if (sessionInfo != null) {
	        	if (list == null) {
	        		list = new ArrayList<RequestProperty>(2);
	        	}
	            list.add(new RequestProperty(HTTP_HEAD_FIELD_USER_ID, sessionInfo.getUserId()));
	            list.add(new RequestProperty(HTTP_HEAD_FIELD_ACCESS_TOKEN, sessionInfo.getAccessToken()));
	        }
	        return list;
		}
	}

	/**
	 * 静态类，负责创建请求头里各必需字段
	 */
	public static final class XwsseRequestPropertiesCreater {
		
		/**
		 * 使用当前时刻为请求时间戳，且不在请求头里加入Session信息
		 * @see #create(String, boolean)
		 */
		public static List<RequestProperty> create() {
			return create(getUTCTimestamp(), false);
		}
		
		/**
		 * 使用当前时刻为请求时间戳
		 * @param addSessionHeaders 是否在请求头里加入Session信息？
		 */
		public static List<RequestProperty> create(boolean addSessionHeaders) {
			return create(getUTCTimestamp(), addSessionHeaders);
		}
		
		/**
		 * 使用指定的时刻创建请求头各字段
		 * @param timestamp 当前UTC时刻的文本表示，参见 {@link #getUTCTimestamp()}
		 * @param addSessionHeaders 是否加入Session头？
		 * @return 请求头各字段的列表
		 * @see #getUTCTimestamp()
		 */
		public static List<RequestProperty> create(String timestamp, boolean addSessionHeaders) {
			List<RequestProperty> xwsseRequestProperties = null;
			try {
				xwsseRequestProperties = createRequestProperties(timestamp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (addSessionHeaders) {
				xwsseRequestProperties = SessionRequestPropertiesCreater.appendTo(xwsseRequestProperties);
			}
			return xwsseRequestProperties;
		}

		private static List<RequestProperty> createRequestProperties(String timestamp) throws IOException {
			List<RequestProperty> requestProperties = new ArrayList<RequestProperty>();
			requestProperties.add(new RequestProperty("Authorization", "WSSE profile=\"UsernameToken\""));
			requestProperties.add(new RequestProperty("X-WSSE",  getXWSSE(timestamp)));
			return requestProperties;
		}

		private static String getXWSSE(String timestamp) throws IOException {
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("SHA1");
			} catch (NoSuchAlgorithmException e) {
				throw new IOException("SHA1 encoding failed");
			}
			String nonce = StringUtils.toHexString(md.digest(("SuBao" + SystemClock.elapsedRealtime()).getBytes()), false);
			StringBuilder sb = new StringBuilder(512);
			sb.append(nonce);
			sb.append(timestamp);
			appendXWSSEPassword(sb);

			String digest = HttpApiService.encodeBySHA1(sb.toString());
			sb.setLength(0);
			sb.append("UsernameToken Username=\"");
			appendXWSSEUserName(sb).append("\", PasswordDigest=\"");
			sb.append(digest).append("\", Nonce=\"").append(nonce);
			sb.append("\", Created=\"").append(timestamp).append('"');
			return sb.toString();
		}

		/**
		 * 取当前UTC时刻的文本表示
		 * @return 形如<b>"2016-01-25T13:23:45Z"</b>的UTC当前时刻
		 */
		public static String getUTCTimestamp() {
			Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			return String.format("%04d-%02d-%02dT%02d:%02d:%02dZ",
				now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH),
				now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
		}
		
		private static StringBuilder appendXWSSEUserName(StringBuilder sb) {
			sb.append('G');
			sb.append('a');
			sb.append("me");
			return sb;
		}
	
		private static StringBuilder appendXWSSEPassword(StringBuilder sb) {
			sb.append("!Peq");
			sb.append('c');
			sb.append("hdka()z?");
			return sb;
		}
		
	}

    public static class SerialExecutor implements Executor {
    	
    	public static final SerialExecutor instance = new SerialExecutor();
    	
        private final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        private Runnable mActive;
        
        private SerialExecutor() { }

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        private synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                com.subao.common.thread.ThreadPool.getExecutor().execute(mActive);
            }
        }
    }
}
