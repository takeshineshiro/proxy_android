package cn.wsds.gamemaster.debugger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.subao.common.Misc;
import com.subao.common.data.Address;
import com.subao.common.data.SubaoIdManager;
import com.subao.common.msg.MessageUserId;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.CalendarUtils;
import com.subao.common.utils.InfoUtils;
import com.subao.common.utils.PhoneNumber;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.DeviceInfo;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.CPUTemperatureDetector;
import cn.wsds.gamemaster.ui.UIUtils;

public class FragmentDebug00 extends FragmentDebug {

	private String dnsResult;

	private MyAdapter listAdapter;

	private static class NameValue {
		public final String name;
		private final String value;

		public NameValue(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}
	}

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_00;
	}

	@Override
	protected void initView(View root) {
		List<NameValue> list = getInfoList(root.getContext());
		this.listAdapter = new MyAdapter(root.getContext(), list);
		ListView listView = (ListView) root.findViewById(R.id.listview);
		listView.setAdapter(listAdapter);
		new DNSExecutor(this).executeOnExecutor(ThreadPool.getExecutor());
	}
// FIXME: 17-3-29 hujd
//	private static void addAccelDataListInfo(List<NameValue> infoList, String title, AccelDataListManager<?> manager) {
//		String cacheTag = manager.getCacheTag();
//		String time = transCacheTagToTime(cacheTag);
//		infoList.add(new NameValue(title, String.format("%s, %d\n%s", cacheTag, manager.getCount(), time)));
//	}

	private static String transCacheTagToTime(String cacheTag) {
		long timestamp = -1;
		if (cacheTag != null) {
			for (int i = 0, len = cacheTag.length(); i < len; ++i) {
				char ch = cacheTag.charAt(i);
				if (ch < '0' || ch > '9') {
					continue;
				}
				timestamp = (ch - '0');
				for (int j = i + i; j < len; ++j) {
					char c = cacheTag.charAt(j);
					if (c < '0' || c > '9') {
						break;
					}
					timestamp = timestamp * 10 + (c - '0');
				}
				break;
			}
		}
		if (timestamp > 0) {
			Calendar calendar = Calendar.getInstance(CalendarUtils.TIME_ZONE_OF_UTC);
			calendar.setTimeInMillis(timestamp);
			return CalendarUtils.calendarToString(calendar, CalendarUtils.FORMAT_DATE | CalendarUtils.FORMAT_TIME | CalendarUtils.FORMAT_ZONE);
		} else {
			return "(时间未知)";
		}
	}

	private List<NameValue> getInfoList(Context context) {
		// FIXME: 17-3-29 hujd
//		AccelGameList aglManager = AccelGameList.getInstance();
//		AccelNodeListManagerImpl anlManager = AccelNodeListManagerImpl.getInstance();
		List<NameValue> infoList = new ArrayList<NameValue>(64);
		// 各种ID
		infoList.add(new NameValue("SubaoId", SubaoIdManager.getInstance().getSubaoId()));
		MessageUserId mui = MessageUserId.build();
		infoList.add(new NameValue("UserId", mui.userId));
		infoList.add(new NameValue("ServiceId", mui.serviceId));
		infoList.add(new NameValue("UserStatus", Integer.toString(mui.userStatus)));
		infoList.add(new NameValue("设备ID", Statistic.getDeviceId()));
		// 版本信息
		infoList.add(new NameValue("Android版本", Integer.toString(android.os.Build.VERSION.SDK_INT)));
		infoList.add(new NameValue("APP版本", String.format("%s (%d)", InfoUtils.getVersionName(context), InfoUtils.getVersionCode(context))));
		infoList.add(new NameValue("发行渠道", DeviceInfo.getUmengChannel(context)));
		infoList.add(new NameValue("git", getString(R.string.git)));
		infoList.add(new NameValue("cproxy commit id", getString(R.string.cproxy_commit_id)));
		infoList.add(new NameValue("qpp commit id", getString(R.string.qpp_commit_id)));
		// 基本数据
		// FIXME: 17-3-29 hujd
//		addAccelDataListInfo(infoList, "节点列表", anlManager);
//		addAccelDataListInfo(infoList, "游戏列表", aglManager);
		infoList.add(new NameValue("WiFi加速", getParallelConfigInfo()));
		// FIXME: 17-3-29 hujd
//		infoList.add(new NameValue("WiFi加速",
//			String.format("support=%b, display=%b, data=%b",
//				NetworkWatcher.isSupported(),
//				DoubelAccelStartStrategy.isDisplayDoubelAccel(),
//				DoubelAccelStartStrategy.getParallelConfig() != null)));
		//
		// 智能域号
		infoList.add(new NameValue("Smart DNS", null) {
			@Override
			public String getValue() {
				return dnsResult;
			}
		});
		// FIXME: 17-3-29 hujd
//		ISP isp = AccelNodeListManagerImpl.getInstance().getISP();
//		List<AccelNode> nodes = AccelNodeListManager.getNodeListWithISP(AccelNodeListManagerImpl.getInstance(), isp, true, true);
//		int countBGP = 0, countForeign = 0;
//		for (AccelNode node : nodes) {
//			if ((AccelNode.ISP.FOREIGN.bitMask & node.ispBits) != 0) {
//				++countForeign;
//			}
//			if ((AccelNode.ISP.BGP.bitMask & node.ispBits) != 0) {
//				++countBGP;
//			}
//		}
//		infoList.add(new NameValue("ISP", String.format("%s, %d (bgp=%d foreign=%d)",
//			(isp == null ? "(null)" : isp.key), nodes.size(),
//			countBGP, countForeign)));
		//
		String imsi = InfoUtils.getIMSI(context);
		infoList.add(new NameValue("IMSI", imsi));
		infoList.add(new NameValue("中国电信", Boolean.toString(PhoneNumber.isIMSIChinaTelecom(imsi))));
		infoList.add(new NameValue("江苏电信4G", Boolean.toString(PhoneNumber.isIMSIChinaTelecom4G_JiangSu(imsi))));
		infoList.add(new NameValue("南京电信4G", Boolean.toString(PhoneNumber.isIMSIChinaTelecom4G_NanJing(imsi))));
		infoList.add(new NameValue("总加速时长", String.format("%d秒", GameManager.getInstance().getAccelTimeSecondsAmount())));
		//
		infoList.add(new NameValue("CPU温度", String.format("%.1f °C", new CPUTemperatureDetector().get())));
		infoList.add(new NameValue("Info", DeviceInfo.get(context)));
		//
		return infoList;
	}
	
	private static String getParallelConfigInfo() {
		// FIXME: 17-3-29 hujd
//		ParallelConfig config = DoubelAccelStartStrategy.getParallelConfig();
//		if (config != null) {
//			ParallelConfig.Data data = config.getData();
//			if (data != null) {
//				return String.format("%s, model=%d, cpu=%d\n%s", data.getCacheTag(), data.getModelCount(), data.getCpuCount(), transCacheTagToTime(data.getCacheTag()));
//			}
//		}
		return "";
	}

	private static class DNSExecutor extends AsyncTask<Void, Void, String> {

		private final WeakReference<FragmentDebug00> owner;

		public DNSExecutor(FragmentDebug00 owner) {
			this.owner = new WeakReference<FragmentDebug00>(owner);
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				InetAddress ia = InetAddress.getByName(Address.HostName.ISP_MAP);
				if (ia != null) {
					byte[] ip = ia.getAddress();
					if (ip != null && ip.length >= 4) {
						return String.format("%d.%d.%d.%d", ip[0] & 0xff, ip[1] & 0xff, ip[2] & 0xff, ip[3] & 0xff);
					}
				}
			} catch (IOException e) {}
			return "(fail)";
		}

		@Override
		protected void onPostExecute(String result) {
			FragmentDebug00 owner = this.owner.get();
			if (owner != null) {
				if (!Misc.isEquals(owner.dnsResult, result)) {
					owner.dnsResult = result;
					owner.listAdapter.notifyDataSetChanged();
				}
			}
		}

	}

	private static class MyAdapter extends BaseAdapter {

		private static class Holder {
			public TextView tvName;
			public TextView tvValue;

			public Holder(TextView tvName, TextView tvValue) {
				this.tvName = tvName;
				this.tvValue = tvValue;
			}
		}

		private final List<NameValue> list;
		private final LayoutInflater layoutInflater;

		public MyAdapter(Context context, List<NameValue> list) {
			this.list = list;
			this.layoutInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder;
			View view = convertView;
			if (view == null) {
				view = layoutInflater.inflate(R.layout.fragment_debug_00_listitem, parent, false);
				TextView tvName = (TextView) view.findViewById(R.id.text_name);
				TextView tvValue = (TextView) view.findViewById(R.id.text_value);
				holder = new Holder(tvName, tvValue);
				view.setTag(holder);
				//
				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						Holder holder = (Holder) v.getTag();
						String text = holder.tvValue.getText().toString();
						ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
						clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
						UIUtils.showToast(String.format("%s\n已复制到剪贴板", text));
						return true;
					}
				});
			} else {
				holder = (Holder) view.getTag();
			}
			//
			NameValue nv = list.get(position);
			holder.tvName.setText(nv.name);
			holder.tvValue.setText(nv.getValue());
			//
			view.setBackgroundColor((position & 1) == 0 ? 0 : 0xff333333);
			return view;
		}
	}
}
