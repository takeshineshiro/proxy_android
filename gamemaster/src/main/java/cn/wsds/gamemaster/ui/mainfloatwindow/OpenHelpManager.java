package cn.wsds.gamemaster.ui.mainfloatwindow;

import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemProp;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemType;
import cn.wsds.gamemaster.ui.UIUtils;

public class OpenHelpManager {
	
	public interface OnFinshListener{
		public void onFinish();
	}

	/**
	 * 
	 * @return
	 *   return null 或者可以引导设置悬浮窗
	 */
	public static OpenFloatwindowHelp createHelper(){
		if(!ConfigManager.getInstance().getShowFloatWindowInGame()){
			return new SettingHelp();
		}
		
		SystemProp sp = MobileSystemTypeUtil.getSystemProp();
		// 小米或华为
		if (sp.type == SystemType.EMUI || sp.type == SystemType.MIUI) {
			return createSpecialUIHelper();
		}
        // 魅族的新型号手机		 
		if(sp.type==SystemType.MX){  		 
			if(OpenHelpManager.canOpenFloatWindowOnMX(sp.prop)){
				return createSpecialUIHelper();
			}
		}

		if(!ConfigManager.getInstance().getShowUsageStateHelpDialog()){
			// 授权引导已经显示过不要再显示
			return null;
		}
		if(UIUtils.isLollipopUser()){
			if(AppsWithUsageAccess.hasModule() && !AppsWithUsageAccess.hasEnable()){
				return new CommonUIHelp();
			}
		}
		return null;
	}
	
	private static OpenFloatwindowHelp createSpecialUIHelper() {
		if(isHelpUIWithoutShown()){
			if(UIUtils.isLollipopUser()){
				return new SpecialUILollipopHelp();
			}else{
				return new SpecialUIHelp();
			}
		}
		return null;
	}
	
	private static boolean isHelpUIWithoutShown(){
		return ConfigManager.getInstance().getOpenFloatWindowHelpPageCount() == 0;
	}
	
	public static boolean canOpenFloatWindowOnMX(String prop){
		return ("M576".equals(prop)||"57AA".equals(prop)||"M5710".equals(prop)) ;
	}
	
}
