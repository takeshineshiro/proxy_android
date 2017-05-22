package cn.wsds.gamemaster.ui.floatwindow;

import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.subao.common.net.NetTypeDetector;
import com.subao.net.NetManager;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.app.GameManager.SecondSegmentNetDelay;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.net.NetSignalStrengthManager;
import cn.wsds.gamemaster.net.NetSignalStrengthManager.OnSignalLevelChangedListener;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.StabilityDrawer;
import cn.wsds.gamemaster.ui.StabilityDrawer.StatbilityStateProperty;
import cn.wsds.gamemaster.ui.floatwindow.skin.SkinResourceBoxInGame;
import cn.wsds.gamemaster.ui.floatwindow.twosegment.FirstSegmentNetDelayDrawer;
import cn.wsds.gamemaster.ui.floatwindow.twosegment.NetDelayDrawer;
import cn.wsds.gamemaster.ui.floatwindow.twosegment.NetDelayDrawer.OnDelayErrorListenter;
import cn.wsds.gamemaster.ui.floatwindow.twosegment.SecondSegmentNetDelayDrawer;

/**
 * 两段延时
 */
public class TwoSectionDelayDrawer {

	private final StabilityModel stabilityModel;
	private final TextView localDelayPormpt;
	private final TextView localNetText;
	private NetTypeDetector.NetType currentNetType;
	private final ImageView ourNetIcon;
	private final PormptFliper pormptFliper;
	private View.OnClickListener viewOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.server_icon:
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_ICON_CLICK, "服务器");
				if (pormptFliper.currentShowServerPormpt()) {
					pormptFliper.showModelStability();
				} else {
					pormptFliper.showServerPormpt();
				}
				break;
			case R.id.our_net_icon:
				if (pormptFliper.currentShowLocalPormpt()) {
					Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_ICON_CLICK, "我的网络");
					pormptFliper.showModelStability();
				} else {
					Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_ICON_CLICK, "WiFi加速");
					setWifiAccelView(WifiAccelState.getInstance().isAccelState());
					pormptFliper.showLocalPormpt();

				}
				break;
			case R.id.gamemaster_icon:
				Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_ICON_CLICK, "加速节点");
				if (pormptFliper.currentShowRemotePormpt()) {
					pormptFliper.showModelStability();
				} else {
					pormptFliper.showRemotePormpt();
				}
				break;
			}
		}
	};

	private final GameInfo gameInfo;
	//private final TextView textBreakCount;

	private ImageView serverIcon;
	private final NetDelayDrawer firstSegmentNetDelayDrawer;
	private final NetDelayDrawer secondSegmentNetDelayDrawer;

	public TwoSectionDelayDrawer(View rootGroup, GameInfo gameInfo, SkinResourceBoxInGame currenSkinResource) {
		this.gameInfo = gameInfo;

		//this.textBreakCount = (TextView) rootGroup.findViewById(R.id.text_break_count);
		this.stabilityModel = new StabilityModel((TextView) rootGroup.findViewById(R.id.text_stability));
		rootGroup.findViewById(R.id.image_foreign_logo).setVisibility(
				gameInfo.isForeignGame() ? View.VISIBLE : View.GONE);

		this.ourNetIcon = (ImageView) rootGroup.findViewById(R.id.our_net_icon);
		this.ourNetIcon.setOnClickListener(viewOnClickListener);
		this.serverIcon = (ImageView) rootGroup.findViewById(R.id.server_icon);
		serverIcon.setOnClickListener(viewOnClickListener);
		ImageView gamemasterIcon = (ImageView) rootGroup.findViewById(R.id.gamemaster_icon);
		gamemasterIcon.setOnClickListener(viewOnClickListener);

		int netNameTextColorId = currenSkinResource.getNetNameTextColorId();
		this.pormptFliper = createPromtpFliper(rootGroup, netNameTextColorId);
		
		TextView serverMess = (TextView) rootGroup.findViewById(R.id.server_mess);
		Resources resources = rootGroup.getResources();
		serverMess.setText(DelayTextSpannable.getSpecialInMid("延迟是手机网络到达游戏服务器所用的时间，单位是毫秒，", "数值越小越好", "。",
			resources.getColor(R.color.color_game_11)));
		
		TextView localDelay = (TextView) rootGroup.findViewById(R.id.local_delay);
		localDelayPormpt = (TextView) rootGroup.findViewById(R.id.ournet_mess);
		localNetText = (TextView) rootGroup.findViewById(R.id.local_net);
		this.firstSegmentNetDelayDrawer = new FirstSegmentNetDelayDrawer(localDelay, (ImageView)rootGroup.findViewById(R.id.local_delay_progress), localDelayPormpt,new OnDelayErrorListenter() {
			
			@Override
			public void onDelayError() {
				pormptFliper.showLocalPormpt();
			}
		});
		TextView remoteDelay = (TextView) rootGroup.findViewById(R.id.remote_delay);
		TextView remoteDelayPormpt = (TextView) rootGroup.findViewById(R.id.gamemaster_mess);
		secondSegmentNetDelayDrawer = new SecondSegmentNetDelayDrawer(remoteDelay, (ImageView)rootGroup.findViewById(R.id.remote_delay_progress), remoteDelayPormpt, new OnDelayErrorListenter() {
			
			@Override
			public void onDelayError() {
				pormptFliper.showRemotePormpt();
			}
		},gameInfo.isForeignGame());
	}

	private PormptFliper createPromtpFliper(View rootGroup,int netNameTextColorId) {
		View modelStability = rootGroup.findViewById(R.id.model_stability);
		View localPrompt = rootGroup.findViewById(R.id.local_pormpt);
		View gamemasterPrompt = rootGroup.findViewById(R.id.gamemaster_pormpt);
		View serverPrompt = rootGroup.findViewById(R.id.server_pormpt);
		TextView localText = (TextView) rootGroup.findViewById(R.id.local_net);
		TextView gamemasterText = (TextView) rootGroup.findViewById(R.id.gamemaster_cloud);
		TextView serverText = (TextView) rootGroup.findViewById(R.id.gamemaster_server);
		return new PormptFliper(modelStability, localPrompt, gamemasterPrompt, serverPrompt, localText,
			gamemasterText, serverText, netNameTextColorId);
	}

	private EventObserver eventObserver = new EventObserver() {
		/**
		 * 当加速状态变化的时候
		 * 
		 * @param on
		 */
		@Override
		public void onAccelSwitchChanged(boolean on) {
			if (on) {
				stabilityModel.onAccelOn();
			} else {
				stabilityModel.onAccelOff();
			}
		}

		/**
		 * 当网络状态发生变化时
		 */
		@Override
		public void onNetChange(NetTypeDetector.NetType netType) {
			currentNetType = netType;
			stabilityModel.onNetChange(netType);
			setNetIcon(netType);
		}

		/**
		 * 当本地延时值改变的时候
		 * 
		 * @param delayMilliseconds
		 */
		@Override
		public void onFirstSegmentNetDelayChange(int delayMilliseconds) {
			firstSegmentNetDelayDrawer.onDelayChange(delayMilliseconds);
		}

		@Override
		public void onSecondSegmentNetDelayChange(int uid, SecondSegmentNetDelay secondSegmentNetDelay) {
			if (uid == gameInfo.getUid()) {
				int delayMilliseconds = secondSegmentNetDelay.getDelayValue();
				secondSegmentNetDelayDrawer.onDelayChange(delayMilliseconds);
				setDirecTransIcon(secondSegmentNetDelay.isDirectTrans());
			}
		}

		@Override
		public void onGetWifiAccelState(boolean isEnable) {
			WifiAccelState.getInstance().setAccelState(isEnable);
			setWifiAccelView(isEnable);
		}
	};

	private void setWifiAccelView(boolean isEnable) {
		if(isEnable) {
			localDelayPormpt.setText(R.string.float_window_text_desc);
			localNetText.setText(R.string.double_accel);
			setWifiAccelIcon();
		} else {
			localNetText.setText(R.string.our_net);
			setNetIcon(currentNetType);
		}
	}
	private void setWifiAccelIcon() {
		ourNetIcon.setImageResource(R.drawable.home_page_network_state_your_network_par_close);
	}

	private void setNetIcon(NetTypeDetector.NetType netType) {
		int imageSource;
		switch (netType) {
		case MOBILE_2G:
			imageSource = R.drawable.suspension_network_state_your_network_2g;
			break;
		case MOBILE_3G:
			imageSource = R.drawable.suspension_network_state_your_network_3g;
			break;
		case MOBILE_4G:
			imageSource = R.drawable.suspension_network_state_your_network_4g;
			break;
		case WIFI:
			imageSource = R.drawable.suspension_network_state_your_network_wifi;
			break;
		default:
			imageSource = R.drawable.home_page_network_state_your_network_abnormal;
			break;
		}
		ourNetIcon.setImageResource(imageSource);
	}

	public void cleanUp() {
		stabilityModel.cleanUp();
		TriggerManager.getInstance().deleteObserver(eventObserver);
	}

	public void start() {
		// 初始化值
		this.currentNetType = NetManager.getInstance().getCurrentNetworkType();
		setWifiAccelView( WifiAccelState.getInstance().isAccelState());
		pormptFliper.showModelStability();
		stabilityModel.onResume();

		//setReconnectCount();

		TriggerManager.getInstance().addObserver(eventObserver);
		
		int uid = gameInfo.getUid();
		SecondSegmentNetDelay secondSegmentNetDelay = GameManager.getInstance().getSecondSegmentNetDelay(uid);
		eventObserver.onSecondSegmentNetDelayChange(uid, secondSegmentNetDelay);
		eventObserver.onFirstSegmentNetDelayChange(GameManager.getInstance().getFirstSegmentNetDelay());
	}

	private void setDirecTransIcon(boolean directTrans) {
		int resId = directTrans ? R.drawable.floating_window_game_through : R.drawable.floatwindow_network_state_external_network;
		serverIcon.setImageResource(resId);
	}

	/*private void setReconnectCount() {
		String reconnectCountDesc = String.valueOf(gameInfo.getAccumulateReconnectCount());
		SpannableStringBuilder builder = new SpannableStringBuilder(reconnectCountDesc);
		String unit = "次";
		builder.append(unit);
		int start = reconnectCountDesc.length();
		int end = start + unit.length();
		builder.setSpan(new AbsoluteSizeSpan(12, true), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		textBreakCount.setText(builder);
	}*/

	private static final class PormptFliper {
		private final Pormpt stabilityPormpt;
		private final Pormpt localPormpt;
		private final Pormpt serverPormpt;
		private final Pormpt gamemasterPormpt;

		private PormptFliper(View modelStability, View local, View gamemaster, View server, TextView localText,
			TextView gamemasterText, TextView serverText, int unHighLightColorId) {
			stabilityPormpt = new Pormpt(modelStability, null, unHighLightColorId);
			this.localPormpt = new Pormpt(local, localText, unHighLightColorId);
			this.serverPormpt = new Pormpt(server, serverText, unHighLightColorId);
			this.gamemasterPormpt = new Pormpt(gamemaster, gamemasterText, unHighLightColorId);
		}

		public boolean currentShowRemotePormpt() {
			return gamemasterPormpt.currentShow();
		}

		public boolean currentShowLocalPormpt() {
			return localPormpt.currentShow();
		}

		public boolean currentShowServerPormpt() {
			return serverPormpt.currentShow();
		}

		public void showModelStability() {
			stabilityPormpt.show();
			localPormpt.dimiss();
			serverPormpt.dimiss();
			gamemasterPormpt.dimiss();
		}

		public void showLocalPormpt() {
			stabilityPormpt.dimiss();
			localPormpt.show();
			serverPormpt.dimiss();
			gamemasterPormpt.dimiss();
		}

		public void showRemotePormpt() {
			stabilityPormpt.dimiss();
			localPormpt.dimiss();
			serverPormpt.dimiss();
			gamemasterPormpt.show();
		}

		public void showServerPormpt() {
			stabilityPormpt.dimiss();
			localPormpt.dimiss();
			serverPormpt.show();
			gamemasterPormpt.dimiss();
		}

		private static final class Pormpt {
			private final View group;
			private final TextView textDesc;

			private final int colorHighLight;
			private final int colorUnHighLight;

			private Pormpt(View group, TextView textDesc, int unHighLightColorId) {
				this.group = group;
				this.textDesc = textDesc;
				Resources resources = group.getResources();
				colorUnHighLight = resources.getColor(unHighLightColorId);
				colorHighLight = resources.getColor(R.color.color_game_7);
			}

			public void show() {
				group.setVisibility(View.VISIBLE);
				if (textDesc != null) {
					textDesc.setTextColor(colorHighLight);
				}
			}

			public void dimiss() {
				group.setVisibility(View.GONE);
				if (textDesc != null) {
					textDesc.setTextColor(colorUnHighLight);
				}
			}

			public boolean currentShow() {
				return View.VISIBLE == group.getVisibility();
			}
		}
	}

	/**
	 * 稳定性模块
	 */
	private static final class StabilityModel {
		/** 信号强度级别管理 */
		private final NetSignalStrengthManager signalStrengthManager = new NetSignalStrengthManager();
		protected StabilityDrawer stabilityDrawer;

		private StabilityModel(TextView textStability) {
			createStabilityDrawer(textStability);
			signalStrengthManager.setSignalLevelChangedListener(new OnSignalLevelChangedListener() {

				@Override
				public void onSignalLevelChanged(int signaLevel) {
					stabilityDrawer.setStabilityDesc(signaLevel);
				}
			});
		}

		public void onResume() {
			signalStrengthManager.start();
		}

		public void cleanUp() {
			signalStrengthManager.cleanUp();
		}

		private void createStabilityDrawer(TextView textStability) {
			Resources resources = textStability.getResources();
			StatbilityStateProperty goodProperty = new StatbilityStateProperty(
				resources.getColor(R.color.color_game_46), "好");
			StatbilityStateProperty normalProperty = new StatbilityStateProperty(
				resources.getColor(R.color.color_game_17), "中");
			StatbilityStateProperty badProperty = new StatbilityStateProperty(
				resources.getColor(R.color.color_game_16), "差");
			StatbilityStateProperty offProperty = new StatbilityStateProperty(
				resources.getColor(R.color.color_game_46), "--");
			stabilityDrawer = new StabilityDrawer(textStability, goodProperty, normalProperty, badProperty, offProperty);
		}

		private void onAccelOn() {
			int signaLevel = getNetStatility(NetManager.getInstance().getCurrentNetworkType());
			stabilityDrawer.setStabilityDesc(signaLevel);
		}

		private void onAccelOff() {
			stabilityDrawer.setStateOff();
		}

		/**
		 * 当网络发生变化的时候
		 * 
		 * @param netType
		 *            网络类型
		 */
		public void onNetChange(NetTypeDetector.NetType netType) {
			stabilityDrawer.setStabilityDesc(getNetStatility(netType));
		}

		private int getNetStatility(NetTypeDetector.NetType netType) {
			switch (netType) {
			case WIFI:
				return signalStrengthManager.getWifiSignalLevel();
			case MOBILE_2G:
			case MOBILE_3G:
			case MOBILE_4G:
			case UNKNOWN:
				return signalStrengthManager.getMobileSignalLevel();
			default:
				return -1;
			}
		}
	}

	
}
