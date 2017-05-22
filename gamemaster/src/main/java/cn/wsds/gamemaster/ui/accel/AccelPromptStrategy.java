package cn.wsds.gamemaster.ui.accel;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

import com.subao.common.utils.CalendarUtils;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.AccelUpgradeDialog;
import cn.wsds.gamemaster.dialog.CommonDesktopDialog;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.user.Identify;

/**开启加速前提示
 * Created by hujd on 17-2-22.
 */
public class AccelPromptStrategy {

	/**
	 * 准备工作
	 */
	public static boolean prepare(Context context) {
		int today = CalendarUtils.todayLocal();
		if (firstPromptLogin(context)) {
			return false;
		}

		if (unloginPrompt(today, context)) {
			return true;
		}

		Identify.VIPStatus vipStatus = Identify.getInstance().getVIPStatus();
		if (vipStatus == Identify.VIPStatus.VIP_VALID) {
			return true;
		}

		if (firstStartPrompt(today)) {
			return true;
		}

		ConfigManager.getInstance().setFirstStrtAccel(false);

		// 是否是新用户
		if (ConfigManager.getInstance().getGuidePage() && ConfigManager.getInstance().isNeedRemindRenew()) {
			AccelUpgradeDialog dialog = new AccelUpgradeDialog(context, false);
			dialog.show();
			ConfigManager.getInstance().setNeedRemindRenew(false);
			return false;
		}

		if (!ConfigManager.getInstance().isEnableDoubleAccel()) {
			UIUtils.showToast("WiFi优化开关未开启，开始普通加速");
		} else {
			AccelUpgradeDialog dialog = new AccelUpgradeDialog(context, true);
			dialog.show();
			return false;
		}

		return true;
	}

	private static boolean firstStartPrompt(int today) {
		if (!ConfigManager.getInstance().getFirstStartAccel()) {
			if (getSameDay(ConfigManager.getInstance().getLastDayOfNormalAceel(), today)) {
				return true;
			}

			UIUtils.showToast("开始普通加速");
			ConfigManager.getInstance().setLastDayOfNormalAceel(today);
			return true;
		}
		return false;
	}

	private static boolean unloginPrompt(int today, final Context context) {
		if (!UserSession.isLogined()) {
			if (getSameDay(ConfigManager.getInstance().getDayOfUnLogin(), today)) {
				return true;
			}
			UIUtils.showToast("未登录状态，仅支持普通加速");
			ConfigManager.getInstance().setDayOfUnLogin(today);
			return true;
		}
		return false;
	}

	private static boolean firstPromptLogin(Context context) {
		if (!UserSession.isLogined() && ConfigManager.getInstance().getFirstPromptLogin()) {
			if (context instanceof Activity) {
				UIUtils.showReloginDialog((Activity) context, "未登录状态，仅支持普通加速", null);
				ConfigManager.getInstance().setFirstPromptLogin(false);
				return true;
			}
		}
		return false;
	}

	private static boolean getSameDay(int day, int today) {
		return today == day;
	}
}
