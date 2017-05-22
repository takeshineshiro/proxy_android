package cn.wsds.gamemaster.debugger.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import cn.wsds.gamemaster.R;

public class FragmentDebugNetDelayChart extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_debugger_netdelay_chart, container, false);
	}
}
