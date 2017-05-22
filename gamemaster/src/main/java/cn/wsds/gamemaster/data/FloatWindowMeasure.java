package cn.wsds.gamemaster.data;


public class FloatWindowMeasure {

	/**
	 * 顺序和取值都不能改变（因为要存盘）
	 */
	public enum Type {
		/** 常规大小 */
		NORMAL,
		/** 小尺寸 */
		MINI,
		/** 最大尺寸 */
		LARGE
	}
	
	public static boolean setCurrentType(Type type) {
		if (!ConfigManager.getInstance().setFloatwindowMeasureType(type.ordinal())) {
			return false;
		}
//		Event statisticEvent;
//		switch (type) {
//		case MINI:
//			statisticEvent = Event.SWITCH_FLOATINGWINDOW_SIZE_MINI;
//			break;
//		default:
//			statisticEvent = Event.SWITCH_FLOATINGWINDOW_SIZE_DEFAULT;
//			break;
//		}
//		StatisticDefault.addEvent(AppMain.getContext(), statisticEvent);
		return true;
	}

	public static Type getCurrentType() {
		int t = ConfigManager.getInstance().getFloatwindowMeasureType();
		Type[] values = Type.values();
		if(t<0 || t>values.length){
			return Type.NORMAL;
		}else{
			return values[t];
		}
	}

}
