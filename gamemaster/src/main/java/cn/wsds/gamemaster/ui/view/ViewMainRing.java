package cn.wsds.gamemaster.ui.view;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

import com.subao.resutils.WeakReferenceHandler;

public class ViewMainRing extends View {
	
	private final List<Bitmap> bitmapList = new ArrayList<Bitmap>(16); 
	
	private Bitmap outterNotOpen, outterOpen;//外圈
	private Bitmap middle00, /* middle01, */ middle02, middle03, middle04, middleNotOpen;//中圈
	private Bitmap innerNotOpen, innerOpen;//内圈
	private Bitmap rocketGrey, rocketShort, rocketLong;//内部火箭
	
	private Paint paintCommon, paintSweep;
	private RectF rectF;
	private MyHandler myHandler = new MyHandler(this);
	private long startTime = -1;
	private final int width, height;
	
	public ViewMainRing(Context context) {
		this(context, null);
	}
	
	public ViewMainRing(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBitmap(context.getResources());
		width = outterNotOpen.getWidth();
		height = outterNotOpen.getHeight();
		Shader shader = new BitmapShader(middle00, TileMode.CLAMP, TileMode.CLAMP);
		paintSweep = new Paint();
		paintSweep.setShader(shader);
		rectF = new RectF(0, 0, middle00.getWidth(), middle00.getHeight());
		
		paintCommon = new Paint();
		
	}
	
	private Bitmap loadBitmap(Resources res, int resId) {
		Bitmap bmp = BitmapFactory.decodeResource(res, resId);
		if (bmp != null) {
			bitmapList.add(bmp);
		}
		return bmp;
	}
	
	private void initBitmap(Resources res){
		
		outterNotOpen = loadBitmap(res, R.drawable.not_open_the_acceleration_outside);
		outterOpen = loadBitmap(res, R.drawable.open_the_acceleration_outside);
		
		middle00 = loadBitmap(res, R.drawable.open_the_acceleration_middle_00);
//		middle01 = loadBitmap(res, R.drawable.open_the_acceleration_middle_01);
		middle02 = loadBitmap(res, R.drawable.open_the_acceleration_middle_02);
		middle03 = loadBitmap(res, R.drawable.open_the_acceleration_middle_03);
		middle04 = loadBitmap(res, R.drawable.open_the_acceleration_middle_04);
		middleNotOpen = loadBitmap(res, R.drawable.not_open_the_acceleration_middle_01);
		
		innerNotOpen = loadBitmap(res, R.drawable.not_open_the_acceleration_inside_point);
		innerOpen = loadBitmap(res, R.drawable.open_the_acceleration_inside_point);
		
		rocketGrey = loadBitmap(res, R.drawable.not_open_ring_android);
		rocketShort = loadBitmap(res, R.drawable.open_ring_android_short);
		rocketLong = loadBitmap(res, R.drawable.open_ring_android_long);
		
	}
	
	private void initDraw(Canvas canvas){
		canvas.drawBitmap(outterNotOpen, 0, 0, paintCommon);
		canvas.drawBitmap(middleNotOpen, 0, 0, paintCommon);
		canvas.drawBitmap(innerNotOpen, 0, 0, paintCommon);
		canvas.drawBitmap(rocketGrey, 0, 0, paintCommon);
	}
	
	private void drawSweep(Canvas canvas, float sweepAngle){
		canvas.drawArc(rectF, 270, sweepAngle, true, paintSweep);
	}
	
