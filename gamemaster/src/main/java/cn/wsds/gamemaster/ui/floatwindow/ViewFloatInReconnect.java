package cn.wsds.gamemaster.ui.floatwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.os.Message;
import android.view.View;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.UIUtils;

import com.subao.resutils.WeakReferenceHandler;

/**
 * 断线重连悬浮窗视图
 * @author Administrator
 *
 */
@SuppressLint("ViewConstructor")
public class ViewFloatInReconnect extends View{
	
	private static final int TOTAL_TIME_MILLI = 20 * 1000 + 999; //20s
	private static final int CYCLE_TIME_SINGLE_POINT = 1000;
	private static final int CYCLE_TIME_MUTIL_POINT = 1000;
	private static final int CYCLE_TIME_SINGLE_LINE = 1000;
	private static final int CYCLE_TIME_MUTIL_LINE = 600;
	
	private static int TEXT_TIME_SIZE;			
	private static int TEXT_TIME_VALUE_SIZE;
	private static int SPACE_TIME_TEXT;
	private static int TEXT_DESC_SMALL_SIZE;
	private static int TEXT_DESC_NOMAL_SIZE;
	
	private static int VIEW_WIDTH;
	private static int VIEW_HEIGHT;
	private int remainTimeMillis;  //剩余时间
	private MyHanlder handler;
	private Status currentStatus;
	private boolean canCloseWindow;

	private Paint timePaint;    //时间画笔
	private Paint descPaint;    //普通文字画笔
	private Paint alphaPaint;   //透明度画笔
	private Paint bitmapPaint;  //普通图片画笔
	private Rect rect;
	private Rect dstRect;
	private Rect srcRect;
	private Context context;
	
	private NinePatch borderBitmap;
	private NinePatch failBorderBitmap;
	private Bitmap pointBitmap;
	private Bitmap cancelBitmap;
	private Bitmap lineBitmap;
	
	private int reconnCount = 1;
	private String reconnMessage = "网络异常，开始断线重连";  //提示消息
	
	/**
	 * 当前状态
	 */
	enum ConnStatus{
		NOMAL,	   //普通
		SUCCESS,   //成功
		FAIL,      //失败
	}
	
	private ConnStatus connStatus; 
	
	public ViewFloatInReconnect(Context context, String beginContent) {
		super(context);
		this.context = context;
		init();
		reconnMessage = beginContent;
		changeCurrentState(new OpenToastStatus(new SwitchInToastStatus(2000, TEXT_DESC_NOMAL_SIZE)));
	}
	
	//初始化资源
	private void init(){
		remainTimeMillis = TOTAL_TIME_MILLI;
		rect = new Rect();
		dstRect = new Rect();
		srcRect = new Rect();
		handler = new MyHanlder(this);
		
		initPaint();
		initBitmap();
		
		VIEW_WIDTH = context.getResources().getDimensionPixelSize(R.dimen.space_size_310);
		VIEW_HEIGHT = borderBitmap.getHeight();
		
		TEXT_TIME_SIZE = getDimenPix(R.dimen.text_size_24);			
		TEXT_TIME_VALUE_SIZE = getDimenPix(R.dimen.text_size_12);
		SPACE_TIME_TEXT = getDimenPix(R.dimen.space_size_70);
		TEXT_DESC_SMALL_SIZE = getDimenPix(R.dimen.text_size_12);
		TEXT_DESC_NOMAL_SIZE = getDimenPix(R.dimen.text_size_14);
	}

	//初始化图片资源
	private void initBitmap() {
		Resources res = context.getResources();
		borderBitmap = createNinePatch(res, R.drawable.toast_frame_success);
		failBorderBitmap = createNinePatch(res, R.drawable.toast_frame_fail);
		cancelBitmap = FloatWindow.commonUIRes.getBitmap(context, 
				R.drawable.suspension_reconnect_cancel_nor);
		//获取点进度画笔
		pointBitmap = FloatWindow.commonUIRes.getBitmap(context, 
				R.drawable.suspension_reconnect_progress_bar_point);
		//获取线进度画笔
		lineBitmap = FloatWindow.commonUIRes.getBitmap(context, 
				R.drawable.suspension_reconnect_progress_bar_line);
	}

