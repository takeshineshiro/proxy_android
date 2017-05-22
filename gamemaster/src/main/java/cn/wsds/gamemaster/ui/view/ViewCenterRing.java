package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

import com.subao.resutils.WeakReferenceHandler;



public class ViewCenterRing extends View {

	private static final int RESTORE = 0;
	private static final int ROTATE_SIXRECT = 1;
	private static final int ROTATE_COMPLETE_LINE = 2;
	private static final int ROTATE_LINE = 3;
	private static final int ROTATE_DOT = 4;
	private static final int ROTATE_COMPLETE_LINE_REVERSE = 5;
	private static final int ROTATE_LINE_REVERSE = 6;
	private static final int ROCKET_LONG = 7;
	private static final int ROCKET_SHORT = 8;
	
	private static final int ROTATE_TIME = 30;

	
	private Bitmap bitmapOutterDot;
	private Bitmap bitmapOutterLine;
	private Bitmap bitmapOutterCompleteLine;
	private Bitmap bitmapInnerRing;
	private Bitmap bitmapInnerSixRect;
	private Bitmap bitmapRocketLong;
	private Bitmap bitmapRocketShort;
	private Matrix matrix;
	private Paint paint;
	private MyHandler myHandler;

	
	private int width;
	private int height;
	private boolean rotateInnerSixRect = false;
	private boolean rotateInnerRing = false;
	private boolean rotateOutterDot = false;
	private boolean rotateOutterLine = false;
	private boolean rotateOutterCompleteLine = false;
	private boolean beforeRotate = true;
	private boolean afterRotate = false;
	private boolean rocketShort = true;
	
	private int degree = 0;
	private int count = 0;
	
	public ViewCenterRing(Context context, AttributeSet attrs) {
		super(context, attrs);
		Resources resources = context.getResources();
		bitmapOutterDot = BitmapFactory.decodeResource(resources, R.drawable.open_the_acceleration_outside_02);
		bitmapOutterLine = BitmapFactory.decodeResource(resources, R.drawable.open_the_acceleration_outside_01);
		bitmapOutterCompleteLine = BitmapFactory.decodeResource(resources, R.drawable.open_the_acceleration_outside);
		bitmapInnerRing = BitmapFactory.decodeResource(resources, R.drawable.open_the_acceleration_inside_point);
		bitmapInnerSixRect = BitmapFactory.decodeResource(resources, R.drawable.open_the_acceleration_inside);
		bitmapRocketLong = BitmapFactory.decodeResource(resources, R.drawable.open_ring_android_long);
		bitmapRocketShort = BitmapFactory.decodeResource(resources, R.drawable.open_ring_android_short);
		matrix = new Matrix();
		myHandler = new MyHandler(this);
		width = bitmapOutterDot.getWidth();
		height = bitmapOutterDot.getHeight();
	}
	
	@Override 
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (rocketShort){
			canvas.drawBitmap(bitmapRocketShort, 0, 0, paint);
		}
		else{
			canvas.drawBitmap(bitmapRocketLong, 0, 0, paint);
		}
		
		if (beforeRotate){
			canvas.drawBitmap(bitmapOutterCompleteLine, 0, 0, paint);
			canvas.drawBitmap(bitmapInnerSixRect, 0, 0, paint);
		}
		
		if (afterRotate){
			canvas.drawBitmap(bitmapOutterCompleteLine, 0, 0, paint);
			canvas.drawBitmap(bitmapInnerRing, 0, 0, paint);
		}
		
		if (rotateInnerSixRect){
			canvas.drawBitmap(bitmapOutterCompleteLine, 0, 0, paint);
			canvas.drawBitmap(bitmapInnerSixRect, matrix, paint);
		}
		if (rotateInnerRing){
			canvas.drawBitmap(bitmapInnerRing, matrix, paint);
		}
		
		if (rotateOutterDot){
			canvas.drawBitmap(bitmapOutterDot, matrix, paint);
		}
		
		if (rotateOutterLine){
			canvas.drawBitmap(bitmapOutterLine, matrix, paint);
		}
		
