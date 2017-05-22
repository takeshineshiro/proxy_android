package cn.wsds.gamemaster.data;

import java.util.List;

public class SingInfo {
	
	public final String resultCode;
	public final String errorInfo;
	/**
	 * 0 未签到
	 * 1 已签到
	 */
	public final List<Integer> history;
	public final Byte dayOfWeek;
	public SingInfo(String resultCode, String errorInfo,List<Integer> history, Byte dayOfWeek) {
		this.resultCode = resultCode;
		this.errorInfo = errorInfo;
		this.history = history;
		this.dayOfWeek = dayOfWeek;
	}
	
}
