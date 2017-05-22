package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.R.styleable;

public class ViewWaitForInit extends SurfaceView {

	private Drawer drawer;
	
	private final Rect rect = new Rect();
	private final Rect rectRing = new Rect();
	private float yText;

	private final Drawable background;
	private final Bitmap[] robotList;
	private final Bitmap bmpRing;
	private final Paint paintBitmap;
	private final Paint paintText;
	private final String text;

	public ViewWaitForInit(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, styleable.ViewWaitForInit);
		try {
			robotList = new Bitmap[2];
			robotList[0] = BitmapFactory.decodeResource(getResources(), R.drawable.loading_center_rocket_l);
			robotList[1] = BitmapFactory.decodeResource(getResources(), R.drawable.loading_center_rocket_s);
			bmpRing = BitmapFactory.decodeResource(getResources(), R.drawable.loading_edge_ring);
			paintBitmap = new Paint();
			background = ta.getDrawable(styleable.ViewWaitForInit_background);
			paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintText.setTextAlign(Paint.Align.CENTER);
			float textSize = ta.getDimensionPixelSize(styleable.ViewWaitForInit_textSize, -1);
			if (textSize < 0) {
				textSize = getResources().getDimensionPixelSize(R.dimen.text_size_12);
			}
			paintText.setTextSize(textSize);
			paintText.setColor(ta.getColor(styleable.ViewWaitForInit_textColor, getResources().getColor(R.color.color_game_6)));
			this.text = ta.getString(styleable.ViewWaitForInit_text);
		} finally {
			ta.recycle();
		}
		//
		if (!isInEditMode()) {
			this.setZOrderOnTop(true);
		}
	}

	@Override
	public void layout(int l, int t, int r, int b) {
		super.layout(l, t, r, b);
		if (drawer == null) {
			drawer = new Drawer();
			this.getHolder().addCallback(drawer);
			drawer.executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		recalcParams(w, h);
	}

	private void recalcParams(int w, int h) {
		this.rect.set(0, 0, w, h);
		if (this.background != null) {
			this.background.setBounds(rect);
		}
		//
		int x = (w - bmpRing.getWidth()) >> 1;
		int y = (h - bmpRing.getHeight()) >> 1;
		this.rectRing.set(x, y, x + bmpRing.getWidth(), y + bmpRing.getHeight());
		if (!TextUtils.isEmpty(text)) {
			FontMetrics fm = paintText.getFontMetrics();
			float textHeight = fm.bottom - fm.top; // 文字高度
			float space = getResources().getDimensionPixelSize(R.dimen.space_size_15); // 间距
			int cy = (int)(bmpRing.getHeight() + textHeight + space);
			// 将Ring的位置上移
			y = (h - cy) >> 1;
			rectRing.top = y;
			rectRing.bottom = y + bmpRing.getHeight();
			// 决定文本的Y位置
			yText = rectRing.bottom + space + textHeight;
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		this.stop();
		super.onDetachedFromWindow();
	}

	private static void sleep(long millisenconds) {
		try {
			Thread.sleep(millisenconds);
		} catch (InterruptedException e) {}
	}

	private class Drawer extends AsyncTask<Void, Integer, Void> implements SurfaceHolder.Callback {

		private static final long DRAW_WAIT = 50;

		private SurfaceHolder holder;

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			this.holder = holder;
			holder.setFormat(PixelFormat.RGBA_8888);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			this.holder = holder;
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			holder = null;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				doDraw();
			} catch (RuntimeException e) {}
			return null;
		}

		private void doDraw() {
			long beginTime = SystemClock.elapsedRealtime();
			while (!isCancelled()) {
				SurfaceHolder sh = this.holder;
				if (sh == null) {
					sleep(DRAW_WAIT);
					continue;
				}
				Canvas canvas = sh.lockCanvas();
				if (canvas == null) {
					sleep(DRAW_WAIT);
					continue;
				}
				//
				long now = SystemClock.elapsedRealtime();
				//					
				//绘制
				try {
					drawBackground(canvas);
					drawProgress(canvas, now - beginTime);
					drawText(canvas);
				} finally {
					sh.unlockCanvasAndPost(canvas);
				}
				//延迟
				sleep(DRAW_WAIT);
			}
		}

		private void drawText(Canvas canvas) {
			if (!TextUtils.isEmpty(text)) {
				canvas.drawText(text, rect.width() >> 1, yText, paintText);
			}
		}

		private void drawBackground(Canvas canvas) {
			if (background != null) {
				background.draw(canvas);
			}
		}

		private void drawProgress(Canvas canvas, long deltaTime) {
			// 转动外环
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			try {
				canvas.rotate((deltaTime >> 3),
					rectRing.left + rectRing.width() * 0.5f,
					rectRing.top + rectRing.height() * 0.5f);
				canvas.drawBitmap(bmpRing, null, rectRing, paintBitmap);
			} finally {
				canvas.restore();
			}
			// 画机器人
			int idx = (int)((deltaTime / 200) & 1);
			Bitmap robot = robotList[idx];
			canvas.drawBitmap(robot, null, rectRing, paintBitmap);
		}
	}

	public void stop() {
		if (drawer != null) {
			drawer.cancel(true);
			this.getHolder().removeCallback(drawer);
			drawer = null;
		}
	}

}
