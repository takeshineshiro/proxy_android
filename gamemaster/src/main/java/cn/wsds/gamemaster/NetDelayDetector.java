package cn.wsds.gamemaster;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;

import com.subao.common.SuBaoObservable;
import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

/**
 * 网络延迟检测器
 */
public class NetDelayDetector {

	private static Worker workerTCP, workerUDP;

	public static enum Type {
		TCP,
		UDP
	}

	public interface Observer {
		public void onNetDelayChange(int value, Type type);
	}

	private static class ThreadDomainResolve extends Thread {

		private final String host;
		private final AtomicReference<InetAddress> addr = new AtomicReference<InetAddress>();

		public ThreadDomainResolve(String host) {
			this.host = host;
		}

		@Override
		public void run() {
			InetAddress ia;
			try {
				ia = InetAddress.getByName(host);
			} catch (Exception e) {
				ia = null;
			}
			addr.set(ia);
		}

		public InetAddress getResult() {
			return addr.get();
		}
	}

	private static abstract class Worker extends AsyncTask<Void, Integer, Void> {

		public static final long PERIOD = 5000;

		public final Type type;

		int netDelay = GlobalDefines.NET_DELAY_TEST_WAIT;
		private EventObserver eventObserver;
		private NetTypeDetector.NetType currentNetState = NetTypeDetector.NetType.UNKNOWN;

		private final String[] hosts;
		private final SocketAddress[] addrList;

		private static final class Observers extends SuBaoObservable<NetDelayDetector.Observer> {

			public void notifyObservers(int value, Type type) {
				List<Observer> list = this.cloneAllObservers();
				if (list != null) {
					for (Observer o : list) {
						o.onNetDelayChange(value, type);
					}
				}
			}
		};

		private final Observers observers = new Observers();

		public Worker(String[] hosts, Type type) {
			this.hosts = hosts;
			this.type = type;
			this.addrList = new SocketAddress[hosts.length];
		}
		
		@Override
		protected void onPreExecute() {
			currentNetState = NetManager.getInstance().getCurrentNetworkType();
			eventObserver = new EventObserver() {
				@Override
				public void onNetChange(NetTypeDetector.NetType state) {
					if (currentNetState != state) {
						currentNetState = state;
						clearResolvedAddr();
						if (!isNetConnectionOk(state)) {
							changeDelayValue(GlobalDefines.NET_DELAY_TEST_FAILED);
						}
					}

				};
			};
			TriggerManager.getInstance().addObserver(eventObserver);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			changeDelayValue(values[0]);
		}

		@Override
		protected Void doInBackground(Void... params) {
			int host_index = 0;
			long sleep_time = 0;
			while (!this.isCancelled()) {
				// 没有观察者了，中止线程的运行
				if (observers.isEmpty()) {
					break;
				}
				if (isCancelled()) {
					break;
				}
				if (sleep_time == 0) {
					sleep_time = PERIOD;
				} else {
					sleep(sleep_time);
				}

				//
				// 网络断开吗？
				if (!isNetConnectionOk(currentNetState)) {
					publishProgress(GlobalDefines.NET_DELAY_TEST_FAILED);
					continue;
				}
				//
				// 开了加速吗？
				if (!AccelOpenManager.isStarted()) {
					publishProgress(GlobalDefines.NET_DELAY_TEST_WAIT);
					continue;
				}
				//
				// 检测
				SocketAddress addr = getResolvedAddr(host_index);
				if (addr == null) {
					InetAddress ia = resolveHost(hosts[host_index]);
					if (ia == null) {
						host_index = incHostIndex(host_index);
						publishProgress(GlobalDefines.NET_DELAY_TEST_FAILED);
						continue;
					}
					addr = new InetSocketAddress(ia, getPort());
					setResolvedAddr(host_index, addr);
					if (isCancelled()) {
						break;
					}
				}
				host_index = incHostIndex(host_index);
				try {
					publishProgress(detect(addr));
				} catch (RuntimeException re) {
					publishProgress(GlobalDefines.NET_DELAY_TEST_FAILED);
				}
			}
			return null;
		}

		private int incHostIndex(int idx) {
			++idx;
			if (idx >= hosts.length) {
				return 0;
			} else {
				return idx;
			}
		}

		protected abstract int getPort();

		protected abstract int detect(SocketAddress addr);

		@Override
		protected void onCancelled(Void result) {
			cleanup();
		}

		@Override
		protected void onPostExecute(Void result) {
			cleanup();
		}

