package cn.wsds.gamemaster.debugger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

abstract class FragmentDebug extends Fragment {

	protected abstract int getRootLayoutResId();

	protected abstract void initView(View root);

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(getRootLayoutResId(), container, false);
		initView(root);
		return root;
	}

	/**
	 * 打开调试的二级界面，并根据id 选择对应的fragment并设置标题
	 * 
	 * @param id
	 */
	protected void turnSecondDebugPage(int id) {
		Intent i = new Intent(getActivity(), ActivityDebuggerSecondPage.class);
		i.putExtra(ActivityDebuggerSecondPage.KEY_DEBUGGER_SECOND_PAGE, id);
		startActivity(i);
	}
}
