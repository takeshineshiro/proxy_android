package cn.wsds.gamemaster.ui.accel;

import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpener.Listener;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailProcesser;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailType;

/**
 * 在桌面开启加速服务结果监听
 */
public class AccelOpenDesktopListener implements Listener {

	private OnEndListener onEndListener;

	public AccelOpenDesktopListener(OnEndListener onEndListener) {
		this.onEndListener = new DefaultOnEndListener(onEndListener);
	}

	@Override
	public void onStartFail(AccelOpener accelOpener,FailType type) {
		/*if (AccelFailType.RootImpowerReject.equals(type)) {
			UIUtils.showToast("获取root权限失败，迅游手游未能开启加速");
		} else*/ if (accelOpener != null) {
			FailProcesser failProcesser = accelOpener.getFailProcesser();
			failProcesser.process(accelOpener,type, null);
		}
		onEndListener.onEnd(false);
	}

	@Override
	public void onStartSucceed() {
		onEndListener.onEnd(true);
	}

	public interface OnEndListener{
		public void onEnd(boolean result);
	}
	
	private static final class DefaultOnEndListener implements OnEndListener {
		
		public final OnEndListener onEndListener;
		private DefaultOnEndListener(OnEndListener onEndListener) {
			this.onEndListener = onEndListener;
		}

		@Override
		public void onEnd(boolean result) {
			if(onEndListener!=null){
				onEndListener.onEnd(result);
			}
		}
		
	}
	
}
