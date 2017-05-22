package cn.wsds.gamemaster.ui.user;

import android.os.Bundle;
import cn.wsds.gamemaster.ui.ActivityUserAccount;
import cn.wsds.gamemaster.ui.FragmentSubao;

/**
 * 用户注册、登录、密码找回、重置密码等所有帐号相关的操作，都从本类派生
 */
public abstract class FragmentUserAccount extends FragmentSubao {
	
	private OnFragmentViewStateRestoredListener onFragmentViewStateRestoredListener;
	public interface OnFragmentViewStateRestoredListener {
		public void onViewStateRestored();
	}
	
	public void setOnFragmentViewStateRestoredListener(OnFragmentViewStateRestoredListener listener) {
		this.onFragmentViewStateRestoredListener = listener;
	}
	
	protected void changeFragment(int fragmentType) {
		changeFragment(fragmentType,false);
	}
	
	protected void changeFragment(int fragmentType,boolean backHistory) {
		ActivityUserAccount.open(getActivity(), fragmentType,backHistory);
	}
	
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if(onFragmentViewStateRestoredListener!=null){
			onFragmentViewStateRestoredListener.onViewStateRestored();
		}
	}
}
