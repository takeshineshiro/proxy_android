package cn.wsds.gamemaster.data;



/**
 * 日期的时分秒值 及单位
 */
public class DateParams {

	/** 小时数 */
	public final int hour;
	/** 分钟数 */
	public final int minute;
	/** 秒数 */
	public final int second;
	/** 格式化显示 小时的文本 */
	public final String hourUnit;
	/** 格式化显示 分钟的文本 */
	public static final String UNIT_MINUTE = "分";
	/** 格式化显示 秒数的文本 */
	public static final String UNIT_SECOND = "秒";
	private DateParams(int hour, int minute, int second, String hourUnit) {
		super();
		this.hour = hour;
		this.minute = minute;
		this.second = second;
		this.hourUnit = hourUnit;
	}
	
	/**
	 * 
	 * @param accelTime 单位为秒的时间值
	 * @param hourFormat 小时的单位数
	 * @return
	 */
	public static DateParams build(long accelTime,String hourFormat) {
		int second = (int) (accelTime % 60);
		int minute = (int) ((accelTime / 60) % 60);
		int hour = (int) (accelTime / 3600);
		return new DateParams(hour, minute, second, hourFormat);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(hour);
		builder.append(hourUnit);
		builder.append(minute);
		builder.append(UNIT_MINUTE);
		builder.append(second);
		builder.append(UNIT_SECOND);
		return builder.toString();
	}
	
	/**
	 * 去除 为0的值
	 * @return
	 */
	public String discardNullValue(){
		StringBuilder builder = new StringBuilder();
		append(builder,hour,hourUnit);
		append(builder,minute,UNIT_MINUTE);
		if(second==0){
			builder.append("0"+UNIT_SECOND);
		}else{
			append(builder,second,UNIT_SECOND);
		}
		
		return builder.toString();
	}

	private void append(StringBuilder builder, int value, String unit) {
		if(value > 0){
			builder.append(value);
			builder.append(unit);
		}
	}
}
