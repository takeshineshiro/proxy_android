package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import cn.wsds.gamemaster.R;

/**
 * 启动界面动画
 */
public class AppStartView extends SubaoAni {

	private static final long DEFAULT_ANI_TIME = 2000;
	private static final long MIN_ANI_TIME = 1000;

	private long aniTime;

	private final Drawable topTitle;	//标题
	private final Drawable background;  //底部背景
	private final Drawable nearPlanet;  //较远的星球
	private final Drawable farPlanet;	//较远的星球

	private final Point sizeBackground;
	private final Point sizeTopTitle;
	private final Point sizeNearPlanet, sizeFarPlanet;

	private final int colorBackground = 0xff010307;

	@SuppressWarnings("deprecation")
	public AppStartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Resources res = context.getResources();
		TypedArray ta = res.obtainAttributes(attrs, R.styleable.AppStartView);
		try {
			setAniTime(ta.getInt(R.styleable.AppStartView_aniTime, (int) DEFAULT_ANI_TIME));
			this.topTitle = res.getDrawable(R.drawable.startpage_top_title);
			this.background = res.getDrawable(R.drawable.startpage_bkgnd);
			this.farPlanet = res.getDrawable(R.drawable.startpage_far_planet);
			this.nearPlanet = res.getDrawable(R.drawable.startpage_near_planet);
			//
			sizeBackground = getDrawableIntrinsicSize(background, 10000);
			sizeTopTitle = getDrawableIntrinsicSize(topTitle, res.getInteger(R.integer.scale_app_start_view_logo));
			sizeNearPlanet = getDrawableIntrinsicSize(nearPlanet, res.getInteger(R.integer.scale_app_start_view_nearer_planet));
			sizeFarPlanet = getDrawableIntrinsicSize(farPlanet, res.getInteger(R.integer.scale_app_start_view_farther_planet));
		} finally {
			ta.recycle();
		}
	}

	private static Point getDrawableIntrinsicSize(Drawable d, int scale) {
		Point result = new Point();
		if (d != null) {
			result.x = d.getIntrinsicWidth() * scale / 10000;
			result.y = d.getIntrinsicHeight() * scale / 10000;
		}
		return result;
	}

	public void setAniTime(long millis) {
		this.aniTime = Math.max(MIN_ANI_TIME, millis);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		beforeFrame(w, h);
	}

	@Override
	protected void beforeFrame(int width, int height) {
		//		// 缩放相关
		//		float finalWidth = width * 0.75f;
		//		float finalHeight = height * 0.75f;
		//		endScale = Math.min(finalWidth / widthForeground, finalHeight / heightForeground);
		//		startScale = endScale * 0.8f;
		//		zoomSpeed = (endScale - startScale) / aniTime;
		//		// 背景图片按宽度填满整个画布，纵横比不变
		//		background.setBounds(0, 0, width,
		//			background.getIntrinsicHeight() * width / background.getIntrinsicWidth());
	}

	@Override
	protected void drawBackground(Canvas canvas, int width, int height, long elapsedTime) {
		canvas.drawColor(colorBackground);
		if (null != background) {
			int h = sizeBackground.y * width / sizeBackground.x;
			background.setBounds(0, 0, width, h);
			//
			float scaleUnit = 0.15f;
			int alpha;
			float scale;
			if (elapsedTime >= aniTime) {
				alpha = 255;
				scale = 1f + scaleUnit;
			} else {
				alpha = 128 + (int) (127 * elapsedTime / aniTime);
				scale = 1f + scaleUnit * elapsedTime / aniTime;
			}
			background.setAlpha(alpha);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.scale(scale, scale, width * 0.5f, height * 0.5f);
			background.draw(canvas);
			canvas.restore();
		}
	}

	@Override
	protected void drawForeground(Canvas canvas, int width, int height, long elapsedTime) {
		float r = elapsedTime / (float)aniTime;
		if (r > 1f) {
			r = 1f;
		}
		if (nearPlanet != null) {
			float xStart = -0.3f * sizeNearPlanet.x;
			float yStart = 0.5f * height;
			int xEnd = 0;
			float yEnd = yStart + sizeNearPlanet.y * 0.3f;
			drawPlanet(canvas, nearPlanet, sizeNearPlanet, r, xStart, yStart, xEnd, yEnd, 0.4f);
		}
		if (farPlanet != null) {
			float xStart = width - sizeFarPlanet.x * 1.2f;
			float xEnd = xStart - sizeFarPlanet.x * 0.3f;
			float yStart = height * 4f / 9;
			float yEnd = yStart - sizeFarPlanet.y * 0.55f;
			drawPlanet(canvas, farPlanet, sizeFarPlanet, r, xStart, yStart, xEnd, yEnd, -0.3f);
		}
		drawLogo(canvas, width, height);
	}
	
	private static void drawPlanet(Canvas canvas, Drawable planet, Point planetSize, float ratio, float xStart, float yStart, float xEnd, float yEnd, float scaleDelta) {
		float x = xStart + ratio * (xEnd - xStart);
		float y = yStart + ratio * (yEnd - yStart);
		//
		float scale = 1f + scaleDelta * ratio; 
		int w = Math.round(planetSize.x * scale);
		int h = Math.round(planetSize.y * scale);
		planet.setBounds(0, 0, w, h);
		//
		canvas.translate(x, y);
		planet.draw(canvas);
		canvas.translate(-x, -y);
	}

	private void drawLogo(Canvas canvas, int width, int height) {
		if (topTitle != null) {
			int x = (width - sizeTopTitle.x) / 2;
			int y = height / 6;
			topTitle.setBounds(0, 0, sizeTopTitle.x, sizeTopTitle.y);
			canvas.translate(x, y);
			topTitle.draw(canvas);
			canvas.translate(-x, -y);
		}
	}

}
