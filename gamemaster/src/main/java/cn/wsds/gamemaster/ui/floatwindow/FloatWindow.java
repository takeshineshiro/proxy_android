package cn.wsds.gamemaster.ui.floatwindow;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import cn.wsds.gamemaster.statistic.Statistic;

import com.subao.resutils.CommonUIResources;

/**
 * 所有悬浮窗（包括自定义的Toast）的抽象基类， 以及相关定义和方法
 */
public abstract class FloatWindow {

	/** 悬浮窗类型 */
	public static enum Type {
		/** TOAST，不接受点击。通常用于自定义Toast */
		TOAST,
		/** 不可拖拽的对话框。例如游戏中的大悬浮窗 */
		DIALOG,
		/** 可以拖拽的悬浮窗。例如游戏中的小悬浮窗 */
		DRAGGED,
	}

	/** FloatWindow被销毁时的侦听器 */
	public static interface OnDestroyListener {
		/**
		 * 当一个FlaotWindow销毁的时候调用
		 * 
		 * @param who
		 *            {@link FloatWindow}对象，是哪一个窗口被销毁
		 */
		void onFloatWindowDestroy(FloatWindow who);
	}

	/** 各个悬浮窗使用了共用的图片资源，为节省内存开销，这里使用{@link CommonUIResources}来共享这些资源 */
	public static final CommonUIResources commonUIRes = new CommonUIResources();

	private final Context context;
	
	protected final WindowManager windowManager;
	private final WindowManager.LayoutParams windowLayoutParams;

	/** {@link Window}所拥用的{@link View} */
	private View view;

	/** 大小尺寸 */
	private final Point size = new Point();

	/** {@link OnDestroyListener} 销毁事件侦听器 */
	private OnDestroyListener onDestroyListener;

	/**
	 * 将指定的整数值调整到给定的范围内
	 * 
	 * @param v
	 *            给定的整数值
	 * @param min
	 *            最小值，调整后的结果将不会小于该值
	 * @param max
	 *            最大值，调整后的结果将不大大于该值
	 * @return 如果给定v值小于min则返回min，如果给定v大于max则返回max，否则返回v值本身。
	 */
	protected static int clamp(int v, int min, int max) {
		if (v < min) {
			v = min;
		} else if (v > max) {
			v = max;
		}
		return v;
	}

	/**
	 * 可移动窗口所拥用的View的点击事件侦听器
	 */
	private final class OnViewTouchListenerForDragged implements View.OnTouchListener {

		private static final long MIN_DELTA_TIME_FOR_CLICK = 300;	// Down和UP之间的时差，不大于多少毫秒算Click
		private static final int DELTA_FOR_DRAG_BEGIN = 5;			// 首次拖动，像素差大于多少才触发首次拖动事件？
		private static final int MIN_DELAT_FRO_CLICK = 10;			// 按下抬起不超过多大距离才算点击

		private boolean touchDown;				// 是否按下
		private float xTouchLast, yTouchLast;	// 最近一次按下拖动的位置
		private int xTouchDownForScreen, yTouchDownForScreen;	// 最近一次Touch Down的屏幕位置
		private long touchDownTime;				// 按下时刻

		private boolean isDragBegin;	// 拖动是否开始？
		
		private final int maxDragDeltaX, maxDragDeltaY;
		
