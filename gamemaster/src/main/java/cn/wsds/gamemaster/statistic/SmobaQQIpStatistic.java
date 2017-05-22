package cn.wsds.gamemaster.statistic;

import java.io.IOException;
import java.net.InetAddress;

import android.os.AsyncTask;

import com.subao.common.net.NetTypeDetector;

public class SmobaQQIpStatistic {

	//	private static final long TIMEOUT = 5000;

	public static class AddressList {
		public final InetAddress[] awx_smoba_qq_com;
		public final InetAddress[] app_smoba_qq_com;

		public AddressList(InetAddress[] awx_smoba_qq_com, InetAddress[] app_smoba_qq_com) {
			this.awx_smoba_qq_com = awx_smoba_qq_com;
			this.app_smoba_qq_com = app_smoba_qq_com;
		}
	}

	public interface Listener {
		public void onAddressTook(NetTypeDetector.NetType netState, AddressList addressList);
	}

	private static class Worker extends AsyncTask<Void, Void, AddressList> {
		private final NetTypeDetector.NetType netState;
		private final Listener listener;

		public Worker(NetTypeDetector.NetType netState, Listener listener) {
			this.netState = netState;
			this.listener = listener;
		}

		private static InetAddress[] getAddress(String host) {
			try {
				return InetAddress.getAllByName(host);
			} catch (IOException e) {} catch (RuntimeException e) {}
			return null;
		}

		@Override
		protected AddressList doInBackground(Void... params) {
			InetAddress[] awx_smoba_qq_com = getAddress("awx.smoba.qq.com");
			InetAddress[] app_smoba_qq_com = getAddress("app.smoba.qq.com");
			if (awx_smoba_qq_com != null || app_smoba_qq_com != null) {
				return new AddressList(awx_smoba_qq_com, app_smoba_qq_com);
			} else {
				return null;
			}
		}

		@Override
		protected void onPostExecute(AddressList result) {
			if (result != null && listener != null) {
				listener.onAddressTook(netState, result);
			}
		}
	}

	private SmobaQQIpStatistic() {}

	public static void start(NetTypeDetector.NetType netState, Listener listener) {
		new Worker(netState, listener).executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
	}

}
