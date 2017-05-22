package cn.wsds.gamemaster.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import cn.wsds.gamemaster.R;

public class StartPage extends View {
	
	private Drawable imgBackground, imgTitle;
	private int widthImgBackground, heightImgBackground;
	private int widthImgTitle, heightImgTitle;

	public StartPage(Context context) {
		super(context);
		init(context);
	}

	public StartPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	@SuppressWarnings("deprecation")
	private void init(Context context) {
		Resources res = context.getResources();
		this.imgBackground = res.getDrawable(R.drawable.startup_homepage_pic);
		this.imgTitle = res.getDrawable(R.drawable.startup_homepage_title);
		this.widthImgBackground = this.imgBackground.getIntrinsicWidth();
		this.heightImgBackground = this.imgBackground.getIntrinsicHeight();
		this.widthImgTitle = this.imgTitle.getIntrinsicWidth();
		this.heightImgTitle = this.imgTitle.getIntrinsicHeight();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		//
		// The background
		canvas.drawColor(0xffffffff, Mode.SRC);
		int heightImgBackgroundDraw = width * heightImgBackground / widthImgBackground;
		imgBackground.setBounds(0, 0, width, heightImgBackgroundDraw);
		imgBackground.draw(canvas);
		//
		// The title
		int w = width * 633 / 1000;
		int h = w * heightImgTitle / widthImgTitle;
		int x = (width - w) / 2;
		int y = heightImgBackgroundDraw + (height - heightImgBackgroundDraw - h) / 2;
		imgTitle.setBounds(x, y, x + w, y + h);
		imgTitle.draw(canvas);
	}

}
