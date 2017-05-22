package cn.wsds.gamemaster.ui.floatwindow;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.utils.MetricsUtils;

public class ToastEx {

	/** 表示成功和失败的效果 */
	public static enum Effect {
		/** 成功时的边框 */
		SUCCEED,
		/** 失败时的边框 */
		FAIL,
	}
	
	private enum Background {
		SMALL,
		LARGE,
	}
	
	/**
	 * 特效策略
	 */
	public static interface Strategy {

		public static class Param {
			public String text;
			public ToastEx.Effect effect;
		}

		/**
		 * 根据序号取要显示的文本和效果
		 * 
		 * @param textIndex
		 *            序号
		 * @return Param，其中text字段为null表示没有要显示的文本了
		 */
		Param getToastDrawParam(int textIndex);

		/**
		 * 当文本开始显示的时候，通知Strategy
		 * 
		 * @param textIndex
		 *            是第几条文本？
		 */
		void onTextDisplay(int textIndex);

		/**
		 * 向Strategy询问：单条文本是否可以渐隐消失了？
		 * 
		 * @param textIndex
		 *            是第几条文本？
		 * @return true表示可以消失了<br />
		 *         false表示不可以消失，继续显示这条文本
		 */
		boolean isTextNeedFadeOut(int textIndex);

	}
	
	public interface OnToastExOverListener {
		public void onToastExOver(int id);
	}

	private static final Queue<ToastEx> queue = new LinkedList<ToastEx>();
	private static ToastEx current;
	
	private static int id_seed;

	private final int id;
	private final Strategy strategy;
	private final OnToastExOverListener onOverListener;
	
	private Wnd wnd;

	private ToastEx(Strategy strategy, OnToastExOverListener onOverListener) {
		this.id = ++id_seed;
		this.onOverListener = onOverListener;
		this.strategy = strategy;
	}

	private static class ViewParams {
		public final Rect rect;
		public final boolean toLeft;

		public ViewParams(Rect rect, boolean toLeft) {
			this.rect = rect;
			this.toLeft = toLeft;
		}
	}

	/** 计算View的位置和尺寸 */
	private static ViewParams calcViewRect(Context context , boolean isNormalNetState) {
		int x, y;
		boolean toLeft;
		Point screenSize = MetricsUtils.getDevicesSizeByPixels(context);
		int viewWidth ;
		if(isNormalNetState){
			viewWidth = (Math.min(screenSize.x, screenSize.y) << 1) / 3;
		}else{
			viewWidth = (Math.min(screenSize.x, screenSize.y)<<3) / 9;
		}
		  
		Bitmap bmpRing = FloatWindowCommon.uiRes.getBitmap(context, R.drawable.suspension_circle_good);
		int viewHeight = bmpRing.getHeight();
//		FloatWindowInGame fwig = FloatWindowInGame.getInstance();
//		if (fwig == null) {
			x = (screenSize.x - viewWidth) >> 1;
			y = screenSize.y >> 2;
			toLeft = false;
//		} else {
//			x = fwig.getX();
//			if (x + (bmpRing.getWidth() >> 1) < (screenSize.x >> 1)) {
//				toLeft = false;
//			} else {
//				toLeft = true;
//				x = x - viewWidth + bmpRing.getWidth();
//			}
//			y = fwig.getY();
//		}
		Rect rect = new Rect(x, y, x + viewWidth, y + viewHeight);
		return new ViewParams(rect, toLeft);
	}
	
//	public static void show(Context context, Background background, int x, int y, int width, String... messages) {
//		show(context, background, x, y, width, new ArrayList<String>(Arrays.asList(messages)));
//	}
	
//	public static void show(Context context, Background background, int x, int y, int width, List<String> messages) {
//		show(context, background, x, y, width, new FixedTimeStrategy(messages));
//	}
	
//	private static void show(Context context, Background background, int x, int y, int width, Strategy strategy) {
//		ToastEx t = new ToastEx(strategy);
//		if (current != null || !queue.isEmpty()) {
//			queue.offer(t);
//		} else {
//			t.execute(context);
//		}
//	}

	public static int show(Context context, List<String> messages, OnToastExOverListener onOverListener, 
			boolean isNormalNetState) {
		if(isNormalNetState){
			return show(context, new FixedTimeStrategy(messages,ToastEx.Effect.SUCCEED), onOverListener,isNormalNetState);
		}else{
			return show(context, new FixedTimeStrategy(messages,ToastEx.Effect.FAIL), onOverListener,isNormalNetState);
		}
	}

	public static int show(Context context, Strategy strategy, OnToastExOverListener onOverListener , 
			boolean isNormalNetState) {
		ToastEx t = new ToastEx(strategy, onOverListener);
		if (((current != null || !queue.isEmpty()))&& isNormalNetState) {
			queue.offer(t);
		} else {
			t.execute(context,isNormalNetState);
		}
		
		return t.id;
	}
	
	public static int getHeight(Context context, Background background) {
		NinePatch np = getBackgroundNinePatch(context, background);
		return np == null ? 0 : np.getHeight();
	}
	
	private static NinePatch getBackgroundNinePatch(Context context, Background background) {
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), background == Background.SMALL ?
				R.drawable.toast_frame_success : R.drawable.toast_frame_success);
		return UIUtils.createNinePatch(bmp);
	}
	
	/**
	 * 当前是否存在一个ToastEx正在显示或等待显示？
	 * @return
	 */
	public static boolean exists() {
		return !(current == null && queue.isEmpty());
	}

	private void execute(Context context , final boolean isNormalNetState) {
		current = this;
		wnd = new Wnd(context);
		ViewParams params = calcViewRect(context,isNormalNetState);
		wnd.addView(FloatWindow.Type.TOAST, new ViewToastEx(context, wnd, params.rect, params.toLeft, strategy,isNormalNetState), 
				params.rect.left, params.rect.top);
		wnd.setOnDestroyListener(new FloatWindow.OnDestroyListener() {
			@Override
			public void onFloatWindowDestroy(FloatWindow who) {
				if (onOverListener != null) {
					onOverListener.onToastExOver(id);
				}
				current = null;
				ToastEx next = queue.poll();
				if (next != null) {
					next.execute(wnd.getContext(),isNormalNetState);
				}
			}
		});
	}

	public static class FixedTimeStrategy implements ToastEx.Strategy {

		private final List<String> messages;
		private final ToastEx.Strategy.Param param;

		public FixedTimeStrategy(List<String> messages, ToastEx.Effect effect) {
			this.messages = messages;
			this.param = new ToastEx.Strategy.Param();
			this.param.effect = effect;
		}

		@Override
		public ToastEx.Strategy.Param getToastDrawParam(int textIndex) {
			if (textIndex >= 0 && textIndex < messages.size()) {
				this.param.text = messages.get(textIndex);
			} else {
				this.param.text = null;
			}
			return this.param;
		}

		@Override
		public void onTextDisplay(int textIndex) {

		}

		@Override
		public boolean isTextNeedFadeOut(int textIndex) {
			return true;
		}

	}

	private static class Wnd extends FloatWindow {

		Wnd(Context context) {
			super(context);
		}

		@Override
		protected void onViewAdded(View view) {}
		
		@Override
		protected boolean canDrag() {
			return false;
		}
	}


}