		/** 清理工作。（必须在主线程里被调用） */
		private void cleanup() {
			if (eventObserver != null) {
				TriggerManager.getInstance().deleteObserver(eventObserver);
				eventObserver = null;
			}
			observers.unregisterAllObservers();
			if (workerTCP == this) {
				workerTCP = null;
			} else if (workerUDP == this) {
				workerUDP = null;
			}
		}

		//		private long beginValue = -1;
		//		private long beginTime;

		/**
		 * 改变NetDelay的值，如果有改变，通知Listener
		 * <p>
		 * <b>必须在主线程里调用！！！</b>
		 * </p>
		 */
		protected final void changeDelayValue(int value) {
			//			int uid = AppMain.getInstance().getApplicationInfo().uid;
			//			long x = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
			//			if (beginValue < 0) {
			//				beginValue = x;
			//				beginTime = now();
			//			} else {
			//				long seconds = (now() - beginTime) / 1000;
			//				x = x - beginValue;
			//				Log.e("TTT", String.format("流量消耗：%d秒共%d字节 (%d字节/秒)", seconds, x, seconds == 0 ? 0 : x / seconds));
			//			}
			//
			if (value == 0) {
				value = 1;
			}
			// 注意：这里不要进行netDelay是否已经等于value的判断，来决定是否要通知观察者
			// 因为当netDelay可能已经是-1了，但当它变为-1的时候（由onNetChange设置）可能并没有观察者
			netDelay = value;
			observers.notifyObservers(value, this.type);
		}

		protected static final void sleep(long time) {
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {}
		}

		protected static final long now() {
			return SystemClock.elapsedRealtime();
		}

		private static boolean isNetConnectionOk(NetTypeDetector.NetType netState) {
			return netState != NetTypeDetector.NetType.DISCONNECT;
		}

		private SocketAddress getResolvedAddr(int index) {
			SocketAddress result;
			synchronized (addrList) {
				result = addrList[index];
			}
			return result;
		}

		private void setResolvedAddr(int index, SocketAddress addr) {
			synchronized (addrList) {
				addrList[index] = addr;
			}
		}

		private void clearResolvedAddr() {
			synchronized (addrList) {
				for (int i = addrList.length - 1; i >= 0; --i) {
					addrList[i] = null;
				}
			}
		}

		private InetAddress resolveHost(String host) {
			long beginTime = SystemClock.elapsedRealtime();
			ThreadDomainResolve dr = new ThreadDomainResolve(host);
			dr.start();
			while (true) {
				try {
					dr.join(500);
				} catch (InterruptedException e) {}
				if (isCancelled()) {
					return null;
				}
				InetAddress addr = dr.getResult();
				if (addr != null) {
					return addr;
				}
				long now = SystemClock.elapsedRealtime();
				if (now - beginTime >= GlobalDefines.NET_DELAY_TIMEOUT) {
					return null;
				}
			}
		}
	}

	private static class WorkerTCP extends Worker {

		private final static String[] HOST_LIST = new String[] {
			"www.baidu.com",
			"www.163.com",
			"www.sina.com.cn",
			"www.qq.com",
			"www.sohu.com",
			"www.taobao.com",
			"www.jd.com",
			"www.amazon.cn",
			"www.xinhuanet.com",
			"www.360.com",
			"www.kingsoft.com",
		};

		//		private final ByteBuffer request = ByteBuffer.wrap("GET /\r\n\r\n".getBytes());

		public WorkerTCP() {
			super(HOST_LIST, Type.TCP);
		}

		@Override
		protected int getPort() {
			return 80;
		}

		@Override
		protected int detect(SocketAddress addr) {
			Socket socket = null;
			try {
				socket = new Socket();
				long time_1 = now();
				socket.connect(addr, GlobalDefines.NET_DELAY_TIMEOUT);
				//				//
				//				if (!tryRead(channel, selector)) {
				//					return GlobalDefines.NET_DELAY_TIMEOUT;
				//				}
				//
				long time_2 = now();
				long delta = (time_2 - time_1);
				if (delta < 0) {
					delta = 0;
				} else if (delta > GlobalDefines.NET_DELAY_TIMEOUT) {
					delta = GlobalDefines.NET_DELAY_TIMEOUT;
				}
				return (int) delta;
			} catch (SocketTimeoutException timeout) {
				return GlobalDefines.NET_DELAY_TIMEOUT;
			} catch (IOException e) {
				return GlobalDefines.NET_DELAY_TEST_FAILED;
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {}
				}
			}
		}

