package cn.wsds.gamemaster.ui.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import cn.wsds.gamemaster.R;

public class FragmentMessage_PreventClean extends FragmentMessage {

	@Override
	public int getTitleResId() {
		return R.string.message_title_prevent_clean;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.message_prevenr_clean, container, false);
	}

}
