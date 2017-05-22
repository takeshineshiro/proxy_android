package cn.wsds.gamemaster.useraction;

import com.subao.common.utils.StringUtils;

/**
 * 用户行为日志
 */
public class UserAction {
	
	/** 事件名 */
	public final String name;
	
	/** 发生本条日志的时刻（UTC的秒） */
	public final long timeOfUTCSeconds;
	
	/** 事件参数 */
	public final String param;

	/**
	 * 构造
	 * @param timeOfUTCSeconds 时刻，UTC的秒
	 * @param name 事件名
	 * @param param 事件参数
	 */
	public UserAction(long timeOfUTCSeconds, String name, String param) {
		this.timeOfUTCSeconds = timeOfUTCSeconds;
		this.name = name;
		this.param = param;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (null == o) {
			return false;
		}
		if (!(o instanceof UserAction)) {
			return false;
		}
		UserAction other = (UserAction) o;
		return this.timeOfUTCSeconds == other.timeOfUTCSeconds
			&& StringUtils.isStringSame(this.name, other.name)
			&& StringUtils.isStringSame(this.param, other.param);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		sb.append("[UserAction (").append(name);
		if (param != null) {
			sb.append(',').append(param);
		}
		return sb.toString();
	}
}
