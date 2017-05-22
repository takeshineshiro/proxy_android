package cn.wsds.gamemaster.tools;

import android.text.TextUtils;
import cn.wsds.gamemaster.Misc;


public class MobileSystemTypeUtil {
	
	private static SystemProp systemProp = null;

	/**
	 * 系统类型
	 */
	public enum SystemType{
		MIUI,  //小米系统MIUI
		EMUI,  //华为系统EMUI
		MX,//魅族
		FUNTOUCH, //vivo系统	
		UNKNOWN;  //其他系统
	}
	
	/**
	 * 获得当前手机UI类型
	 * @return
	 */
	public static SystemType getSystemType(){
		return getSystemProp().type;
	}
	
	public static SystemProp getSystemProp() {
		if(systemProp != null)
			return systemProp;
		systemProp = buildSystemProp();
		return systemProp;
	}

	private static SystemProp buildSystemProp() {
		String prop = Misc.getSystemProperty("ro.miui.ui.version.name");
		if (!TextUtils.isEmpty(prop)) {
			return new SystemProp(SystemType.MIUI, prop);
		}
		String emuiProp = Misc.getSystemProperty("ro.build.version.emui");
		if (!TextUtils.isEmpty(emuiProp)) {
			return new SystemProp(SystemType.EMUI, emuiProp);
		}
		String meizuProp = Misc.getSystemProperty("ro.meizu.product.model");
		if (!TextUtils.isEmpty(meizuProp)) {
			return new SystemProp(SystemType.MX, meizuProp);
		}
		String vivoProp = Misc.getSystemProperty("ro.vivo.os.build.display.id");
		if(!TextUtils.isEmpty(vivoProp)){
			return new SystemProp(SystemType.FUNTOUCH,vivoProp);
		}
		return new SystemProp(SystemType.UNKNOWN, null);
	}
	
	/**
	 * 封装了需要提示打开悬浮窗的UI类型和版本信息
	 * 
	 * @author Administrator
	 * 
	 */
	public static final class SystemProp {
		/**
		 * 不详时为null
		 */
		public final String prop;
		public final SystemType type;

		public SystemProp(SystemType type, String prop) {
			this.prop = prop;
			this.type = type;
		}
	}
}
