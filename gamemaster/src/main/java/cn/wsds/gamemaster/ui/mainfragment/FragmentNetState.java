package cn.wsds.gamemaster.ui.mainfragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.NetDelayDetector;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.net.NetSignalStrengthManager;
import cn.wsds.gamemaster.net.NetSignalStrengthManager.OnSignalLevelChangedListener;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityMain;
import cn.wsds.gamemaster.ui.StabilityDrawer;
import cn.wsds.gamemaster.ui.StabilityDrawer.StatbilityStateProperty;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.doublelink.ActivityDoubleLink;

import com.subao.common.data.ParallelConfigDownloader;
import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

/**
 * 网络状态模块
 */
@SuppressLint("DefaultLocale")
public class FragmentNetState extends Fragment {

	/**
	 * 信号强度级别管理
	 */
	private final NetSignalStrengthManager signalStrengthManager = new NetSignalStrengthManager();
	private StabilityDrawer stabilityDrawer;

	/**
	 * 界面控件组
	 */
	private Item itemIcon, itemStability;

	private DelayUI delayUI;

	private NetTypeDetector.NetType currentNetType = NetTypeDetector.NetType.UNKNOWN;

	private boolean isDisplayDoubleAccel;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState) {
		Activity activity = getActivity();
		View rootView = inflater.inflate(R.layout.fragment_net_state, new LinearLayout(activity));
		itemIcon = new Item(rootView.findViewById(R.id.net_icon), R.string.our_net);
		delayUI = new DelayUI(this.getResources(), new Item(rootView.findViewById(R.id.net_delay), R.string.delay).value);
		currentNetType = NetManager.getInstance().getCurrentNetworkType();
		initLastItem(rootView);
		return rootView;
	}

	private void initLastItem(View rootView) {
		isDisplayDoubleAccel = ParallelConfigDownloader.isPhoneParallelSupported();
		if (!isDisplayDoubleAccel) {
			initStabilityItem(rootView);
		} else {
			initDoubleLinkItem(rootView);
		}
	}

	private Item itemDouble;

	private void initDoubleLinkItem(final View rootView) {

		View view = rootView.findViewById(R.id.net_stability);
		itemDouble = new Item(view, R.string.double_accel);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				UIUtils.turnActivity(rootView.getContext(), ActivityDoubleLink.class);
				Statistic.addEvent(getActivity(), Statistic.Event.INTERACTIVE_DUAL_NETWORK_HOMEPAGE_CLICK);
			}
		});

		setDoubleIcon();
		itemDouble.icon.setVisibility(View.VISIBLE);
		itemDouble.value.setVisibility(View.INVISIBLE);
	}

	private void setDoubleIcon() {
		if (itemDouble == null) {
			return;
		}

		itemDouble.setRemindVisible(ConfigManager.getInstance().isDoubleAccelFirst());
		if (ConfigManager.getInstance().isEnableDoubleAccel() ) {
			itemDouble.icon.setImageResource(R.drawable.home_page_network_state_your_network_par_open);
		} else {
			itemDouble.icon.setImageResource(R.drawable.home_page_network_state_your_network_par_close);
		}
	}

	private void initStabilityItem(View rootView) {
		itemStability = new Item(rootView.findViewById(R.id.net_stability), R.string.net_stability);
		createStabilityDrawer(itemStability.value);
	}

	private void createStabilityDrawer(TextView textStability) {
		StatbilityStateProperty goodProperty = new StatbilityStateProperty(getResources().getColor(R.color.color_game_8), "好");
		StatbilityStateProperty normalProperty = new StatbilityStateProperty(getResources().getColor(R.color.color_game_17), "中");
		StatbilityStateProperty badProperty = new StatbilityStateProperty(getResources().getColor(R.color.color_game_16), "差");
		StatbilityStateProperty offProperty = new StatbilityStateProperty(getResources().getColor(R.color.color_game_8), "--");
		stabilityDrawer = new StabilityDrawer(textStability, goodProperty, normalProperty, badProperty, offProperty);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (!isDisplayDoubleAccel) {
			signalStrengthManager.setSignalLevelChangedListener(new OnSignalLevelChangedListener() {

				@Override
				public void onSignalLevelChanged(int signaLevel) {
					stabilityDrawer.setStabilityDesc(signaLevel);
				}
			});
			signalStrengthManager.start();
		}
		//
		delayUI.refresh(NetDelayDetector.getDelayValue(ActivityMain.NET_DELAY_DETECT_TYPE));
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (!isDisplayDoubleAccel) {
			signalStrengthManager.cleanUp();
		}
	}

	/**
	 * 设置当加速关闭时界面状态
	 */
	public void setStateAccelOff() {
		Activity activity = getActivity();
		if (activity == null)
			return;
		NetTypeDetector.NetType currentNetworkType = NetManager.getInstance().getCurrentNetworkType();
		setNetIcon(currentNetworkType);

		delayUI.showDelayWhenAccelOff();

		if (!isDisplayDoubleAccel) {
			stabilityDrawer.setStateOff();
		}
	}

	/**
	 * 设置当加速开启时加速状态
	 */
	public void setStateAccelOn() {
		Activity activity = getActivity();
		if (activity == null)
			return;
		NetTypeDetector.NetType currentNetworkType = NetManager.getInstance().getCurrentNetworkType();
		setNetIcon(currentNetworkType);

		if (!isDisplayDoubleAccel) {
			int signaLevel = getNetStatility(currentNetworkType);
			stabilityDrawer.setStabilityDesc(signaLevel);
		}
	}

	/**
	 * 界面控件组
	 */
	private static final class Item {

		private final View root;

		/**
		 * 描述文本 具体值控件
		 */
		public final TextView name, value;
		/**
		 * 图标 和值控件一样
		 */
		public final ImageView icon;

		private View remind;

		public Item(View groupView, int desc) {
			this.root = groupView;
			name = (TextView) groupView.findViewById(R.id.net_state_desc);
			name.setText(desc);
			value = (TextView) groupView.findViewById(R.id.net_state_value);
			icon = (ImageView) groupView.findViewById(R.id.image_icon);
		}

		public void setRemindVisible(boolean visible) {
			if (visible) {
				if (remind == null) {
					ViewStub stub = (ViewStub) root.findViewById(R.id.remind_animation);
					if (stub == null) {
						return;
					}
					remind = stub.inflate();
				}
			}
			if (remind != null) {
				remind.setVisibility(visible ? View.VISIBLE : View.GONE);
				ImageView img = (ImageView) remind.findViewById(R.id.ani);
				AnimationDrawable ad = (AnimationDrawable) img.getDrawable();
				if (visible) {
					ad.start();
				} else {
					ad.stop();
				}
			}
		}

	}

	/**
	 * 设置“我的网络”图标
	 */
	private void setNetIcon(NetTypeDetector.NetType netType) {
		switch (netType) {
		case MOBILE_2G:
			setMobileNetIcon("2G");
			break;
		case MOBILE_3G:
			setMobileNetIcon("3G");
			break;
		case MOBILE_4G:
			setMobileNetIcon("4G");
			break;
		case WIFI:
			setExceptMobileNetIcon(R.drawable.home_page_network_state_your_network_wifi);
			break;
		case DISCONNECT:
			setExceptMobileNetIcon(R.drawable.home_page_network_state_your_network_abnormal);
			break;
		default:
			setMobileNetIcon("--");
			break;
		}
	}

	/**
	 * 设置“我的网络”非数据网络（异常、WiFi）时图标
	 * 
	 * @param resource
	 *            图片ID
	 */
	private void setExceptMobileNetIcon(int resource) {
		itemIcon.icon.setImageResource(resource);
		itemIcon.icon.setVisibility(View.VISIBLE);
		itemIcon.value.setVisibility(View.INVISIBLE);
	}

	/**
	 * 设置“我的网络”数据网络时图标
	 * 
	 * @param netName
	 *            (2G\3G\4G)
	 */
	private void setMobileNetIcon(String netName) {
		itemIcon.icon.setVisibility(View.GONE);
		itemIcon.value.setVisibility(View.VISIBLE);
		itemIcon.value.setText(netName);
	}

	/**
	 * 当网络发生变化的时候
	 * 
	 * @param netType
	 *            网络类型
	 */
	public void onNetChange(NetTypeDetector.NetType netType) {
		if (currentNetType == netType) {
			return;
		}
		this.currentNetType = netType;
		setNetIcon(netType);
		if (!isDisplayDoubleAccel) {
			stabilityDrawer.setStabilityDesc(getNetStatility(netType));
		}
	}

	private int getNetStatility(NetTypeDetector.NetType netType) {
		switch (netType) {
		case WIFI:
			return signalStrengthManager.getWifiSignalLevel();
		case DISCONNECT:
			return -1;
		default:
			return signalStrengthManager.getMobileSignalLevel();
		}
	}

	public View getDleayVlaue() {
		return delayUI.textView;
	}

	public View getStabilityValue() {
		return itemStability.value;
	}

	private static class DelayUI {

		private final int colorNormal, colorBad, colorWorse;
		private final TextView textView;

		public DelayUI(Resources res, TextView textView) {
			this.textView = textView;
			colorNormal = res.getColor(R.color.color_game_8);
			colorBad = res.getColor(R.color.color_game_17);
			colorWorse = res.getColor(R.color.color_game_16);
		}

		public void refresh(long delay) {
			if (AccelOpenManager.isStarted()) {
				showDelayWhenAccelOn(delay);
			} else {
				showDelayWhenAccelOff();
			}
		}

		/**
		 * 设置加速开启的时候的延时值
		 */
		private void showDelayWhenAccelOn(long delayMilliseconds) {
			if (delayMilliseconds < 0) {
				showDelayWhenAccelOff();
				if (GlobalDefines.NET_DELAY_TEST_FAILED == delayMilliseconds || NetManager.getInstance().isDisconnected()) {
					textView.setTextColor(colorWorse);
				}
				return;
			}
			if (delayMilliseconds >= GlobalDefines.NET_DELAY_TIMEOUT) {
				textView.setText("> 2s");
				textView.setTextColor(colorWorse);
				return;
			}
			String value, unit;
			if (delayMilliseconds > 999) {
				value = String.format("%.1f", delayMilliseconds * 0.001f);
				unit = "s";
			} else {
				value = Long.toString(delayMilliseconds);
				unit = "ms";
			}
			//
			SpannableStringBuilder builder = new SpannableStringBuilder();
			builder.append(value);
			builder.append(unit);
			builder.setSpan(new AbsoluteSizeSpan(12, true), value.length(), builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(builder);
			//
			if (delayMilliseconds < 150) {
				textView.setTextColor(colorNormal);
			} else if (delayMilliseconds <= 300) {
				textView.setTextColor(colorBad);
			} else {
				textView.setTextColor(colorWorse);
			}
		}

		private void showDelayWhenAccelOff() {
			textView.setText("--");
			textView.setTextColor(colorNormal);
		}
	}

	public void onGeneralNetDelayChange(long delayMilliseconds) {
		delayUI.refresh(delayMilliseconds);
	}

	public void refreshDoubleIcon() {
		if (isDisplayDoubleAccel) {
			setDoubleIcon();
		}
	}
}
