package cn.wsds.gamemaster.dialog;

import com.subao.net.NetManager;
import com.subao.resutils.WeakReferenceHandler;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.VPNUtils;

/**
 * 重新加速逻辑对话框
 */
public class ReaccelPrompt {

	private static final String TAG = "ReaccelPrompt" ;
	public static void execute(Activity activity, int gameUID) {
		if(NetManager.getInstance().isDisconnected()){
			neterrorPormpt(activity);
			Statistic.addEvent(activity, Statistic.Event.ACC_GAME_RESTART,"网络断开");
			return;
		}
		Statistic.addEvent(activity, Statistic.Event.ACC_GAME_RESTART,"网络有连接");
	    ReaccelProgressDialog.showInstance(activity, gameUID);
	}

	/**
	 * 网络异常对话框
	 */
	private static void neterrorPormpt(Activity activity) {
		String message = "当前网络已断开，请稍后再试";
		String buttonMess = "确定";
		CommonDialog dialog = new CommonAlertDialog(activity);
		dialog.setMessage(message);
		dialog.setPositiveButton(buttonMess, null);
		dialog.show();
	}

	/**
	 * 对话框提示可能会中断当前正在进行的游戏
	 */
	private static final class ReaccelProgressDialog extends Dialog {

		private ViewFlipper viewFlipper;
		private ProgressUpdate progressUpdate;
		

		public static void showInstance(Context context, int gameUID) {
			ReaccelProgressDialog dlg = new ReaccelProgressDialog(context, gameUID);
			dlg.show();
		}

		private ReaccelProgressDialog(Context context, int gameUID) {
			super(context, R.style.AppDialogTheme);
			initViews(context);
			progressUpdate.start();
			VPNUtils.startNodeDetect(gameUID, true, TAG);
		}

		private void initViews(Context context) {
			setContentView(R.layout.dialog_reaccel);
			
			viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
			viewFlipper.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom));
			viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_out_top));
			
			findViewById(R.id.button_confirm).setOnClickListener(new View.OnClickListener() {
			
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});
			TextView textView = (TextView) findViewById(R.id.text_remind);
			progressUpdate = new ProgressUpdate(context, textView);
			setCanceledOnTouchOutside(false);
		}

		private void progressEnd() {
			viewFlipper.showNext();
		}

		/**
		 * 进度更新
		 */
		private final class ProgressUpdate {
			/** 动画最大时长 */
			private static final int MAX_TIME = 5000;
			/** 图片Level maximum */
			private static final int TOTAL_LENGTH = 10000;
			/** 单位时间内 图片level进度 */
			private static final int PER_SCHEDULE = TOTAL_LENGTH / MAX_TIME;
			/** 动画开始时间 */
			private long benginTime = 0;
			private Drawable drawable;
			private ProgressUpdateHandler handler = new ProgressUpdateHandler(this);

			@SuppressWarnings("deprecation")
			private ProgressUpdate(Context context, TextView textView) {
				Resources resources = context.getResources();
				drawable = resources.getDrawable(R.drawable.reaccel_progress);
				drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
				textView.setCompoundDrawables(null, drawable, null, null);
			}

			/**
			 * 开始刷新进度
			 */
			public void start() {
				handler.sendEmptyMessageDelayed(ProgressUpdateHandler.MSG_REACCEL_PROGRESS, 500);
			}

			/**
			 * 当进度有更新的时候
			 */
			public void onReaccelProgressChange() {
				if (benginTime == 0) {
					benginTime = SystemClock.elapsedRealtime();
				}
				long elapsedTime = SystemClock.elapsedRealtime() - benginTime;
				if (elapsedTime >= MAX_TIME) { //动画到了最大时长执行结束
					handler.removeMessages(ProgressUpdateHandler.MSG_REACCEL_PROGRESS);
					drawable.setLevel(TOTAL_LENGTH);
					ReaccelProgressDialog.this.progressEnd();
					return;
				}
				int level = Long.valueOf(elapsedTime * PER_SCHEDULE).intValue();
				drawable.setLevel(level);
			}
		}

		/**
		 * 刷新进度handler
		 */
		private static final class ProgressUpdateHandler extends WeakReferenceHandler<ProgressUpdate> {
			public static final int MSG_REACCEL_PROGRESS = 0;

			public ProgressUpdateHandler(ProgressUpdate ref) {
				super(ref);
			}

			@Override
			protected void handleMessage(ProgressUpdate ref, Message msg) {
				switch (msg.what) {
				case MSG_REACCEL_PROGRESS:
					sendEmptyMessageDelayed(ProgressUpdateHandler.MSG_REACCEL_PROGRESS, 100);
					ref.onReaccelProgressChange();
					break;
				}
			}
		}
	}
}
