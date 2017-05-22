package cn.wsds.gamemaster.ui.floatwindow;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager;
import cn.wsds.gamemaster.netcheck.NetworkCheckManager.Checker.Result;

public class DelayTextSpannable {

	private final int color;
	
	private final OnTakeLocalDelayTextSpannableListener onTakeLocalDelayTextSpannableListener;
	public interface OnTakeLocalDelayTextSpannableListener {
		public void onTakeLocalDelayTextSpannable(Spannable localSpannable, Spannable remoteSpannable);
	}
	
	public DelayTextSpannable(OnTakeLocalDelayTextSpannableListener onTakeLocalDelayTextSpannableListener) {
		this.onTakeLocalDelayTextSpannableListener = onTakeLocalDelayTextSpannableListener;
		Resources resources = AppMain.getContext().getResources();
		this.color = resources.getColor(R.color.color_game_16);
	}
	
	public void toTakeLocalTextSpannable(Context context ){
		NetworkCheckManager.start(context, new NetworkCheckManager.Observer() {
			
			@Override
			public void onNetworkCheckResult(Result result) {
				Spannable localSpannable = getLocalTextSpannableByNetEvent(result);
				Spannable remoteSpannable = getRemoteDelayTextSpannable(result);
				if(onTakeLocalDelayTextSpannableListener!=null){
					onTakeLocalDelayTextSpannableListener.onTakeLocalDelayTextSpannable(localSpannable,remoteSpannable);
				}
			}
		});
	}
	
	public static Spannable getRemoteDelayTextSpannable(Result netExceptionDesc) {
		Resources resources = AppMain.getContext().getResources();
		int color = resources.getColor(R.color.color_game_16);
		if(Result.WAP_POINT == netExceptionDesc){
			return getSpecialInBehind("迅游手游加速的网络接入点类型为NET，", "建议使用NET接入点进行游戏。", color);
		}else{
			return getSpecialInBefore("“我的网络”异常", "，“加速网络”无法正常工作。", color);
		}
	}
	
	private Spannable getLocalTextSpannableByNetEvent(Result netExceptionDesc) {
		switch (netExceptionDesc) {
		case WIFI_UNAVAILABLE:
			return getSpecialInMid("“我的网络”", "连接超时", "，建议重连WiFi/重启无线路由器/更换WiFi/使用数据连接。", color);
		case NETWORK_DISCONNECT:
			return getSpecialInBefore("“我的网络”已断开", "，请检查“我的网络”。",  color);
		case WAP_POINT:
			return getSpecialInBefore("WAP接入点无法使用加速", "，建议使用NET接入点进行游戏。",  color);
		case MOBILE_UNAVAILABLE:
			return getSpecialInMid("“我的网络”", "连接超时", "，建议尝试重启数据连接（开关飞行模式）/换到信号更优的地方。", color);
		case AIRPLANE_MODE:
			return getSpecialInBefore("飞行模式已开启", "，网络连接已断开，建议关闭飞行模式再游戏。",  color);
		case WIFI_MOBILE_CLOSED:
			return getSpecialInBefore("WiFi与移动数据均为关闭状态", "，请先打开网络连接。",  color);
		case WIFI_FAIL_RETRIEVE_ADDRESS:
			return getSpecialInMid("当前WiFi网络", "没有获取到网络地址", "，建议尝试重连/更换WiFi/使用数据连接。", color);
		case WIFI_SHOULD_AUTHORIZE:
			return getSpecialInMid("当前", "WiFi网络需要验证", "，建议打开浏览器进行验证/连接其他不需要验证的WiFi。", color);
		case IP_ADDR_ASSIGN_PENDING:
			return getSpecialInMid("当前WiFi不可用，", "WiFi正在等待获取地址", "，请稍后。", color);
		case NETWORK_AUTHORIZATION_FORBIDDED:
			return getSpecialInBefore("网络权限被禁止", "，请确保迅游手游能够获取网络访问权限。",  color);
		default:
			return getSpecialInBefore("未知网络异常", "，建议尝试手动开关WiFi/飞行模式。",  color);
		}
	}

	public static Spannable getSpecialInBefore(String special,String specialBehind,int specialColor){
		return getSpecialInMid("", special, specialBehind, specialColor);
	}
	
	public static Spannable getSpecialInBehind(String specialBefore,String special,int specialColor){
		return getSpecialInMid(specialBefore, special, "", specialColor);
	}
	
	public static SpannableStringBuilder getSpecialInMid(String specialBefore,String special,String specialBehind,int specialColor){
		SpannableStringBuilder spannable = new SpannableStringBuilder();
		spannable.append(specialBefore);
		spannable.append(special);
		spannable.append(specialBehind);
		int start = specialBefore.length();
		spannable.setSpan(new ForegroundColorSpan(specialColor), start, start + special.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		return spannable;
	}
	
}