	private static final long TIME_OF_STEP = 50;
	public static final long TIME_SHOW_STATIC_ROCKET = 1000;
	public static final long TIME_ROTATE_INNER = TIME_SHOW_STATIC_ROCKET + 1200;
	public static final long TIME_ROTATE_MIDDLE00 = TIME_ROTATE_INNER + 1200;
	private static final long TIME_ROTATE_MIDDLE01 = TIME_ROTATE_MIDDLE00 + 1200;
	private static final long TIME_ROTATE_MIDDLE02 = TIME_ROTATE_MIDDLE01 + 600;
	private static final long TIME_ROTATE_MIDDLE03 = TIME_ROTATE_MIDDLE02 + 600;
	private static final long TIME_ROTATE_MIDDLE04 = TIME_ROTATE_MIDDLE03 + 600;
	public static final long TIME_SWITCH_LENGTH = TIME_ROTATE_MIDDLE04 + 800;
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (startTime < 0){
			initDraw(canvas);
			return;
		}
		long elapsedTime = SystemClock.elapsedRealtime() - startTime;
		if (elapsedTime < TIME_SHOW_STATIC_ROCKET){
			canvas.drawBitmap(outterNotOpen, 0, 0, paintCommon);
			canvas.drawBitmap(middleNotOpen, 0, 0, paintCommon);
			canvas.drawBitmap(innerNotOpen, 0, 0, paintCommon);
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		} else if (elapsedTime >= TIME_SHOW_STATIC_ROCKET && elapsedTime < TIME_ROTATE_INNER){
			canvas.drawBitmap(outterNotOpen, 0, 0, paintCommon);
			canvas.drawBitmap(middleNotOpen, 0, 0, paintCommon);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate((elapsedTime - TIME_SHOW_STATIC_ROCKET) * 0.3f, width * 0.5f, width * 0.5f);
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			canvas.restore();
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		} else if (elapsedTime >= TIME_ROTATE_INNER && elapsedTime < TIME_ROTATE_MIDDLE00){
			canvas.drawBitmap(outterNotOpen, 0, 0, paintCommon);
			canvas.drawBitmap(middleNotOpen, 0, 0, paintCommon);
			drawSweep(canvas, (elapsedTime - TIME_ROTATE_INNER) * 0.3f);
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		} else if (elapsedTime >= TIME_ROTATE_MIDDLE00 && elapsedTime < TIME_ROTATE_MIDDLE01){
			canvas.drawBitmap(outterOpen, 0, 0, paintCommon);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate((elapsedTime - TIME_ROTATE_MIDDLE00) * 0.3f, width * 0.5f, width * 0.5f);
			canvas.drawBitmap(middle00, 0, 0, paintCommon);
			canvas.restore();
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		}  else if (elapsedTime >= TIME_ROTATE_MIDDLE01 && elapsedTime < TIME_ROTATE_MIDDLE02){
			canvas.drawBitmap(outterOpen, 0, 0, paintCommon);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate((elapsedTime - TIME_ROTATE_MIDDLE00) * 0.3f, width * 0.5f, width * 0.5f);
			canvas.drawBitmap(middle02, 0, 0, paintCommon);
			canvas.restore();
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		} else if (elapsedTime >= TIME_ROTATE_MIDDLE02 && elapsedTime < TIME_ROTATE_MIDDLE03){
			canvas.drawBitmap(outterOpen, 0, 0, paintCommon);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate((elapsedTime - TIME_ROTATE_MIDDLE00) * 0.3f, width * 0.5f, width * 0.5f);
			canvas.drawBitmap(middle03, 0, 0, paintCommon);
			canvas.restore();
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		} else if (elapsedTime >= TIME_ROTATE_MIDDLE03 && elapsedTime < TIME_ROTATE_MIDDLE04){
			canvas.drawBitmap(outterOpen, 0, 0, paintCommon);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate((elapsedTime - TIME_ROTATE_MIDDLE00) * 0.3f, width * 0.5f, width * 0.5f);
			canvas.drawBitmap(middle04, 0, 0, paintCommon);
			canvas.restore();
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		} else if (elapsedTime >= TIME_ROTATE_MIDDLE04 && elapsedTime < TIME_SWITCH_LENGTH){
			canvas.drawBitmap(outterOpen, 0, 0, paintCommon);
			canvas.drawBitmap(middle04, 0, 0, paintCommon);
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			if ((elapsedTime - TIME_ROTATE_MIDDLE04) / 100 % 2 == 0){
				canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
			} else {
				canvas.drawBitmap(rocketShort, 0, 0, paintCommon);
			}
		} else {
			canvas.drawBitmap(outterOpen, 0, 0, paintCommon);
			canvas.drawBitmap(middle04, 0, 0, paintCommon);
			canvas.drawBitmap(innerOpen, 0, 0, paintCommon);
			canvas.drawBitmap(rocketLong, 0, 0, paintCommon);
		}
	}
	
	public void startAnimation(){
		startTime = SystemClock.elapsedRealtime();
		myHandler.removeCallbacksAndMessages(null);
		myHandler.sendEmptyMessage(MyHandler.MSG_INVALIDATE);
	}
	
	public void interrupt(){
		startTime = -1;
		myHandler.removeCallbacksAndMessages(null);
		invalidate();
	}
	
	private static class MyHandler extends WeakReferenceHandler<ViewMainRing>{
		private static final int MSG_INVALIDATE = 0;
		private static final int MSG_COMPLETE = 1;

		public MyHandler(ViewMainRing ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(ViewMainRing ref, Message msg) {
			switch (msg.what) {
			case MSG_INVALIDATE:
				if (SystemClock.elapsedRealtime()- ref.startTime < TIME_SWITCH_LENGTH){
					ref.invalidate();
					sendEmptyMessageDelayed(MSG_INVALIDATE, TIME_OF_STEP);
				} else {
					sendEmptyMessage(MSG_COMPLETE);
				}
				break;
			case MSG_COMPLETE:
				ref.startTime = -1;
				break;
			default:
				break;
			}
		}
		
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		this.setMeasuredDimension(width, height);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		myHandler.removeCallbacksAndMessages(null);
		super.onDetachedFromWindow();
		for (Bitmap bmp : bitmapList) {
			bmp.recycle();
		}
		bitmapList.clear();
	}

}
