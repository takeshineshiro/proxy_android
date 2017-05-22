package cn.wsds.gamemaster.ui;

import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.dialog.DoubleAccelPormpt;
import cn.wsds.gamemaster.dialog.UsageStateHelpDialog;
import cn.wsds.gamemaster.dialog.UserCenterPormpt;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemProp;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemType;
import cn.wsds.gamemaster.ui.view.OpenFloatWindowHelpDialog;

public class MainHelp {

	/**
	 * 帮助UI的类型
	 */
	public enum HelpType {

		/** 小米和华为手机的开启悬浮窗引导 */
		OPEN_FLOAT_WINDOW,
		
		/**  “查看应用使用权限” 引导 */
		OPEN_USAGE_STATE,

		/** 在APP内启动游戏引导 */
		OPEN_GAME_INSIDE,
		
		/** “用户中心”引导 */
		OPEN_USER_CENTER;
	}

	/**
	 * 判断给定的帮助UI是否需要且现在可以显示？
	 * @param helpType 
	 * @param activity 基于哪个Activity？
	 * @param hasSupportGame 是否至少有一个支持的游戏？
	 * @param checkSelfHistoryShown 是否检查以前是否显示过（如果此参数为True，则以前如果显示过，本函数将返回False）
	 * @return true表示需要/可以显示，false表示不需要或现在不能显示
	 */
	public static boolean canShowHelpUI(HelpType helpType, ActivityBase activity, boolean hasSupportGame, boolean checkSelfHistoryShown) {
		if (activity.isFinishing()) {
			return false;
		}
		if (!activity.isForeground()) {
			return false;
		}
		if (StartNodeDetectUI.doesInstanceExists()) {
			return false;
		}
		
		OpenHelper openHelper = createHelper(helpType, hasSupportGame);
		if(openHelper == null){
			return false;
		}
		if (checkSelfHistoryShown && openHelper.doesHelpUIHasBeenShown()) {
			return false;
		}
		if (isHelpUIShowing()) {
			return false;
		}
		return openHelper.canShowHelpUI();
	}
	
	private static boolean isHelpUIShowing() {
		return OpenFloatWindowHelpDialog.isInstanceExists() || HelpUIForStartGameInside.isInstanceExists() ||
				UserCenterPormpt.isInstanceExists()||
				UsageStateHelpDialog.isInstanceExists();
	}

	private static OpenHelper createHelper(HelpType helpType,boolean hasSupportGame){
		switch (helpType) {
		case OPEN_FLOAT_WINDOW:
			return new OpenFloatwindowHelper();
		case OPEN_GAME_INSIDE:
			return new OpenGameInsideHelper(hasSupportGame);
		case OPEN_USAGE_STATE:
			return new OpenUsageStateHelper();
		case OPEN_USER_CENTER:
			return new OpenUserCenterHelper();
		default:
			return null;
		}
	}
	/**
	 * 关闭所有帮助UI
	 */
	public static void close() {
		OpenFloatWindowHelpDialog.close();
		UsageStateHelpDialog.close();
		HelpUIForStartGameInside.close();
		UserCenterPormpt.close();
        DoubleAccelPormpt.close();
	}
	
	private interface OpenHelper {
		public abstract boolean canShowHelpUI();
		public abstract boolean doesHelpUIHasBeenShown();
	}
	
	private static final class OpenFloatwindowHelper implements OpenHelper{

		@Override
		public boolean canShowHelpUI() {
			SystemProp sp = MobileSystemTypeUtil.getSystemProp();
			if (sp.type == SystemType.EMUI || sp.type == SystemType.MIUI) {
				return true;
			} else {
				ConfigManager.getInstance().setOpenFloatWindowHelpPageCount(1);
				return false;
			}
		}

		@Override
		public boolean doesHelpUIHasBeenShown() {
			return ConfigManager.getInstance().getOpenFloatWindowHelpPageCount() > 0;
		}
	}
	
	private static final class OpenUsageStateHelper implements OpenHelper {

		@Override
		public boolean canShowHelpUI() {
			return AppsWithUsageAccess.hasModule() && !AppsWithUsageAccess.hasEnable();
		}

		@Override
		public boolean doesHelpUIHasBeenShown() {
			return !ConfigManager.getInstance().getShowUsageStateHelpDialog();
		}
	}
	
	private static final class OpenGameInsideHelper implements OpenHelper {

		private final boolean hasSupportGame;
		private OpenGameInsideHelper(boolean hasSupportGame) {
			this.hasSupportGame = hasSupportGame;
		}

		@Override
		public boolean canShowHelpUI() {
			return hasSupportGame;
		}
		@Override
		public boolean doesHelpUIHasBeenShown() {
			return 0 != (ConfigManager.getInstance().getHelpUIStatus() & ConfigManager.HELP_UI_STATUS_START_GAME_INSIDE);
		}
	}
	
	private static final class OpenUserCenterHelper implements OpenHelper {

		@Override
		public boolean canShowHelpUI() {
			return !ActivityBase.hasSmartBar();
		}

		@Override
		public boolean doesHelpUIHasBeenShown() {
			return 0 != (ConfigManager.getInstance().getHelpUIStatus() & ConfigManager.HELP_UI_STATUS_USER_CENTER_PORMPT);
		}
		
	}
	
}
