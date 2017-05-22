package cn.wsds.gamemaster.useraction;


public class VersionInfo {
	public final String number;
	public final String channel;
	public final String osVersion;
	public final String androidVersion;
	
	public VersionInfo(String number, String channel, String osVersion, String androidVersion) {
		this.number = number;
		this.channel = channel;
		this.osVersion = osVersion;
		this.androidVersion = androidVersion;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (o instanceof VersionInfo) {
			VersionInfo other = (VersionInfo)o;
			return com.subao.common.utils.StringUtils.isStringEqual(this.number, other.number)
				&& com.subao.common.utils.StringUtils.isStringEqual(this.channel, other.channel)
				&& com.subao.common.utils.StringUtils.isStringEqual(this.osVersion, other.osVersion)
				&& com.subao.common.utils.StringUtils.isStringEqual(this.androidVersion, other.androidVersion);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return String.format("[VersionInfo: %s, %s, %s, %s]", number, channel, osVersion, androidVersion);
	}
}
