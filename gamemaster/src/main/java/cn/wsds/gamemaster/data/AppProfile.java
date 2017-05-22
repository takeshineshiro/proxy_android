package cn.wsds.gamemaster.data;

public class AppProfile {
	public final String packageName;
	public final String appLabel;

	public AppProfile(String packageName, String appLabel) {
		this.packageName = packageName;
		this.appLabel = appLabel;
	}
	
	@Override
	public String toString() {
		return String.format("[%s %s]", packageName, appLabel);
	}
}