		//		private boolean tryRead(SocketChannel channel, Selector selector) throws IOException {
		//			request.rewind();
		//			channel.write(request);
		//			channel.register(selector, SelectionKey.OP_READ);
		//			if (1 != selector.select(GlobalDefines.NET_DELAY_TIMEOUT)) {
		//				return false;
		//			}
		//			Set<SelectionKey> keys = selector.selectedKeys();
		//			if (keys != null) {
		//				keys.clear();
		//			}
		//			return true;
		//		}

	}

	/**
	 * Worker的UDP实现
	 * <p>
	 * 注意：已知在Android 4.3及以下的几个手机上，发现NIO不可用（收不到UDP包）的情况，所以改用阻塞式的DatagramSocket
	 * </p>
	 * <p align="right">
	 * by YinHaiBao 2016.6.14
	 * </p>
	 */
	private static class WorkerUDP extends Worker {

		private DatagramSocket socket;
		private final byte[] buffer = new byte[16];
		private final DatagramPacket packet = new DatagramPacket(SEND_DATA, SEND_DATA.length);
		
		private static final byte[] SEND_DATA = new byte[] {
			0x01, 0x01, 0x12, 0x34, 0x56, 0x78, (byte)0xfe
		};

		public WorkerUDP() {
			super(makeHosts(), Type.UDP);
			try {
				socket = new DatagramSocket();
				socket.setSoTimeout(GlobalDefines.NET_DELAY_TIMEOUT);
			} catch (IOException e) {
				e.printStackTrace();
				cleanup();
			}
		}
		
		private static String[] makeHosts() {
			String host = "node-ddns.wsds.cn";
			if (TextUtils.isEmpty(host)) {
				host = "www.baidu.com";
			}
			String[] result = new String[1];
			result[0] = host;
			return result;
		}

		private void cleanup() {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		}

		private void buildRequestPack(SocketAddress addr) {
			packet.setData(SEND_DATA);
			packet.setSocketAddress(addr);
		}

		@Override
		protected int detect(SocketAddress addr) {
			if (socket == null) {
				return GlobalDefines.NET_DELAY_TEST_FAILED;
			}
			try {
				buildRequestPack(addr);
				long time_1 = now();
				socket.send(packet);
				packet.setData(buffer);
				socket.receive(packet);
				long time_2 = now();
				long delta = (time_2 - time_1);
				if (delta < 0) {
					delta = 0;
				} else if (delta > GlobalDefines.NET_DELAY_TIMEOUT) {
					delta = GlobalDefines.NET_DELAY_TIMEOUT;
				}
				return (int) delta;
			} catch (SocketTimeoutException timeout) {
				return GlobalDefines.NET_DELAY_TIMEOUT;
			} catch (IOException e) {
				e.printStackTrace();
				return GlobalDefines.NET_DELAY_TEST_FAILED;
			}
		}

		@Override
		protected int getPort() {
			return 222;
		}
	}

	/**
	 * 添加一个观察者
	 */
	public static void addObserver(Observer observer, Type type) {
		if (observer == null) {
			throw new IllegalArgumentException("Observer can not be null");
		}
		Worker worker;
		if (type == Type.TCP) {
			if (workerTCP == null) {
				workerTCP = new WorkerTCP();
			}
			worker = workerTCP;
		} else {
			if (workerUDP == null) {
				workerUDP = new WorkerUDP();
			}
			worker = workerUDP;
		}
		if (worker.getStatus() == AsyncTask.Status.PENDING) {
			worker.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
		}
		if (worker.observers.registerObserver(observer)) {
			observer.onNetDelayChange(worker.netDelay, worker.type);
		}
	}

	private static Worker findWorker(Type type) {
		return (type == Type.TCP) ? workerTCP : workerUDP;
	}

	/**
	 * 移除观察者
	 */
	public static void removeObserver(Observer observer, Type type) {
		Worker worker = findWorker(type);
		if (worker != null) {
			worker.observers.unregisterObserver(observer);
			if (worker.observers.isEmpty()) {
				worker = null;
			}
		}
	}

	/**
	 * 取当前TCP时延值
	 */
	public static int getDelayValue(Type type) {
		if (NetManager.getInstance().isDisconnected()) {
			return GlobalDefines.NET_DELAY_TEST_FAILED;
		}
		Worker worker = findWorker(type);
		if (worker != null) {
			return worker.netDelay;
		}
		return GlobalDefines.NET_DELAY_TEST_WAIT;
	}

	/**
	 * 测试是否正在运行当中？
	 */
	public static boolean isRunning(Type type) {
		return findWorker(type) != null;
	}

//	private static void clearSelectionKeys(Selector selector) {
//		Set<SelectionKey> keys = selector.selectedKeys();
//		if (keys != null) {
//			keys.clear();
//		}
//	}

}