		public OnViewTouchListenerForDragged(View v) {
			this.maxDragDeltaX = v.getMeasuredWidth();
			this.maxDragDeltaY = v.getMeasuredHeight();
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touchDown = true;
				touchDownTime = SystemClock.elapsedRealtime();
				xTouchLast = event.getRawX();
				yTouchLast = event.getRawY();
				xTouchDownForScreen = FloatWindow.this.getX();
				yTouchDownForScreen = FloatWindow.this.getY();
				FloatWindow.this.onTouchDown();
				break;
			case MotionEvent.ACTION_UP:
				touchDown = false;
				if (isDragBegin) {
					isDragBegin = false;
					onDragEnd();
				}
				if (SystemClock.elapsedRealtime() - touchDownTime <= MIN_DELTA_TIME_FOR_CLICK) {
					int dx = xTouchDownForScreen - FloatWindow.this.getX();
					if (dx <= MIN_DELAT_FRO_CLICK && dx >= -MIN_DELAT_FRO_CLICK) {
						int dy = yTouchDownForScreen - FloatWindow.this.getY();
						if (dy <= MIN_DELAT_FRO_CLICK && dy >= -MIN_DELAT_FRO_CLICK) {
							v.performClick();
							FloatWindow.this.onClick((int)event.getRawX() - FloatWindow.this.getX(), (int)event.getRawY() - FloatWindow.this.getY());
						}
					}
				}
				FloatWindow.this.onTouchUp();
				break;
			case MotionEvent.ACTION_MOVE:
				onTouchMove(v, event);
				break;
			case MotionEvent.ACTION_CANCEL:
				touchDown = false;
//				FloatWindow.this.onTouchDown();
				FloatWindow.this.onTouchCancel();
				break;
			case MotionEvent.ACTION_OUTSIDE:
				FloatWindow.this.onTouchOutside();
				break;
			default:
				return false;
			}
			return true;
		}

