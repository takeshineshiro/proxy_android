package cn.wsds.gamemaster.ui.floatwindow.skin;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.FloatWindowMeasure;
/**
 * 大悬浮窗资源（正常版）
 */
public class SkinResourceBoxInGameNormal extends SkinResourceBoxInGame{

	@Override
	public int getLayoutResource() {
		return R.layout.box_in_game;
	}

	@Override
	public int getButtonCurveResourceChecked() {
		return R.drawable.popup_window_background_butten_graph;
	}

	@Override
	public int getButtonCurveResourceUnChecked() {
		return R.drawable.popup_window_background_butten_graph_current;
	}

	@Override
	public int getButtonDetailsResourceChecked() {
		return  R.drawable.popup_window_background_butten_information;
	}

	@Override
	public int getButtonDetailsResourceUnChecked() {
		return  R.drawable.popup_window_background_butten_information_current;
	}

	@Override
	public int getButtonSettingResourceChecked() {
		return R.drawable.popup_window_background_butten_set_up;
	}

	@Override
	public int getButtonSettingResourceUnChecked() {
		return R.drawable.popup_window_background_butten_set_up_current;
	}

	@Override
	public int getButtonLogUploadResourceChecked() {
		return R.drawable.popup_window_background_butten_update;
	}
	
	@Override
	public int getButtonLogUploadResourceUnChecked() {
		return R.drawable.popup_window_background_butten_update_current;
	}
	
	@Override
	public int getButtonScreenShotResourceChecked() {
		return R.drawable.popup_window_background_butten_shear;
	}

	@Override
	public int getButtonScreenShotResourceUnChecked() {
		return R.drawable.popup_window_background_butten_shear_current;
	}

	@Override
	public int getButtonEnableBackgroundRecource() {
		return R.drawable.float_button_bg;
	}

	@Override
	public int getButtonUnEnableBackgroundRecource() {
		return R.drawable.popup_window_background_butten_press;
	}

	@Override
	public int getBackgroundResource() {
		switch (FloatWindowMeasure.getCurrentType()) {
		case MINI:
			return R.drawable.popup_window_background_left_short_mini;
		case LARGE:
			return R.drawable.popup_window_background_left_short_huge;
		case NORMAL:
		default:
			return R.drawable.popup_window_background_left_short;
		}
	}

	@Override
	public int getButtonBackgroundResourceOnSelected() {
		return R.drawable.popup_window_background_butten_selected;
	}

	@Override
	public int getNetNameTextColorId() {
		return R.color.color_game_31;
	}
}