	//初始化画笔
	private void initPaint() {
		timePaint = new Paint();
		descPaint = new Paint();
		alphaPaint = new Paint();
		bitmapPaint = new Paint();
		timePaint.setColor(getResources().getColor(R.color.color_game_8));
		timePaint.setAntiAlias(true);
		descPaint.setColor(getResources().getColor(android.R.color.white));
		descPaint.setTextAlign(Align.CENTER);
		descPaint.setAntiAlias(true);
		alphaPaint.setColor(getResources().getColor(android.R.color.white));
		alphaPaint.setTextAlign(Align.CENTER);
		alphaPaint.setTextSize(TEXT_DESC_NOMAL_SIZE);
		alphaPaint.setAntiAlias(true);
	}
	
	/**
	 * 改变当前状态
	 * @param status
	 */
	private void changeCurrentState(Status status){
		currentStatus = status;
		handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, 0);
	}
	
	/**
	 * 改变当前显示数据
	 * @param count
	 * @param successed
	 * @param type
	 */
	public void changeCurrentData(int count, boolean successed){
		if(successed){
			reconnMessage = "断线重连成功，游戏已恢复";
			connStatus = ConnStatus.SUCCESS;
			currentStatus.connSuccessOrFail();
		}
		if(count == 5 && !successed){
			reconnMessage = "断线重连失败";
			connStatus = ConnStatus.FAIL;
			currentStatus.connSuccessOrFail();
		}
		reconnCount = count;
		connStatus = ConnStatus.NOMAL;
	}
	
	/**
	 * 根据坐标判断时候可以关闭窗口
	 * @param touchX
	 * @param touchY
	 * @return
	 */
	public boolean canCloseWindow(int touchX, int touchY){
		if(canCloseWindow 
				&& touchX > VIEW_WIDTH - VIEW_HEIGHT 
				&& touchX < VIEW_WIDTH
				&& touchY > 0 
				&& touchY < VIEW_HEIGHT) return true;
		return false;
	}
	
	/**
	 * 获取.9图
	 * @param res
	 * @param resId
	 * @return
	 */
	private static NinePatch createNinePatch(Resources res, int resId) {
		Bitmap bmp = BitmapFactory.decodeResource(res, resId);
		return UIUtils.createNinePatch(bmp);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(VIEW_WIDTH, VIEW_HEIGHT);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		currentStatus.doDraw(canvas);
	}
	
	//////////////////////////////////////////////
	
	/**
	 * 动画状态
	 * @author Administrator
	 *
	 */
	private abstract class Status{
		public Status(){
			canCloseWindow = false;
		}
		
		/**
		 * 绘制界面方法
		 */
		abstract protected void doDraw(Canvas canvas);
		
		/**
		 * 成功或者失败
		 */
		protected void connSuccessOrFail(){
			changeCurrentState(new SwitchInToastStatus(2000, TEXT_DESC_NOMAL_SIZE, 
					new CloseToastStatus()));
		};
	}
	
	/**
	 * 抽象的动画逻辑类
	 * @author Administrator
	 *
	 */
	private abstract class DrawProgressStatus extends Status{
		
		protected int totalCount;
		protected int totalMillisecond;
		protected int currentCount = 1;
		int firstPintX = (VIEW_WIDTH - pointBitmap.getWidth()) / 2;
		int firstPintY = VIEW_HEIGHT / 2 + pointBitmap.getHeight();
		
		public DrawProgressStatus(int totalMill) {
			this(20, totalMill);
		}
		
		public DrawProgressStatus(int totalCount, int totalMilli){
			super();
			this.totalCount = totalCount;
			this.totalMillisecond = totalMilli;
		}
		
		@Override
		protected void doDraw(Canvas canvas) {
			if(remainTimeMillis < 0){
				remainTimeMillis = 0;
			}
			drawBorder(canvas);  //绘制背景
			drawCancel(canvas);  //绘制取消按钮
			drawTime(canvas, remainTimeMillis);  //绘制倒计时
			drawDesc(canvas, reconnCount);   //绘制描述文字
			int gap = (int)((float)pointBitmap.getWidth() / totalCount);
			draw(canvas, firstPintX, firstPintY, currentCount * gap);
			
			currentCount++;
			
			if(currentCount <= totalCount){
				int delay = updateAndDelay();
				remainTimeMillis -= delay;
			}else{
				cycleComplete();
			}
		}
		
		/**
		 * 一个动画周期完成
		 */
		protected abstract void cycleComplete();
		
		/**
		 * 更新并延迟
		 * @return 延迟时间
		 */
		protected abstract int updateAndDelay();
		
		/**
		 * 绘制（可以是圆点或者线条）
		 */
		protected abstract void draw(Canvas canvas, int bx, int by, int ex);
	}
	
	/**
	 * 绘制小圆点状态
	 * @author Administrator
	 *
	 */
	private abstract class DrawPointStatus extends DrawProgressStatus{
		
		public DrawPointStatus(int totalMill) {
			super(totalMill);
		}

		@Override
		protected void draw(Canvas canvas, int bx, int by, int dx) {
			drawPoint(canvas, bx, by, dx);
		}
	}
	
	/**
	 * 绘制线段点状态
	 * @author Administrator
	 *
	 */
	private abstract class DrawLineStatus extends DrawProgressStatus{
		
		public DrawLineStatus(int totalMill) {
			super(totalMill);
		}

		@Override
		protected void draw(Canvas canvas, int bx, int by, int dx) {
			drawPoint(canvas, bx, by, pointBitmap.getWidth());
			drawLine(canvas, bx, by, dx);
		}
	}
	
	/**
	 * 单个小圆点绘制
	 * @author Administrator
	 *
	 */
	private class SingleDrawPointStatus extends DrawPointStatus{
		private int delay;
		
		public SingleDrawPointStatus(){
			this(CYCLE_TIME_SINGLE_POINT);
		}
		
		public SingleDrawPointStatus(int milli){
			super(milli);
			delay = totalMillisecond / totalCount;
		}
		
		@Override
		protected int updateAndDelay() {
			handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, delay);
			return delay;
		}

		@Override
		protected void cycleComplete() {
			if(remainTimeMillis > TOTAL_TIME_MILLI * 3 / 4){
				currentCount = 0;
				updateAndDelay();
			}else{
				changeCurrentState(new MutilDrawPointStatus());
			}
		}
	}
	
	/**
	 * 多个小圆点绘制
	 * @author Administrator
	 *
	 */
	private class MutilDrawPointStatus extends DrawPointStatus{
		int delay;
		
		public MutilDrawPointStatus(){
			this(CYCLE_TIME_MUTIL_POINT);
		}
		
		public MutilDrawPointStatus(int milli){
			super(milli);
		}

		@Override
		protected int updateAndDelay() {
			delay = getDelayByCurrentCount(currentCount);
			handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, delay);
			return delay;
		}

		@Override
		protected void cycleComplete() {
			if(remainTimeMillis > TOTAL_TIME_MILLI * 2 / 4){
				currentCount = 0;
				updateAndDelay();
			}else{
				changeCurrentState(new SingleDrawLineStatus());
			}
		}
	}
	
	/**
	 * 更加当前数量获取延时
	 * @param currentCount
	 * @return
	 */
	private int getDelayByCurrentCount(int currentCount) {
		switch (currentCount) {
		case 2:
		case 5:
		case 9:
		case 14:
		case 20:
			return 100;
		default:
			return 0;
		}
	}
	
	/**
	 * 单个线段点绘制
	 * @author Administrator
	 *
	 */
	private class SingleDrawLineStatus extends DrawLineStatus{
		
		private int delay;
		
		public SingleDrawLineStatus(){
			this(CYCLE_TIME_SINGLE_LINE);
		}
		
		public SingleDrawLineStatus(int milli){
			super(milli);
			delay = totalMillisecond / totalCount;
		}
		
		@Override
		protected int updateAndDelay() {
			handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, delay);
			return delay;
		}

		@Override
		protected void cycleComplete() {
			if(remainTimeMillis > TOTAL_TIME_MILLI / 4){
				currentCount = 0;
				updateAndDelay();
			}else{
				changeCurrentState(new MutilDrawLineStatus());
			}
		}
	}
	
	/**
	 * 多个线段点绘制
	 * @author Administrator
	 *
	 */
	private class MutilDrawLineStatus extends DrawLineStatus{
		private int delay;
		private boolean isStop = false;
		
		public MutilDrawLineStatus(){
			this(CYCLE_TIME_MUTIL_LINE);
		}
		public MutilDrawLineStatus(int milli){
			super(milli);
		}

		@Override
		protected int updateAndDelay() {
			delay = getDelayByCurrentCount(currentCount);
			handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, delay);
			return delay;
		}

		@Override
		protected void cycleComplete() {
			if(!isStop){
				currentCount = 0;
				updateAndDelay();
			}
		}
		
		@Override
		protected void connSuccessOrFail() {
			isStop = true;
			handler.removeMessages(MyHanlder.MSG_REDRAW);
			super.connSuccessOrFail();
		}
	}
	
	/**
	 * 切换Toast进入
	 * @author Administrator
	 *
	 */
	private class SwitchInToastStatus extends Status{
		private int percent;
		private int alpha;
		private Status nextStatus;
		private int currentWait;
		private int wait;
		private String msg;
		
		public SwitchInToastStatus(int wait, int textPix){
			this(wait, textPix, null);
		}
		
		public SwitchInToastStatus(int wait, int textPix, Status nextStatus){
			this(wait, textPix, nextStatus, null);
		}
		
		public SwitchInToastStatus(int wait, int textPix, Status nextStatus, String msg){
			this.nextStatus = nextStatus;
			this.wait = wait;
			this.msg = msg;
			currentWait = 0;
			percent = 90;
			alpha = 40;
			alphaPaint.setTextSize(textPix);
		}
		
		@Override
		protected void doDraw(Canvas canvas) {
			drawBorder(canvas);
			if(percent > 60){
				alphaPaint.setAlpha(alpha);
				drawToast(canvas,  msg == null ? reconnMessage : msg, percent, alphaPaint);
				handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, 30);
				percent -= 5;
				currentWait += 30;
			}else if(currentWait < wait){
				alphaPaint.setAlpha(255);
				percent = 60;
				drawToast(canvas, msg == null ? reconnMessage : msg, percent, alphaPaint);
				handler.removeMessages(MyHanlder.MSG_REDRAW);
				handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, wait - currentWait);
				currentWait = wait;
			}else{
				if(nextStatus != null){
					changeCurrentState(nextStatus);
				}else{
					changeCurrentState(new SwitchOutToastStatus());
				}
			}
		}
	}
	
	/**
	 * 切换Toast出去
	 * @author Administrator
	 *
	 */
	private class SwitchOutToastStatus extends Status{
		private int percent;
		private int alpha;
		private Status nextStatus;
		public SwitchOutToastStatus(){
			this(null);
		}
		public SwitchOutToastStatus(Status nextStatus){
			this.nextStatus = nextStatus;
			percent = 60;
			alphaPaint.setTextSize(TEXT_DESC_NOMAL_SIZE);
			alpha = 120;
		}
		@Override
		protected void doDraw(Canvas canvas) {
			drawBorder(canvas);
			if(percent >= 40){
				alphaPaint.setAlpha(alpha);
				drawToast(canvas, reconnMessage, percent, alphaPaint);
				handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, 50);
				percent -= 5;
				alpha -= 20;
			}else{
				alphaPaint.setAlpha(0);
				percent = 40;
				drawToast(canvas, reconnMessage, percent, alphaPaint);
				if(nextStatus != null){
					changeCurrentState(nextStatus);
				}else{
					changeCurrentState(new SwitchInToastStatus(150, TEXT_DESC_SMALL_SIZE, 
							new SingleDrawPointStatus(), 
							String.format("正在尝试断线重连，第%d次", reconnCount)));
				}
			}
		}
	}
	
	/**
	 * 展开Toast
	 * @author Administrator
	 *
	 */
	private class OpenToastStatus extends BorderToastStatus{
		
		private Status nextStatus;
		
		public OpenToastStatus(Status nextStatus){
			this.nextStatus = nextStatus;
		}

		@Override
		public int getWidth(int cxCanvas) {
			super.getWidth(cxCanvas);
			if(width==0){
				width = borderBitmap.getWidth();
			}
			return width + delta;
		}
		
		@Override
		public void finish() {
			if(nextStatus != null){
				changeCurrentState(nextStatus);
			}
		}

	}
	
	/**
	 * 关闭Toast
	 * @author Administrator
	 *
	 */
	private class CloseToastStatus extends BorderToastStatus{

		@Override
		public int getWidth(int cxCanvas) {
			super.getWidth(cxCanvas);
			if(width==0){
				width = cxCanvas;
			}
			return width - delta;
		}

		@Override
		public void finish() {
			OnFinishListener l = onFinishListener;
			if (l != null) {
				l.onFinish(connStatus == ConnStatus.SUCCESS ? true : false);
			}
		}
	}
	
	/**
	 * 背景缩放的逻辑
	 * @author Administrator
	 *
	 */
	private abstract class BorderToastStatus extends Status{

		private final int LIVE_TIME = 300;
		public int width=0;
		protected int delta=0;
		private final int TOTAL_FRAME = 6;
		private final int FRAME_DURATION = LIVE_TIME/TOTAL_FRAME;
		private int currentFrame = 0;
		
		@Override
		protected void doDraw(Canvas canvas) {
			int cxCanvas = canvas.getWidth();
			if(++currentFrame>TOTAL_FRAME)
				return;
			handler.sendEmptyMessageDelayed(MyHanlder.MSG_REDRAW, FRAME_DURATION);
			width = getWidth(cxCanvas);
			drawBorder(canvas, width);
			if(currentFrame == TOTAL_FRAME){
				finish();
			}
		}
		
		public int delta(int cxCanvas){
			int delta = (cxCanvas - borderBitmap.getWidth()) * FRAME_DURATION / LIVE_TIME;
			return delta;
		}
		
		public abstract void finish();
		
		public int getWidth(int cxCanvas){
			if(delta==0){
				delta = delta(cxCanvas);
			}
			return width;
		}
	}
	
	////////////////////////////////////////////////
	
	/**
	 * 绘制进度小圆点
	 * @param canvas  
	 * 				画布
	 * @param bx
	 * 				起始x坐标
	 * @param by
	 * 				起始y坐标
	 * @param ex    
	 * 				x方向上的长度
	 */
	private void drawPoint(Canvas canvas, int bx, int by, int dx){
		srcRect.set(0, 0, dx, pointBitmap.getHeight());
		dstRect.set(bx, by, bx + dx, by + pointBitmap.getHeight());
		canvas.drawBitmap(pointBitmap, srcRect, dstRect, bitmapPaint);
	}
	
	/**
	 * 绘制进度线条
	 * @param canvas
	 * 				画布
	 * @param bx
	 * 				起始x坐标
	 * @param by
	 * 				起始y坐标
	 * @param dx
	 * 				x方向上的长度
	 */
	private void drawLine(Canvas canvas, int bx, int by, int dx){
		srcRect.set(0, 0, dx, lineBitmap.getHeight());
		dstRect.set(bx, by, bx + dx, by + lineBitmap.getHeight());
		canvas.drawBitmap(lineBitmap, srcRect, dstRect, bitmapPaint);
	}
	
	/**
	 * 绘制背景及边框
	 * @param canvas
	 */
	private void drawBorder(Canvas canvas){
		rect.set(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
		borderBitmap.draw(canvas, rect, bitmapPaint);
	}
	
	/**
	 * 绘制背景及边框
	 * @param canvas
	 */
	private void drawBorder(Canvas canvas, int width){
		int left = (VIEW_WIDTH-width)/2;
		drawBorder(canvas, left, left + width);
	}
	
	/**
	 * 绘制背景及边框
	 * @param canvas
	 */
	private void drawBorder(Canvas canvas, int left,int right){
		rect.set(left, 0, right, VIEW_HEIGHT);
		if(connStatus == ConnStatus.FAIL){
			failBorderBitmap.draw(canvas, rect, bitmapPaint);
		}else{
			borderBitmap.draw(canvas, rect, bitmapPaint);
		}
	}
	
	/**
	 * 绘制取消按钮
	 * @param canvas
	 */
	private void drawCancel(Canvas canvas){
		rect.set(VIEW_WIDTH - (VIEW_HEIGHT  + cancelBitmap.getWidth()) / 2, 
				(VIEW_HEIGHT - cancelBitmap.getHeight()) / 2, 
				VIEW_WIDTH - (VIEW_HEIGHT  - cancelBitmap.getWidth()) / 2, 
				(VIEW_HEIGHT  + cancelBitmap.getWidth()) / 2);
		canvas.drawBitmap(cancelBitmap, null, rect, bitmapPaint);
		canCloseWindow = true;
	}
	
	/**
	 * 绘制时间
	 * @param canvas
	 * 					画布
	 * @param timeMillis
	 * 					时间
	 */
	private void drawTime(Canvas canvas, long timeMillis){
		String timeMess = String.valueOf(timeMillis / 1000);
		String timeUnit = "s";

		rect.set(0, 0, SPACE_TIME_TEXT, VIEW_HEIGHT);
		// 测量 X 坐标
		timePaint.setTextSize(TEXT_TIME_VALUE_SIZE);
		float unitWidth = timePaint.measureText(timeUnit);
		timePaint.setTextSize(TEXT_TIME_SIZE);
		float messWidth = timePaint.measureText(timeMess);
		int x = (int) ((rect.right - messWidth - unitWidth)/2);
		FontMetricsInt fontMetrics = timePaint.getFontMetricsInt();  
		int y = rect.top + (rect.bottom - rect.top - 
				(fontMetrics.bottom - fontMetrics.top)) / 2 - fontMetrics.top;
		canvas.drawText(timeMess, x, y, timePaint);
		x += messWidth;
		timePaint.setTextSize(TEXT_TIME_VALUE_SIZE);
		canvas.drawText(timeUnit, x, y, timePaint);
	}

	/**
	 * 绘制描述文字
	 * @param canvas
	 * 				画笔
	 * @param count
	 * 				次数
	 */
	@SuppressLint("DefaultLocale")
	private void drawDesc(Canvas canvas, int count){
		String desc = String.format("正在尝试断线重连，第%d次", count);
		descPaint.setTextSize(TEXT_DESC_SMALL_SIZE);
		Rect targetRect = new Rect(0, 0, VIEW_WIDTH, VIEW_HEIGHT / 2);
		canvas.drawText(desc, targetRect.centerX(), targetRect.bottom, descPaint);
	}
	
	/**
	 * 绘制Toast文字
	 * @param canvas
	 * 
	 * @param content
	 * 
	 * @param x  
	 * 				文字的x坐标
	 * @param y  
	 * 				文字的y坐标
	 */
	private void drawToast(Canvas canvas, String content, int x, int y, Paint paint){
		descPaint.setTextSize(TEXT_DESC_NOMAL_SIZE);
		canvas.drawText(content, x, y, paint);
	}
	
	/**
	 * 绘制Toast文字
	 * @param canvas
	 * @param content
	 */
	/*private void drawToast(Canvas canvas, String content){
		Rect targetRect = new Rect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
		FontMetricsInt fontMetrics = timePaint.getFontMetricsInt();  
		int y = targetRect.top + 
				(targetRect.bottom - targetRect.top - 
						(fontMetrics.bottom - fontMetrics.top)) / 2 - fontMetrics.top;
		drawToast(canvas, content, targetRect.centerX(), y);
	}*/
	
	/**
	 * 绘制透明度变化的Toast文字，
	 * @param canvas
	 * 				画布
	 * @param yPercent
	 * 				y轴方向上的高度百分比
	 */
	private void drawToast(Canvas canvas, String content, int yPercent, Paint paint){
		Rect targetRect = new Rect(0, 0, VIEW_WIDTH, (int)((float)VIEW_HEIGHT * yPercent / 100));
		drawToast(canvas, content, targetRect.centerX(), targetRect.bottom, paint);
	}
	
	/**
	 * dip转px
	 * @param dip
	 * @return
	 */
/*	private int d2p(int dip){
		return MetricsUtils.dp2px(context, dip);
	}*/
	
	/**
	 * sp转px
	 * @param sp
	 * @return
	 */
/*	private int s2p(int sp){
		return MetricsUtils.dp2px(getContext(), sp);
	}*/
	
	/**
	 * 根据id获取尺寸
	 * @param dimenIdd
	 * @return
	 */
	private int getDimenPix(int dimenIdd){
		return context.getResources().getDimensionPixelSize(dimenIdd);
	}

	
	///////////////////////////////////////////////
	
	private static class MyHanlder extends WeakReferenceHandler<ViewFloatInReconnect>{
		public static final int MSG_REDRAW = 0x0001; //重绘

		public MyHanlder(ViewFloatInReconnect ref) {
			super(ref);
		}

		@Override
		protected void handleMessage(ViewFloatInReconnect ref, Message msg) {
			switch (msg.what) {
			case MSG_REDRAW:
				ref.invalidate();
				break;
			}
		}
	}
	
	interface OnFinishListener{
		/**
		 * 返回结果
		 * @param successed
		 */
		public void onFinish(boolean successed);
	}
	
	private OnFinishListener onFinishListener;
	
	/**
	 * 设置结果监听器
	 * @param onFinishListener
	 */
	public void setOnFinishListener(OnFinishListener l){
		this.onFinishListener = l;
	}
}
