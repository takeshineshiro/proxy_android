package cn.wsds.gamemaster.socket;

import android.os.AsyncTask;

import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.thread.ThreadPool;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketServer {

	public static final int MAX_PACKAGE_SIZE = 12 * 1024;
	public static final int INVALIDPORT = -1;

	private static final SocketServer instance = new SocketServer();

	private static final String TAG = "SubaoSocketServer";

	private Worker worker;

	public static SocketServer getInstance() {
		return instance;
	}
	
	private SocketServer() {
		
	}

	public static InetAddress createListenAddress() throws UnknownHostException {
		return InetAddress.getByAddress(new byte[] {
			127,
			0,
			0,
			1
		});
	}

	public void start() {
		if (worker == null || worker.isCancelled()) {
			worker = new Worker();
			worker.executeOnExecutor(ThreadPool.getExecutor());
			worker.startSender();
			Logger.d(TAG, "Server start!");
		}
	}

	public void stop() {
		if (worker != null) {
			worker.requestTerminate();
			Logger.d(TAG, "Server stop!");
			worker = null;
		}
	}

	public void postData(byte[] data) {
		if (worker != null) {
			worker.postData(data);
		}
	}

	public int getPort() {
		if (worker == null) {
			return INVALIDPORT;
		}
		return worker.getListenPort();
	}

	/**
	 * 负责发送数据的线程
	 */
	private static class Sender extends AsyncTask<Void, Void, Void> {

		private static class Client implements Closeable {

			public final int id;
			private final Socket socket;
			private final OutputStream output;

			public Client(Socket socket) throws IOException {
				this.id = socket.getPort();
				this.socket = socket;
				this.output = socket.getOutputStream();
			}

			public void write(byte[] pack) throws IOException {
				int packageSize = pack.length;
				if (packageSize > MAX_PACKAGE_SIZE) {
					throw new IOException(String.format("Try to send invalid package (size = %d)", packageSize));
				}
				output.write((packageSize >> 24) & 0xff);
				output.write((packageSize >> 16) & 0xff);
				output.write((packageSize >> 8) & 0xff);
				output.write(packageSize & 0xff);
				output.write(pack);
				output.flush();
			}

			@Override
			public void close() throws IOException {
				socket.close();
			}
		}

		/** 数据队列 */
		private final Queue<byte[]> packageList = new ConcurrentLinkedQueue<>();

		/** 客户端Socket */
		private final Queue<Client> clients = new ConcurrentLinkedQueue<>();

		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				byte[] pack = this.packageList.poll();
				if (pack == null) {
					synchronized (this) {
						try {
							this.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					continue;
				}
				Iterator<Client> it = this.clients.iterator();
				while (!isCancelled() && it.hasNext()) {
					Client client = it.next();
					if (client == null) {
						break;
					}
					try {
						client.write(pack);
					} catch (IOException e) {
						it.remove();
						Misc.close(client);
						Logger.d(TAG, String.format("Client #%d disconnected", client.id));
					}
				}
			}
			cleanup();
			Logger.d(TAG, "Sender terminated");
			return null;
		}

		private void cleanup() {
			packageList.clear();
			while (true) {
				Client client = clients.poll();
				if (client == null) {
					break;
				}
				Misc.close(client);
			}
		}

		public boolean addClient(Socket socket) {
			if (this.isCancelled()) {
				return false;
			}
			Client client;
			try {
				client = new Client(socket);
				this.clients.offer(client);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}

		public void addData(byte[] data) {
			if (!this.isCancelled()) {
				this.packageList.offer(data);
				notifySelf();
			}
		}

		public void requestTerminate() {
			if (!this.isCancelled()) {
				this.cancel(true);
				notifySelf();
			}
		}
		
		private void notifySelf() {
			synchronized (this) {
				this.notify();
			}
		}
	}

	/**
	 * 负责Listen & Accept的工作线程
	 */
	private static class Worker extends AsyncTask<Void, Void, Void> {

		private final Sender sender = new Sender();
		private volatile ServerSocket serverSocket;
		private volatile int port = INVALIDPORT;

		/**
		 * 创建一个侦听127.0.0.1随机端口的TCP-Server-Socket
		 * 
		 * @return null表示失败
		 */
		private static ServerSocket createServerSocket() {
			try {
				return new ServerSocket(0, 5, createListenAddress());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			//
			// 创建ServerSocket
			synchronized (this) {
				if (this.isCancelled()) {
					return null;
				}
				this.serverSocket = createServerSocket();
				if (this.serverSocket == null) {
					return null;
				}
				this.port = serverSocket.getLocalPort();
			}
			Logger.d(TAG, String.format("Server listen succeed, port = %d", this.port));

			// 循环，直到外部通知结束
			while (!isCancelled()) {
				ServerSocket serverSocket = this.serverSocket;
				if (serverSocket == null || serverSocket.isClosed()) {
					break;
				}
				try {
					Socket client = serverSocket.accept();
					Logger.d(TAG, "Accept client: " + client.getPort());
					if (!sender.addClient(client)) {
						client.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
					if (isCancelled() || serverSocket.isClosed()) {
						// 如果是因为serverSocket被关闭（要退出）而导致的，退出本循环
						break;
					} else {
						// 否则休眠一下（防止可能出现的CPU高占用）
						try {
							Thread.sleep(1000);
						} catch (InterruptedException inter) {
							inter.printStackTrace();
						}
					}
				}
			}
			//
			// 结束Sender
			sender.requestTerminate();
			//
			cleanup();
			Logger.d(TAG, "Listen terminated");
			return null;
		}

		/**
		 * 返回ServerSocket侦听的端口
		 * 
		 * @return port
		 */
		public int getListenPort() {
			return this.port;
		}

		/**
		 * 请求结束工作
		 */
		public void requestTerminate() {
			if (!this.isCancelled()) {
				this.cancel(true);
				cleanup();
			}
		}

		/**
		 * 发送数据
		 */
		public void postData(byte[] data) {
			sender.addData(data);
		}

		private void cleanup() {
			synchronized (this) {
				if (serverSocket != null) {
					try {
						serverSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					serverSocket = null;
					this.port = INVALIDPORT;
				}
			}
		}

		private void startSender(){
			// 启动Sender
			sender.executeOnExecutor(ThreadPool.getExecutor());
			//
		}

	}
}
