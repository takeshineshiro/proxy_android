package cn.wsds.gamemaster.socket;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.content.Context;
import android.os.AsyncTask;
import cn.wsds.gamemaster.vpn.JniCallbackProcesser;

import com.subao.common.Logger;

public class SocketClient {

	private static final String TAG = "SubaoSocketClient";

	private static final SocketClient instance = new SocketClient();

	private Worker worker;

	public static SocketClient getInstance() {
		return instance;
	}
	
	private SocketClient() {
		
	}

	public void start(Context context, int port) {
		JniCallbackProcesser.createInstance(context);
		if (worker == null) {
			worker = new Worker();
			worker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, port);
			Logger.d(TAG, "Client start!");
		}
	}

	public void stop() {
		if (worker != null) {
			worker.requestTerminate();
			worker = null;
			Logger.d(TAG, "Client stop!");
		}
	}

	private static class Worker extends AsyncTask<Integer, Void, Void> {

        private static final int PACKAGE_HEADER_SIZE = 4;
        private Socket socket;
		private int socketLocalPort;

		private byte[] readPakcage(InputStream input, ByteBuffer buffer) throws IOException {
			byte[] buf = buffer.array();
			//读4字节头
			readn(input, buf, PACKAGE_HEADER_SIZE);
			buffer.position(0);
			int packageSize = buffer.getInt();
			if (packageSize <= 0 || packageSize > buffer.capacity()) {
				throw new IOException("Read invalid package size");
			}
			//读取body
			readn(input, buf, packageSize);
			// 已读到完整的Package了
			return Arrays.copyOfRange(buf, 0, packageSize);
		}

		@Override
		protected Void doInBackground(Integer... params) {
			try {
				InputStream input;
				synchronized (this) {
					if (this.isCancelled()) {
						return null;
					}
					this.socket = new Socket(SocketServer.createListenAddress(), params[0]);
					this.socketLocalPort = socket.getLocalPort();
					input = this.socket.getInputStream();
					Logger.d(TAG, "Connect to server ok, local port: " + socketLocalPort);
				}
				ByteBuffer buffer = ByteBuffer.allocate(SocketServer.MAX_PACKAGE_SIZE); // 注意：使用默认的大头序
				while (!this.isCancelled()) {
					byte[] pack = readPakcage(input, buffer);
					if (pack == null) {
						break;
					}

					SocketDataParser.parse(pack);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			cleanup();
		}

		@Override
		protected void onCancelled(Void result) {
			cleanup();
		}

		public void requestTerminate() {
			if (!this.isCancelled()) {
				this.cancel(true);
				cleanup();
			}
		}

		private synchronized void cleanup() {
			if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
				Logger.d(TAG, String.format("Client #%d closed", socketLocalPort));
			}
		}
	}

    /**
     * 读n字节的数据到buffer中（目标起始位置总是buffer的0偏移处）
     */
    private static void readn(InputStream input, byte[] buffer, int size) throws IOException {
        int left = size;
        int pos = 0;
        while (left > 0) {
        	int read = input.read(buffer, pos, left);
        	if (read <= 0) {
        		throw new IOException();
        	}
            left -= read;
            pos += read;
        }
    }

}
