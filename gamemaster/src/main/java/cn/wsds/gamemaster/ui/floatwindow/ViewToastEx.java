package cn.wsds.gamemaster.ui.floatwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowCommon.RobotList;

import com.subao.resutils.WeakReferenceHandler;

/**
 * 显示Toast效果 （向左或右展开，显示文字）
 */
@SuppressLint("ViewConstructor")
public class ViewToastEx extends View {

	//	private static final boolean LOG = false;
	//	private static final String TAG = "ViewToast";

	private static class MyHandler extends WeakReferenceHandler<ViewToastEx> {

		static final int MSG_REDRAW = 1;
		static final int MSG_DESTROY = 2;

		public MyHandler(ViewToastEx ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(ViewToastEx ref, Message msg) {
			switch (msg.what) {
			case MSG_REDRAW:
				ref.invalidate();
				break;
			case MSG_DESTROY:
				ref.parent.destroy();
				break;
			}
		}

	}

	private abstract class State {

		/**
		 * 初始化
		 */
		abstract void init();

		/**
		 * 执行绘制操作
		 * 
		 * @param canvas
		 */
		abstract void draw(Canvas canvas);
	}

	private class StateEnd extends State {

		private static final long LIVE_TIME = 250;
		
		private final NinePatch border;
		private final Bitmap robot;
		private final Rect rect;
		
		private long beginTime; 
		
		StateEnd(NinePatch border, Bitmap robot, Rect rect) {
			this.border = border;
			this.robot = robot;
			this.rect = rect;
		}
		
		@Override
		void init() {
			beginTime = SystemClock.elapsedRealtime();
		}

		@Override
		void draw(Canvas canvas) {
			long elapsed = SystemClock.elapsedRealtime() - beginTime;
			if (elapsed < LIVE_TIME) {
				int alpha = 255 - (int)(255 * elapsed / LIVE_TIME);
				paintNormal.setAlpha(alpha);
				drawBorderAndRobot(canvas, this.border, robot, rect);
			} else {
				myHandler.sendEmptyMessage(MyHandler.MSG_DESTROY);
			}
			
		}
	}

	/** 展开和收拢 */
	private class StateInflate extends State {

		private static final long LIVE_TIME = 300;

		private final ToastEx.Effect effect;
		private final boolean inflate;
		private long beginTime;
		private final NinePatch border;
		private final Rect rect = new Rect();
		private final int minWidth;

		/**
		 * Constructor
		 */
		StateInflate(ToastEx.Effect effect, boolean inflate) {
			this.effect = effect;
			this.inflate = inflate;
			this.border = (effect == ToastEx.Effect.FAIL) ? borderFail : borderSucceed;
			this.minWidth = border.getWidth();
		}

		@Override
		void init() {
			beginTime = SystemClock.elapsedRealtime();
		}

		@Override
		void draw(Canvas canvas) {
			// 流逝的时间
			long elapsed = SystemClock.elapsedRealtime() - beginTime;
			// 计算改变的宽度
			int dx = viewWidth - minWidth;
			if (elapsed < LIVE_TIME) {
				dx = (int) (elapsed * dx / LIVE_TIME);
			}
			// 画外边框和机器人
			int w = inflate ? (minWidth + dx) : (viewWidth - dx);
			if (xRobot == 0) {
				rect.set(0, 0, w, viewHeight);
			} else {
				rect.set(viewWidth - w, 0, viewWidth, viewHeight);
			}
			Bitmap robot = (effect == ToastEx.Effect.FAIL) ? robotFail : robotList.get(0);
			drawBorderAndRobot(canvas, this.border, robot, rect);
			// 本状态是否应该结束了？
			if (elapsed >= LIVE_TIME) {
				if (inflate) {
					changeCurrentState(new StateText());
				} else {
					changeCurrentState(new StateEnd(border, robot, rect));
				}
			}
		}

	}

	/** 显示文本动效 */
	private class StateText extends State {
		private static final long TIME_MESSAGE_IN = 200;	// 文本进入动效时间
		private static final long TIME_MESSAGE_HOLD = 2000; // 进入动效时间+文本停留时间

		private final Rect viewRect;
		private final float yTextCenter;	// 上下居中显示文本时的Y坐标

		private long beginTime;

		private int textIndex;
		private String textPrev;	// 前一条文本

		private ToastEx.Strategy.Param param;
		private NinePatch imgBorder;

		StateText() {
			this.viewRect = new Rect(0, 0, viewWidth, viewHeight);
			yTextCenter = viewHeight - fontHeight;
		}

		@Override
		void init() {
			beginTime = SystemClock.elapsedRealtime();
			param = strategy.getToastDrawParam(0);
		}

		private void changeToNextState() {
			changeCurrentState(new StateInflate(param.effect, false));
		}

		/** 如果要左右居中显示文本，计算它的起始位置 */
		private float calcTextXForCenter(String text, Paint paint) {
			return (viewWidth - paint.measureText(text)) * 0.5f;
		}