		private void onTouchMove(View v, MotionEvent event) {
			if (!touchDown || !canDrag()) {
				return;
			}
			float xCurrent = event.getRawX();
			float yCurrent = event.getRawY();
			int xDelta = Math.round(xCurrent - xTouchLast);
			int yDelta = Math.round(yCurrent - yTouchLast);

			//
			if (xDelta >= maxDragDeltaX || xDelta <= -maxDragDeltaX || yDelta >= maxDragDeltaY || yDelta <= -maxDragDeltaY) {
				return;
			}
			xTouchLast = xCurrent;
			yTouchLast = yCurrent;
			//
			boolean changed;
			if (isDragBegin) {
				changed = (xDelta != 0 || yDelta != 0);
			} else {
				changed = (xDelta >= DELTA_FOR_DRAG_BEGIN || xDelta <= -DELTA_FOR_DRAG_BEGIN || yDelta >= DELTA_FOR_DRAG_BEGIN || yDelta <= -DELTA_FOR_DRAG_BEGIN);
			}
			if (changed) {
				if (!isDragBegin) {
					isDragBegin = true;
					onDragBegin();
				}
				setPosition(windowLayoutParams.x + xDelta, windowLayoutParams.y + yDelta);
			}
		}
	};

	/**
	 * 不可移动窗口所拥用的View的点击事件侦听器
	 */
	private final View.OnTouchListener onViewTouchListenerForDialog = new View.OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
				FloatWindow.this.onTouchOutside();
			}
			return true;
		}

	};

	protected FloatWindow(Context context) {
		this.context = context.getApplicationContext();
		this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
		this.windowLayoutParams = new WindowManager.LayoutParams();
	}

	/**
	 * 为悬浮窗口添加View
	 * 
	 * @param type
	 *            {@link Type} 哪种类型
	 * @param view
	 *            布局
	 * @param x
	 *            位置X
	 * @param y
	 *            位置Y
	 */
	public final void addView(Type type, View view, int x, int y) {
		addView(type, view, x, y, 0, 0);
	}
	
	/**
	 * 为悬浮窗口添加View
	 * 
	 * @param type
	 *            {@link Type} 哪种类型
	 * @param view
	 *            布局
	 * @param x
	 *            位置X
	 * @param y
	 *            位置Y
	 */
	public final void addView(Type type, View view, int x, int y,int width,int height) {
		// 因为设备众多、兼容性参差不齐，所以干脆把整个流程放在try里面
		try {
			doAddView(type, view, x, y, width, height);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	private void doAddView(Type type, View view, int x, int y, int width, int height) {
		removeView();
		//
		this.view = view;
		view.setLayoutParams(new ViewGroup.LayoutParams(0,0));
		view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		//
		windowLayoutParams.width = width != 0 ? width : view.getMeasuredWidth();
		windowLayoutParams.height = height != 0 ? height : view.getMeasuredHeight();
		switch (type) {
		case TOAST:
			windowLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
			break;
		case DIALOG:
			windowLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
			break;
		default:
			windowLayoutParams.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;	// 用这个值可以防止被DIALOG类的悬浮窗盖住
			break;
		}
		windowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL	// 将点击事件透给后面的窗口
			| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
			| WindowManager.LayoutParams.FLAG_FULLSCREEN
			| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
			| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS	// 这个标志防止：大悬浮窗展开时切换横竖屏，产生错位
		;
		windowLayoutParams.format = PixelFormat.RGBA_8888;
		windowLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		windowLayoutParams.x = x;
		windowLayoutParams.y = y;
		
		//
		try {
			windowManager.addView(view, windowLayoutParams);
		} catch (Exception ex) {
			this.view = null;
			Statistic.addEvent(this.context, Statistic.Event.BACKSTAGE_FLOATWINDOW_EXCEPTION_ADDVIEW, ex.getClass().getSimpleName());
			return;
		}
		onViewAdded(this.view);
		//
		switch (type) {
		case DRAGGED:
			view.setOnTouchListener(new OnViewTouchListenerForDragged(view));
			break;
		case DIALOG:
			view.setOnTouchListener(this.onViewTouchListenerForDialog);
			break;
		default:
			break;
		}
	}
	
	/**
	 * 用派生类实现，当View被添加到Window后调用
	 * 
	 * @param view
	 *            {@link View}
	 */
	protected abstract void onViewAdded(View view);

	/**
	 * 用派生类实现，决定当前是否可被拖动
	 * 
	 * @return true表示当前可以被拖动，false表示现在不能。
	 */
	protected abstract boolean canDrag();

	public void setOnDestroyListener(OnDestroyListener l) {
		this.onDestroyListener = l;
	}

	private void removeView() {
		if (this.view != null) {
			try {
				windowManager.removeView(this.view);
			} catch (Exception e) {
				Statistic.addEvent(this.context, Statistic.Event.BACKSTAGE_FLOATWINDOW_EXCEPTION_REMOVEVIEW, e.getClass().getSimpleName());
			}
			this.view = null;
		}
	}

	/**
	 * 销毁悬浮窗（从WindowManager里移除View）<br />
	 * 这个函数不要public被外部直接调用到，因为大多数派生类的实现用了单例
	 */
	protected void destroy() {
		if (this.view != null) {
			try {
				windowManager.removeView(this.view);
			} catch (Exception e) { }
			this.view = null;
			//
			OnDestroyListener l = this.onDestroyListener;
			if (l != null) {
				l.onFloatWindowDestroy(this);
			}
		}
	}

	/**
	 * 显示或隐藏。
	 * 
	 * @param visible
	 *            与 View.setVisibility()参数取值相同
	 * 
	 */
	public void setVisibility(int visibility) {
		if (this.view != null) {
			this.view.setVisibility(visibility);
		}
	}
	
	public int getVisibility(){
		if (this.view != null){
			return this.view.getVisibility();
		}
		return View.GONE;
	}

	/**
	 * 调整大小
	 * 
	 * @param width
	 *            宽度，像素
	 * @param height
	 *            高度，像素
	 */
	public void setSize(int width, int height) {
		if (view != null) {
			this.windowLayoutParams.width = width;
			this.windowLayoutParams.height = height;
			this.windowManager.updateViewLayout(view, windowLayoutParams);
		}
	}

	/**
	 * 设置本悬浮窗左上角的位置
	 * 
	 * @param x
	 *            以屏幕左上角为原点的横坐标（像素）
	 * @param y
	 *            以屏幕左上角为原点的纵坐标（像素）
	 */
	public void setPosition(int x, int y) {
		setPosition(x, y, false);
	}
	
	public void setPosition(int x, int y, boolean outOfBounds){
		if (view == null) {
			return;
		}
		windowManager.getDefaultDisplay().getSize(size);
		if (!outOfBounds){
			x = clamp(x, 0, size.x - this.getWidth());
			y = clamp(y, 0, size.y - this.getHeight());
		}
		if (x != windowLayoutParams.x || y != windowLayoutParams.y) {
			windowLayoutParams.x = x;
			windowLayoutParams.y = y;
			windowManager.updateViewLayout(view, windowLayoutParams);
			this.onPositionChange(x, y);
		}
	}

	/**
	 * 设置本悬浮窗中心点在屏幕的哪个位置
	 * 
	 * @param x
	 *            以屏幕左上角为原点的横坐标（像素）
	 * @param y
	 *            以屏幕左上角为原点的纵坐标（像素）
	 */
	public void setCenterPosition(int xCenter, int yCenter) {
		int x = xCenter - (this.getWidth() >> 1);
		int y = yCenter - (this.getHeight() >> 1);
		this.setPosition(x, y);
	}
	
	public final void reLayout(int x, int y, int width, int height) {
		if (view == null) {
			return;
		}
		if (width < 0) {
			width = this.getWidth();
		}
		if (height < 0) {
			height = this.getHeight();
		}
		windowManager.getDefaultDisplay().getSize(this.size);
		x = clamp(x, 0, size.x - width);
		y = clamp(y, 0, size.y - height);
		boolean isPosChanged = (x != windowLayoutParams.x || y != windowLayoutParams.y);
		if (isPosChanged || width != windowLayoutParams.width || height != windowLayoutParams.height) {
			windowLayoutParams.x = x;
			windowLayoutParams.y = y;
			windowLayoutParams.width = width;
			windowLayoutParams.height = height;
			windowManager.updateViewLayout(view, windowLayoutParams);
			if (isPosChanged) {
				this.onPositionChange(x, y);
			}
		}
	}

	/**
	 * 返回本悬浮窗左上角在屏幕的X位置（像素）
	 * 
	 * @return 本悬浮窗左上角在屏幕的X位置（像素）
	 */
	public int getX() {
		return windowLayoutParams.x;
	}

	/**
	 * 返回本悬浮窗中心点在屏幕的X位置（像素）
	 * 
	 * @return 本悬浮窗中心点左上角在屏幕的X位置（像素）
	 */
	public int getCenterX() {
		return getX() + (this.getWidth() >> 1);
	}

	/**
	 * 返回本悬浮窗左上角在屏幕的Y位置（像素）
	 * 
	 * @return 本悬浮窗左上角在屏幕的Y位置（像素）
	 */
	public int getY() {
		return windowLayoutParams.y;
	}

	/**
	 * 返回本悬浮窗中心点在屏幕的Y位置（像素）
	 * 
	 * @return 本悬浮窗中心点左上角在屏幕的Y位置（像素）
	 */
	public int getCenterY() {
		return this.getY() + (this.getHeight() >> 1);
	}

	/**
	 * 返回本悬浮窗的宽度（像素）
	 * 
	 * @return 本悬浮窗的宽度（像素）
	 */
	public int getWidth() {
		return view == null ? 0 : windowLayoutParams.width;
	}

	/**
	 * 返回本悬浮窗的高度（像素）
	 * 
	 * @return 本悬浮窗的高度（像素）
	 */
	public int getHeight() {
		return view == null ? 0 : windowLayoutParams.height;
	}

	/**
	 * 返回本悬浮窗的View
	 * 
	 * @return 本悬浮窗的View
	 */
	protected View getView() {
		return this.view;
	}

	/**
	 * 返回一个可用的Context
	 * 
	 * @return 如果View为空，返回Application的Context，否则返回View的Context
	 */
	protected final Context getContext() {
		return this.context;
	}

	/**
	 * 派生类可重写：当在悬浮窗矩形范围外点击时被调用
	 */
	protected void onTouchOutside() {

	}

	/**
	 * 派生类可重写：当Touch up时被调用
	 */
	protected void onTouchUp() {

	}

	/**
	 * 派生类可重写：当Touch down时被调用
	 */
	protected void onTouchDown() {

	}

	/**
	 * 派生类可重写：当Touch Cancel时被调用
	 */
	protected void onTouchCancel() {

	}
	
	protected void onClick(int x, int y) {
		
	}

	/**
	 * 派生类可重写：当拖动开始时被调用
	 */
	protected void onDragBegin() {

	}

	/**
	 * 派生类可重写：当拖动结束时被调用
	 */
	protected void onDragEnd() {

	}

	/**
	 * 派生类可重写：当位置发生改变时被调用
	 * 
	 * @param x
	 *            新位置的屏幕X坐标（像素）
	 * @param y
	 *            新位置的屏幕Y坐标（像素）
	 */
	protected void onPositionChange(int x, int y) {

	}
}
