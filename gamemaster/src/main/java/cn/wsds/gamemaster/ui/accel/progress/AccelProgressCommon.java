package cn.wsds.gamemaster.ui.accel.progress;

import android.view.ViewGroup;

public class AccelProgressCommon {

	public static void setChildrenAlpha(ViewGroup parent, float alpha) {
		for (int i = parent.getChildCount() - 1; i >= 0; --i) {
			parent.getChildAt(i).setAlpha(alpha);
		}
	}
}