		if (rotateOutterCompleteLine){
			canvas.drawBitmap(bitmapOutterCompleteLine, matrix, paint);
		}
	}
	
	
	
	
	private static class MyHandler extends WeakReferenceHandler<ViewCenterRing>{
		
		MyHandler(ViewCenterRing ref){
			super(ref);
		}
		private void rotateMatrix(ViewCenterRing ref) {
			ref.degree += 10;
			ref.matrix.setRotate(ref.degree, ref.width / 2, ref.height / 2);
			ref.invalidate();
		}
		@Override
		protected void handleMessage(ViewCenterRing ref, Message msg) {
			switch (msg.what) {
			case ROTATE_SIXRECT:
				ref.beforeRotate = false;
				ref.rotateInnerSixRect = true;
				if(ref.degree < 360){
					ref.degree += 30;
					ref.matrix.setRotate(ref.degree, ref.width / 2, ref.height / 2);
					ref.invalidate();
					ref.myHandler.sendEmptyMessageDelayed(ROTATE_SIXRECT, 100);
				}
				else{
					ref.matrix.reset();
					ref.rotateInnerSixRect = false;
					ref.degree = 0;
					ref.myHandler.sendEmptyMessage(ROTATE_COMPLETE_LINE);
				}
				break;
			case ROTATE_COMPLETE_LINE:
				ref.rotateInnerRing = true;
				ref.rotateOutterCompleteLine = true;
				if (ref.degree < 120){
					rotateMatrix(ref);
					ref.myHandler.sendEmptyMessageDelayed(ROTATE_COMPLETE_LINE, ROTATE_TIME);
				}
				else{
					ref.rotateOutterCompleteLine = false;
					ref.myHandler.sendEmptyMessage(ROTATE_LINE);
				}
				break;
			case ROTATE_LINE:
				ref.rotateOutterLine = true;
				if (ref.degree < 240){
					rotateMatrix(ref);
					ref.myHandler.sendEmptyMessageDelayed(ROTATE_LINE, ROTATE_TIME);
				}
				else {
					ref.rotateOutterLine = false;
					ref.myHandler.sendEmptyMessage(ROTATE_DOT);
				}
				break;
			case ROTATE_DOT:
				ref.rotateOutterDot = true;
				if (ref.degree < 480){
					rotateMatrix(ref);
					ref.myHandler.sendEmptyMessageDelayed(ROTATE_DOT, ROTATE_TIME);
				}
				else {
					ref.rotateOutterDot = false;
					ref.myHandler.sendEmptyMessage(ROTATE_LINE_REVERSE);
				}
				break;
			case ROTATE_LINE_REVERSE:
				ref.rotateOutterLine = true;
				if (ref.degree < 600){
					rotateMatrix(ref);
					ref.myHandler.sendEmptyMessageDelayed(ROTATE_LINE_REVERSE, ROTATE_TIME);
				}
				else{
					ref.rotateOutterLine = false;
					ref.myHandler.sendEmptyMessage(ROTATE_COMPLETE_LINE_REVERSE);
				}
				break;
			case ROTATE_COMPLETE_LINE_REVERSE:
				ref.rotateOutterCompleteLine = true;
				if (ref.degree < 720){
					rotateMatrix(ref);
					ref.myHandler.sendEmptyMessageDelayed(ROTATE_COMPLETE_LINE_REVERSE, ROTATE_TIME);
				}
				else{
					ref.rotateOutterCompleteLine = false;
					ref.rotateInnerRing = false;
					ref.degree = 0;
					ref.afterRotate = true;
					ref.myHandler.sendEmptyMessageDelayed(ROCKET_LONG, 170);
				}
				break;
			case ROCKET_LONG:
				ref.rocketShort = false;
				ref.invalidate();
				ref.myHandler.sendEmptyMessageDelayed(ROCKET_SHORT, 200);
				break;
			case ROCKET_SHORT:
				ref.rocketShort = true;
				ref.invalidate();
				if (ref.count < 3){
					ref.count ++;
					ref.myHandler.sendEmptyMessageDelayed(ROCKET_LONG, 200);
				}
				else{
					ref.count = 0;
				}
				break;
			case RESTORE:
				ref.invalidate();
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
	
	public void rotate(){
		myHandler.sendEmptyMessage(ROTATE_SIXRECT);
	}
	
	public void vpnOnState(){
		beforeRotate = false;
		afterRotate = true;
		matrix.reset();
		myHandler.sendEmptyMessage(RESTORE);
	}
	
	public void interruptAndInit(){
		myHandler.removeCallbacksAndMessages(null);
		beforeRotate = true;
		afterRotate = false;
		rotateInnerSixRect = false;
		rotateInnerRing = false;
		rotateOutterDot = false;
		rotateOutterLine = false;
		rotateOutterCompleteLine = false;
		rocketShort = true;
		matrix.reset();
		myHandler.sendEmptyMessage(RESTORE);
	}
}
