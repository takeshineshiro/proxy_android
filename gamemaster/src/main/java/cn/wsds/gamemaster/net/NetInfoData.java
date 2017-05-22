package cn.wsds.gamemaster.net;


/**
 * 获取网络信息工具类
 * @author Administrator
 *
 */
public class NetInfoData {
	
//	private static NetInfoData netInfoData;
//
//	private WifiManager wifiManager; 
//	private WifiInfo wifiInfo;
//	private TelephonyManager tManager;
	
//	private final static int[] channelsFrequency = new int[] {
//		0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
//        2452, 2457, 2462, 2467, 2472, 2484
//	};
	

//	private NetInfoData(Context context) {
//		context = context.getApplicationContext();
//		wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);  
//		wifiInfo = wifiManager.getConnectionInfo();
//		tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//	}
//	
//	public static NetInfoData getInstance(Context context) {
//		//return getInstance(false);
//		if(netInfoData == null){
//			netInfoData = new NetInfoData(context);
//		}
//		return netInfoData;
//	}
	
	
//	/**
//	 * 在wifi状态下要看最新的状态则先调用该方法
//	 */
//	public void wifiRefreshConnectionInfo(){
//		netInfoData.wifiInfo = netInfoData.wifiManager.getConnectionInfo();
//	}
	
	
//	/**
//	 * 获取wifi下的SSID
//	 * @param context
//	 * @return 如果获取不到，则返回null
//	 */
//	public String getWifiSSID(){
//		wifiInfo = wifiManager.getConnectionInfo();
//		String ssid = wifiInfo.getSSID();
//		if(ssid != null) {
//			// 如果有前后引号，去掉它
//			int len = ssid.length();
//			if (len >= 2) {
//				if (ssid.charAt(0) == '"' && ssid.charAt(len - 1) == '"') {
//					ssid = ssid.substring(1, len - 1);
//				}
//			}
//		}
//		return ssid;
//	}

//	/**
//	 * 获取wifi下的MAC地址
//	 * @param context
//	 * @return
//	 */
//	public String getWifiMAC(){
//		return wifiInfo.getMacAddress();
//	}
	
	
//	/**
//	 * 获取wifi下的IP
//	 * @param context
//	 * @return
//	 */
//	public String getWifiIP(){
//		int ipAddress = wifiInfo.getIpAddress();
//		if (ipAddress != 0) {
//			return IPv4.ipToString(IPv4.htonl(ipAddress));
//		} else
//			return null;
//	}
	
//	/**
//	 * 获取最大无线速率
//	 */
//	public String getWifiMaxSpeed(){
//		return wifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS;
//	}
	
//	public static class WiFiOtherInfo {
//		/** 加密方式 （为null表示未加密） */
//		public final String encryption;
//		/** 信道 */
//		public final int channel;
//		
//		public WiFiOtherInfo(String encryption, int channel) {
//			this.encryption = encryption;
//			this.channel = channel;
//		}
//	}
	
//	/**
//	 * 获取wifi的加密方式/信号强度/信道
//	 * @return  如果wifiInfors[0] == null 则说明没有进行加密
//	 */
//	public WiFiOtherInfo getWifiOtherInfor() {
//		String encryption = null;
//		int channel = -1;
//		//
//		List<ScanResult> scanResults = wifiManager.getScanResults();
//		if (scanResults != null) {
//			for (int i = scanResults.size() - 1; i >= 0; --i) {
//				ScanResult sresult = scanResults.get(i);
//				if (sresult.BSSID == null || !sresult.BSSID.equals(wifiInfo.getBSSID())) {
//					continue;
//				}
//				String rs = sresult.capabilities;
//				if (rs != null) {
//					rs = rs.toLowerCase(Locale.US);
//					if (rs.contains("wpa")) {
//						encryption = "WPA ";
//					} else if (rs.contains("wep")) {
//						encryption = "WEP ";
//					}
//				}
//				//
//				channel = channelsFrequency.length - 1;
//				while (channel >= 0) {
//					if (channelsFrequency[channel] == sresult.frequency) {
//						break;
//					} else {
//						--channel;
//					}
//				}
//				//
//				break;
//			}
//		}
//		return new WiFiOtherInfo(encryption, channel);
//	}
	
	
//	/**
//	 * 获取信号强度
//	 * @return
//	 */
//	public int getWifiLevel(){
//		return wifiInfo.getRssi();
//	}
//	
	//2G、3G、4G网络
	
//	/**
//	 * 获取网络运营商
//	 * @return
//	 */
//	public String getNetWorkOperatorName(){
//		String tm = tManager.getNetworkOperatorName();
//		if(tm != null && tm.length() >= 5 && tm.substring(0, 3).equals("460")){
//			String tp = tm.substring(3, 5);
//			if(tp.equals("00") || tp.equals("02") || tp.equals("07")){
//				return "中国移动";
//			}else if(tp.equals("01") || tp.equals("06")){
//				return "中国联通";
//			}else if(tp.equals("03") || tp.equals("05")){
//				return "中国电信";
//			}else{
//				return "未识别";
//			}
//		}else if(tm == null || tm.equals("") || tm.trim().equals("null") || tm.trim().equals("")){
//			return "未识别";
//		}
//		return tm;
//	}
	
//	/**
//	 * 获取网络类型和最高速率
//	 * @return
//	 */
//	public String[] getNetWorkTypeAndMaxSpeed(){
//		String maxSpeed = null;
//		String netType = null;
//		if (tManager.getDataState() == TelephonyManager.DATA_CONNECTED) {
//			switch (tManager.getNetworkType()) {
//			case TelephonyManager.NETWORK_TYPE_GPRS:
//				netType = "GPRS";
//				maxSpeed = "64Kbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_EDGE:
//				netType = "EDGE";
//				maxSpeed = "384Kbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_CDMA:
//				netType = "CDMA";
//				maxSpeed = "64Kbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_EVDO_0:
//				netType = "EVDO-0";
//				maxSpeed = "2.4Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_EVDO_A:
//				netType = "EVDO-A";
//				maxSpeed = "3.1Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_EVDO_B:
//				netType = "EVDO-B";
//				maxSpeed = "9.3Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_UMTS:
//				netType = "UMTS";
//				maxSpeed = "384Kbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_HSDPA:
//				netType = "HSDPA";
//				maxSpeed = "14.4Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_HSUPA:
//				netType = "HSUPA";
//				maxSpeed = "5.76Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_HSPA:
//				netType = "HSPA";
//				maxSpeed = "14.4Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_HSPAP:
//				netType = "HSPA+";
//				maxSpeed = "42Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_1xRTT:
//				netType = "CDMA 1xRTT";
//				maxSpeed = "153.6Kbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_EHRPD:
//				netType = "eHRPD";
//				maxSpeed = "3.1Mbps";
//				break;
//			case TelephonyManager.NETWORK_TYPE_LTE:
//				netType = "LTE";
//				maxSpeed = "150Mbps";
//				break;
//			default:
//
//			}
//		}
//		return new String[]{netType, maxSpeed};
//	}
	
//	/**
//	 * 数据状态
//	 * @return
//	 */
//	public String getDataState(){
//		String dataState = null;
//		switch (tManager.getDataState()) {
//		case TelephonyManager.DATA_DISCONNECTED:
//			dataState = "未连接";
//			break;
//		case TelephonyManager.DATA_CONNECTED:
//			dataState = "已连接";
//			break;
//		case TelephonyManager.DATA_CONNECTING:
//			dataState = "正在连接";
//			break;
//		case TelephonyManager.DATA_SUSPENDED:
//			dataState = "暂停";
//			break;
//		default:
//			break;
//		}
//		return dataState;
//	}
	
//	/*
//	 * 获取手机类型
//	 */
//	public String getPhoneType(){
//		String phoneType = null;
//		switch (tManager.getPhoneType()) {
//		case TelephonyManager.PHONE_TYPE_NONE:
//			phoneType = "NONE";
//			break;
//		case TelephonyManager.PHONE_TYPE_CDMA:
//			phoneType = "CDMA";
//			break;
//		case TelephonyManager.PHONE_TYPE_GSM:
//			phoneType = "GSM";
//			break;
//		case TelephonyManager.PHONE_TYPE_SIP:
//			phoneType = "SIP";
//			break;
//		default:
//			break;
//		}
//		return phoneType;
//	}

}
