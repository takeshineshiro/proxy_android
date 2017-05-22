package cn.wsds.gamemaster.service.aidl;

import android.os.Parcel;
import android.os.Parcelable;

import com.subao.common.data.AccelGame;

import java.util.ArrayList;
import java.util.List;

/**
 * 进程之间通信数据
 * Created by hujd on 17-4-7.
 */
public class VpnAccelGame implements Parcelable {
	private String appLabel;
	private int accelMode;
	private int flags;
	private boolean isLabelThreeAsciiChar;

	private List<AccelGame.PortRange> whitePorts;
	private List<AccelGame.PortRange> blackPorts;

	private List<String> blackIps;
	private List<String> whiteIps;


	public String getAppLabel() {
		return appLabel;
	}

	public int getAccelMode() {
		return accelMode;
	}

	public int getFlags() {
		return flags;
	}

	public List<AccelGame.PortRange> getWhitePorts() {
		return whitePorts;
	}

	public List<AccelGame.PortRange> getBlackPorts() {
		return blackPorts;
	}

	public List<String> getBlackIps() {
		return blackIps;
	}

	public List<String> getWhiteIps() {
		return whiteIps;
	}

	public void setAppLabel(String appLabel) {
		this.appLabel = appLabel;
	}

	public void setAccelMode(int accelMode) {
		this.accelMode = accelMode;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public void setLabelThreeAsciiChar(boolean labelThreeAsciiChar) {
		isLabelThreeAsciiChar = labelThreeAsciiChar;
	}

	public void setWhitePorts(Iterable<AccelGame.PortRange> whitePorts) {
		clonePortList(this.whitePorts, whitePorts);
	}

	private void clonePortList(List<AccelGame.PortRange> portList, Iterable<AccelGame.PortRange> ports) {
		if (ports == null) {
			return;
		}
		if (portList == null) {
			portList = new ArrayList<>();
		}

		portList.clear();
		for (AccelGame.PortRange portRange : ports) {
			portList.add(portRange);
		}
	}

	public void setBlackPorts(Iterable<AccelGame.PortRange> blackPorts) {
		clonePortList(this.blackPorts, blackPorts);
	}

	public void setBlackIps(Iterable<String> blackIps) {
		cloneIpList(this.blackIps, blackIps);
	}

	private void cloneIpList(List<String> ipList, Iterable<String> ips) {
		if (ips == null) {
			return;
		}

		if (ipList == null) {
			ipList = new ArrayList<>();
		}

		ipList.clear();
		for (String ip : ips) {
			ipList.add(ip);
		}
	}

	public void setWhiteIps(Iterable<String> whiteIps) {
		cloneIpList(this.whiteIps, whiteIps);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.appLabel);
		dest.writeInt(this.accelMode);
		dest.writeInt(this.flags);
		dest.writeByte(this.isLabelThreeAsciiChar ? (byte) 1 : (byte) 0);
		dest.writeList(this.whitePorts);
		dest.writeList(this.blackPorts);
		dest.writeStringList(this.blackIps);
		dest.writeStringList(this.whiteIps);
	}

	public VpnAccelGame() {
	}

	protected VpnAccelGame(Parcel in) {
		this.appLabel = in.readString();
		this.accelMode = in.readInt();
		this.flags = in.readInt();
		this.isLabelThreeAsciiChar = in.readByte() != 0;
		this.whitePorts = new ArrayList<AccelGame.PortRange>();
		in.readList(this.whitePorts, AccelGame.PortRange.class.getClassLoader());
		this.blackPorts = new ArrayList<AccelGame.PortRange>();
		in.readList(this.blackPorts, AccelGame.PortRange.class.getClassLoader());
		this.blackIps = in.createStringArrayList();
		this.whiteIps = in.createStringArrayList();
	}

	public static final Parcelable.Creator<VpnAccelGame> CREATOR = new Parcelable.Creator<VpnAccelGame>() {
		@Override
		public VpnAccelGame createFromParcel(Parcel source) {
			return new VpnAccelGame(source);
		}

		@Override
		public VpnAccelGame[] newArray(int size) {
			return new VpnAccelGame[size];
		}
	};
}