		@Override
		void draw(Canvas canvas) {
			long now = SystemClock.elapsedRealtime();
			long elapsed = now - beginTime;
			// 用哪种边框和文字颜色？
			if (param.effect == ToastEx.Effect.SUCCEED) {
				imgBorder = borderSucceed;
				paintText.setColor(textColorSucceed);
			} else {
				imgBorder = borderFail;
				paintText.setColor(textColorFail);
			}
			// 边框和机器人
			drawBorderAndRobot(canvas, imgBorder, robotList.get((int) elapsed / 200), viewRect);
			// 文本
			String textCurrent = param.text;
			if (textCurrent == null && textPrev == null) {
				changeToNextState();
				return;
			}
			if (elapsed <= TIME_MESSAGE_IN) {
				// 本次文字从下方淡入，前一文本向上淡出
				drawTextIn(canvas, param.text, textPrev, elapsed / (float) TIME_MESSAGE_IN);
				return;
			}
			if (textCurrent == null) {
				changeToNextState();
				return;
			}
			// 本次文本停留
			drawTextHold(canvas, textCurrent);
			if (elapsed < TIME_MESSAGE_HOLD) {
				return;
			}
			// 停留时长到了，问一下是不是需要换下一文本
			if (!strategy.isTextNeedFadeOut(textIndex)) {
				return;
			}
			// 要换下一行文本
			++textIndex;
			textPrev = textCurrent;
			param = strategy.getToastDrawParam(textIndex);
			if (param.text == null && textPrev == null) {
				changeToNextState();
			} else {
				beginTime = now;
			}
		}

		private void drawTextIn(Canvas canvas, String textCurrent, String textPrev, float r) {
			int alpha = (int) (255 * r);
			float x, y;
			// 前一文本
			if (textPrev != null) {
				x = calcTextXForCenter(textPrev, paintText);
				y = yTextCenter - fontHeight * r;
				paintText.setAlpha(255 - alpha);
				canvas.drawText(textPrev, x, y, paintText);
			}
			// 本次文本
			if (textCurrent != null) {
				x = calcTextXForCenter(textCurrent, paintText);
				y = yTextCenter + fontHeight * (1f - r);
				paintText.setAlpha(alpha);
				canvas.drawText(textCurrent, x, y, paintText);
			}
		}

		private void drawTextHold(Canvas canvas, String text) {
			if (text != null) {
				paintText.setAlpha(255);
				canvas.drawText(text, calcTextXForCenter(text, paintText), yTextCenter, paintText);
			}
		}

	}

	private final FloatWindow parent;
	private final ToastEx.Strategy strategy;
	private final MyHandler myHandler;

	private final Paint paintNormal = new Paint();
	private final Paint paintText;
	private final RobotList robotList;
	private final Bitmap robotFail;
	private final NinePatch borderSucceed, borderFail;
	private final int textColorSucceed, textColorFail;

	private final float fontHeight;
	private final int viewWidth, viewHeight;

	private final int xRobot;

	private State currentState;

	public ViewToastEx(Context context, FloatWindow parent, Rect rect, boolean toLeft, ToastEx.Strategy strategy , boolean isNormalNetState) {
		super(context);
		this.parent = parent;
		this.strategy = strategy;
		this.myHandler = new MyHandler(this);
		this.viewWidth = rect.width();
		this.viewHeight = rect.height();
		//
		this.robotList = new RobotList(context,isNormalNetState);
		this.paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
		int	textSizeRes = isNormalNetState ? R.dimen.toast_ex_text_size : R.dimen.toast_ex_abnormal_text_size;	 
		this.paintText.setTextSize(context.getResources().getDimensionPixelSize(textSizeRes)); //   MetricsUtils.sp2px(context, 14));
		FontMetrics fm = this.paintText.getFontMetrics();
		this.fontHeight = fm.bottom - fm.top;
		//
		if (toLeft) {
			this.xRobot = viewWidth - robotList.get(0).getWidth();
		} else {
			this.xRobot = 0;
		}
		//
		Resources res = context.getResources();
		this.borderSucceed = createNinePatch(res, R.drawable.toast_frame_success);
		this.textColorSucceed = res.getColor(R.color.color_game_7);
		this.textColorFail = res.getColor(R.color.color_game_7);
		
		if(isNormalNetState){
			this.robotFail = FloatWindow.commonUIRes.getBitmap(context, R.drawable.suspension_useless_robot);
			this.borderFail = createNinePatch(res, R.drawable.toast_frame_fail);
			changeCurrentState(new StateInflate(ToastEx.Effect.SUCCEED, true));
		}else{
			this.robotFail = FloatWindow.commonUIRes.getBitmap(context, R.drawable.floating_window_abnormal_state_prompt_huge);
		    this.borderFail = createNinePatch(res, R.drawable.toast_frame_red);
		    changeCurrentState(new StateInflate(ToastEx.Effect.FAIL, true));
		}
	}

	private static NinePatch createNinePatch(Resources res, int resId) {
		Bitmap bmp = BitmapFactory.decodeResource(res, resId);
		return UIUtils.createNinePatch(bmp);
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		return this.viewWidth;
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return this.viewHeight;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		currentState.draw(canvas);
		myHandler.removeMessages(MyHandler.MSG_REDRAW);
		myHandler.sendEmptyMessageDelayed(MyHandler.MSG_REDRAW, 50);
	}

	/**
	 * 切换到另一状态
	 * 
	 * @param newState
	 */
	private void changeCurrentState(State newState) {
		currentState = newState;
		currentState.init();
		myHandler.removeMessages(MyHandler.MSG_REDRAW);
		myHandler.sendEmptyMessageDelayed(MyHandler.MSG_REDRAW, 0);
	}

	/**
	 * 在指定的位置绘制边框和机器人
	 * 
	 * @param canvas
	 *            画布
	 * @param border
	 *            边框
	 * @param robot
	 *            机器人
	 * @param rect
	 *            边框矩形
	 */
	private void drawBorderAndRobot(Canvas canvas, NinePatch border, Bitmap robot, Rect rect) {
		border.draw(canvas, rect, paintNormal);
		canvas.drawBitmap(robot, this.xRobot, 0, paintNormal);
	}
}
