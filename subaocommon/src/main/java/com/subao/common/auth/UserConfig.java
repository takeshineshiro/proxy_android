package com.subao.common.auth;

import com.subao.common.Misc;

/**
 * 用户配置（SDK）
 */
public class UserConfig {

	public final String value;

	public final boolean accel;

	public final boolean parallel;

	public final char accelMode;

    /**
     * 判断给定的配置字符串里，Parallel开关状态是否打开？
     * @param configString 给定的形如“110”这样的配置字符串
     * @return true 表示Parallel开关片于“开”的位置
     */
    public static boolean isParallelSwitchOn(String configString) {
        return configString != null && configString.length() >= 2 && configString.charAt(1) != '0';
    }

    public static UserConfig create(String value) {
        if (value == null || value.length() != 3) {
            return null;
        }
        return new UserConfig(value);
    }

	private UserConfig(String value) {
		this.value = value;
		this.accel = ('0' != value.charAt(0));
		this.parallel = ('0' != value.charAt(1));
		this.accelMode = value.charAt(2);
	}

	public UserConfig(boolean accel, boolean parallel, char accelMode) {
		this.accel = accel;
		this.parallel = parallel;
		this.accelMode = accelMode;
		this.value = String.format("%c%c%c", accel ? '1' : '0', parallel ? '1' : '0', accelMode);
	}

	@Override
	public String toString() {
		return value == null ? "(null)" : value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (!(o instanceof UserConfig)) {
			return false;
		}
		UserConfig other = (UserConfig) o;
		return this.accel == other.accel
			&& this.parallel == other.parallel
			&& this.accelMode == other.accelMode
			&& Misc.isEquals(this.value, other.value);
	}

}
