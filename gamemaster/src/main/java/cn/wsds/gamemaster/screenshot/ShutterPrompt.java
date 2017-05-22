package cn.wsds.gamemaster.screenshot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindow;

import com.subao.utils.MetricsUtils;

/**
 * 截屏前动画提示
 */
public class ShutterPrompt extends FloatWindow{

	private static ShutterPrompt awaitPrompt;
	private ShutterPrompt(Context context) {
		super(context);
	}

	public static ShutterPrompt create() {
		Context context = AppMain.getContext();
		awaitPrompt = new ShutterPrompt(context);
		ShutterView view = new ShutterView(context);
		Point point = MetricsUtils.getDevicesSizeByPixels(context);
		awaitPrompt.addView(Type.DIALOG, view, 0, 0,point.x,point.y);
		return awaitPrompt;
	}
	
	
	protected static void destroyInstance() {
		if(awaitPrompt!=null){
			awaitPrompt.destroy();
			awaitPrompt = null;
		}
	}
	

	@Override
	protected void onViewAdded(View view) {
		Point point = MetricsUtils.getDevicesSizeByPixels(getContext());
		view.setLayoutParams(new LayoutParams(point.x,point.y));
	}

	@Override
	protected boolean canDrag() {
		return false;
	}
	
	/**
	 * 快门动画
	 */
	private static final class ShutterView extends View {
		
		private final Paint paint = new Paint();
		private int width;
		private int height;
		private Percent percent;
		
		public ShutterView(Context context) {
			super(context);
			init();
		}

		private void init() {
			paint.setColor(getContext().getResources().getColor(android.R.color.black));
			changePercent(new Shrink());
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			if(percent == null){
				return;
			}
			if(percent.isTimeOut()){
				percent.changeState();
			}
			if(percent == null){
				return;
			}
			postInvalidateDelayed(20);
			percent.draw(canvas);
		}
		
		@Override
		public void setLayoutParams(LayoutParams params) {
			super.setLayoutParams(params);
			this.width = params.width;
			this.height = params.height;
		}
		
		
		private abstract class Percent {
			private long beginTime;
			private final long maxTime;
			private Percent(long maxTime) {
				this.maxTime = maxTime;
			}
			protected long getElapsedTime(){
				if(beginTime == 0){
					beginTime = SystemClock.elapsedRealtime();
				}
				return SystemClock.elapsedRealtime() - beginTime;
			}
			public boolean isTimeOut(){
				return getElapsedTime() >= this.maxTime; 
			}
			public void draw(Canvas canvas){
				float topY = height * getTopYPercent();
				RectF topOval = new RectF(0, 0, width, topY );
				canvas.drawRect(topOval, paint);
				float bottomY = height - topY;
				RectF bottomOval = new RectF(0, bottomY, width, height);
				canvas.drawRect(bottomOval, paint);
			}
			public abstract void changeState();
			public abstract float getTopYPercent();
		}
		
		private final class Shrink extends Percent {
			/** 收缩时长 */
			private final static int TIME_SHRINK = 300;
			private Shrink() {
				super(TIME_SHRINK);
			}
			public float getTopYPercent(){
				return getElapsedTime() * 1.0f / TIME_SHRINK ;
			}
			@Override
			public void changeState() {
				changePercent(new Spread());
			}
		}
		private final class Spread extends Percent {
			/** 展开时长 */
			private final static int TIME_SPREAD = 300;
			private Spread() {
				super(TIME_SPREAD);
			}
			public float getTopYPercent(){
				return 1 - getElapsedTime() * 1.0f / TIME_SPREAD;
			}
			@Override
			public void changeState() {
				changePercent(null);
				finish();
			}
		}
		private void changePercent(Percent percent){
			this.percent = percent;
		}
		
		public void finish(){
			removeCallbacks(null);
			ShutterPrompt.destroyInstance();
		}
		
	}

}
