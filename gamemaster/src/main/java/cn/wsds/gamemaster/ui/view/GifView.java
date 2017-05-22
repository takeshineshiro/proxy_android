package cn.wsds.gamemaster.ui.view;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import cn.wsds.gamemaster.R;

/**
 * 显示Gif
 * 
 * @author YHB 2016.3.29.
 */
public class GifView extends SubaoAni {

	private int gifWidth, gifHeight;
	private boolean gifLoop;
	private Movie movie;
	private Drawable gifBackground;

	public GifView(Context context) {
		super(context);
	}

	public GifView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GifView);
		try {
			this.setGifWidth(ta.getInt(R.styleable.GifView_gifWidth, 0));
			this.setGifHeight(ta.getInt(R.styleable.GifView_gifHeight, 0));
			this.setGifLoop(ta.getBoolean(R.styleable.GifView_gifLoop, false));
			this.setGifBackground(ta.getDrawable(R.styleable.GifView_gifBackground));
			this.setScaleType(ScaleType.fromValue(ta.getInt(R.styleable.GifView_aniScaleType, 0)));
			//
			int resId = ta.getResourceId(R.styleable.GifView_gifSrc, 0);
			if (resId > 0) {
				this.setGifSrc(resId);
			} else {
				this.setGifAsset(ta.getString(R.styleable.GifView_gifAsset));
			}
		} finally {
			ta.recycle();
		}
	}

	private void setGifBackground(Drawable drawable) {
		this.gifBackground = drawable;
	}

	public int getGifWidth() {
		return gifWidth;
	}

	public void setGifWidth(int gifWidth) {
		this.gifWidth = Math.max(gifWidth, 0);
	}

	public int getGifHeight() {
		return gifHeight;
	}

	public void setGifHeight(int gifHeight) {
		this.gifHeight = Math.max(gifHeight, 0);
	}

	public boolean isGifLoop() {
		return gifLoop;
	}

	public void setGifLoop(boolean gifLoop) {
		this.gifLoop = gifLoop;
	}

	public void setGifSrc(int resId) {
		if (resId > 0) {
			this.movie = Movie.decodeStream(this.getResources().openRawResource(resId));
		} else {
			this.movie = null;
		}
	}

	public void setGifAsset(String assetName) {
		if (!TextUtils.isEmpty(assetName)) {
			AssetManager am = this.getContext().getAssets();
			try {
				InputStream input = am.open(assetName);
				try {
					this.movie = Movie.decodeStream(input);
				} finally {
					try {
						input.close();
					} catch (IOException e) {}
				}
				return;
			} catch (IOException e) {}
		}
		this.movie = null;
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		return this.gifWidth;
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		return this.gifHeight;
	}

	@Override
	protected void drawBackground(Canvas canvas, int width, int height, long elapsedTime) {
		Drawable d = this.gifBackground;
		if (d != null) {
			d.setBounds(0, 0, width, height);
			d.draw(canvas);
		}
	}

	@Override
	protected void drawForeground(Canvas canvas, int width, int height, long elapsedTime) {
		if (movie == null) {
			return;
		}
		long movieDuration = movie.duration();
		long time;
		if (gifLoop) {
			time = (movieDuration == 0) ? 0 : (elapsedTime % movieDuration);
		} else {
			time = Math.min(elapsedTime, movieDuration);
		}
		boolean canvasChanged = doScale(canvas, gifWidth, gifHeight, width, height);
		movie.setTime((int) time);
		movie.draw(canvas, 0, 0);
		if (canvasChanged) {
			canvas.restore();
		}
	}

	@Override
	protected void beforeFrame(int width, int height) {
		// do nothing
	}
}
