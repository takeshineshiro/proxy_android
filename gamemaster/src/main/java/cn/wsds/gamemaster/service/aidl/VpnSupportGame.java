package cn.wsds.gamemaster.service.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class VpnSupportGame implements Parcelable{

	private int uid;
	private String packageName;
	private String appLabel;
	private int protocol;
	private boolean isForeign;
	private String whitePorts;
	private String blackPorts;
	private String blackIps;
	private String whiteIps;

	public VpnSupportGame(int uid ,String packageName,String appLabel ,int protocol,
			boolean isForeign,String whitePorts,String blackPorts, String blackIps, String whiteIps){
		this.uid = uid ;
		this.packageName = packageName;
		this.appLabel = appLabel;
		this.protocol = protocol ;
		this.isForeign = isForeign ;
		this.whitePorts = whitePorts ;
		this.blackPorts = blackPorts ;
		this.blackIps = blackIps;
		this.whiteIps = whiteIps;
	}
	
	private VpnSupportGame(Parcel in){
		uid = in.readInt();
		packageName = in.readString();
		appLabel=in.readString();
		protocol = in.readInt();
		boolean[] bArray = in.createBooleanArray();
		if((bArray!=null)&&(bArray.length>0)){
			isForeign = bArray[0];
		}
		whitePorts = in.readString();
		blackPorts = in.readString();
		blackIps = in.readString();
		whiteIps = in.readString();
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(uid);
		dest.writeString(packageName);
		dest.writeString(appLabel);
		dest.writeInt(protocol);
		dest.writeBooleanArray(new boolean[]{isForeign});
		dest.writeString(whitePorts);
		dest.writeString(blackPorts);
		dest.writeString(blackIps);
		dest.writeString(whiteIps);
	}
	
	public static final Parcelable.Creator<VpnSupportGame> CREATOR = new Parcelable.Creator<VpnSupportGame>() {

		@Override
		public VpnSupportGame createFromParcel(Parcel in) {	
			return new VpnSupportGame(in);
		}

		@Override
		public VpnSupportGame[] newArray(int size) {		
			return new VpnSupportGame[size];
		}
	};

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getAppLabel() {
		return appLabel;
	}

	public void setAppLabel(String appLabel) {
		this.appLabel = appLabel;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public boolean isForeign() {
		return isForeign;
	}

	public void setForeign(boolean isForeign) {
		this.isForeign = isForeign;
	}

	public String getWhitePorts() {
		return whitePorts;
	}

	public void setWhitePorts(String whitePorts) {
		this.whitePorts = whitePorts;
	}

	public String getBlackPorts() {
		return blackPorts;
	}

	public void setBlackPorts(String blackPorts) {
		this.blackPorts = blackPorts;
	}

	public String getBlackIps() {
		return blackIps;
	}

	public String getWhiteIps() {
		return whiteIps;
	}
}
