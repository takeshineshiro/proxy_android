package cn.wsds.gamemaster.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.util.Random;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DateParams;
import cn.wsds.gamemaster.dialog.AchieveShareDialog;
import cn.wsds.gamemaster.statistic.Statistic;

/**
 * 用户使用游戏成就
 */
public class ActivityNewGamePlayAchieve extends ActivityShare{
	
//	/**
//	 * 界面被打开方式
//	 */
//	public enum OpenWay{
//		notification_bar,
//		main_page;
//	}

//	private OpenWay openWay;
//	private boolean clickShared;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		openWay = getOpenWay();
//		statisticOpen(openWay);
		ConfigManager.getInstance().setAlreadyOpenNoticeAccelAchieve();
//		StatisticDefault.addEvent(AppMain.getContext(),StatisticDefault.Event.NOTIFICATION_REPORT_READ);
		Statistic.addEvent(AppMain.getContext(),Statistic.Event.SHARE_ACHIEVEMENT_APPEAR);
		setContentView(R.layout.activity_new_game_play_achieve);
		initContent();
		initAchieve();
		initButtonView();
	}
	
//	@Override
//	protected void onDestroy() {
//		super.onDestroy();
//		if(!clickShared){
//			statisticClose(openWay);
//		}
//	}

//	private void statisticOpen(OpenWay openWay) {
//		Event event;
//		switch (openWay) {
//		case main_page:
//			event = Event.INTERFACE_SHARE_RESULT_POPUP;
//			break;
//		case notification_bar:
//		default:
//			event = Event.CLICK_NOTIFICATION_BAR_SHARE_RESULT;
//			break;
//		}
//		StatisticDefault.addEvent(getApplicationContext(), event);
//	}
//	
//	private void statisticClose(OpenWay openWay) {
//		Event event;
//		switch (openWay) {
//		case main_page:
//			event = Event.INTERFACE_SHARE_RESULT_CLOSE;
//			break;
//		case notification_bar:
//		default:
//			event = Event.NOTIFICATION_BAR_SHARE_RESULT_CLOSE;
//			break;
//		}
//		StatisticDefault.addEvent(getApplicationContext(), event);
//	}

//	private OpenWay getOpenWay() {
//		boolean openatMain = getIntent().getBooleanExtra(IntentExtraName.INTENT_EXTRANAME_AVHIEVE_OPENAT_MAIN, false);
//		return openatMain ? OpenWay.main_page :OpenWay.notification_bar;
//	}

	private void initContent() {
		TextView textTitle = (TextView) findViewById(R.id.text_content);
		textTitle.setText(ContentSpannableCreater.create());
	}
	
	private static final class ContentSpannableCreater {
		
		private static Spannable create(){
			ContentSpannableCreater creater = new ContentSpannableCreater();
			SpannableStringBuilder spannable = new SpannableStringBuilder();
			spannable.append("您已经是迅游忠实的小伙伴了\n");
			spannable.append(creater.createAccelSpannale());
			spannable.append("\n");
			/*spannable.append(creater.createStopSpannale());
			spannable.append("\n");*/
			spannable.append(creater.createReduceSpannale());
			return spannable;
		}

		private CharSequence createReduceSpannale() {
			SpannableStringBuilder spannable = new SpannableStringBuilder();
			spannable.append(" * 降低延迟");
			ImageSpan stopSpan = new MyImageSpan(AppMain.getContext(), R.drawable.achievement_to_share_reduce);
			spannable.setSpan(stopSpan, 1,2, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
			int percent = new Random().nextInt(20) + 50;
			String percentDesc = String.valueOf(percent);
			int start = spannable.length();
			int percentEnd = start+percentDesc.length();
			spannable.append(percentDesc);
			int color = AppMain.getContext().getResources().getColor(R.color.game_achieve_content_text_time_color);
			spannable.setSpan(new ForegroundColorSpan(color), start, percentEnd , Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			spannable.setSpan(new AbsoluteSizeSpan(30,true), start, percentEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			String percentUnit = "%";
			spannable.append(percentUnit);
			spannable.setSpan(new ForegroundColorSpan(color), percentEnd,percentEnd+percentUnit.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
			spannable.setSpan(new AbsoluteSizeSpan(15,true), percentEnd,percentEnd+percentUnit.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
			return spannable;
		}

		private CharSequence createStopSpannale() {
			SpannableStringBuilder spannable = new SpannableStringBuilder();
			spannable.append(" * 阻止游戏断线");
			int stopCount = GameManager.getInstance().getTotalReconnectCount();
			String stopCountDesc = String.valueOf(stopCount);
			int start = spannable.length();
			int end = start+stopCountDesc.length();
			spannable.append(stopCountDesc);
			int color = AppMain.getContext().getResources().getColor(R.color.game_achieve_content_text_date_color);
			spannable.setSpan(new ForegroundColorSpan(color), start, end , Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			spannable.setSpan(new AbsoluteSizeSpan(30,true), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			spannable.append("次");
			ImageSpan stopSpan = new MyImageSpan(AppMain.getContext(), R.drawable.achievement_to_share_stop);
			spannable.setSpan(stopSpan, 1,2, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
			return spannable;
		}

		private CharSequence createAccelSpannale() {
			SpannableStringBuilder spannable = new SpannableStringBuilder();
			spannable.append("我们为您 * ");
			ImageSpan rocketsSpan = new MyImageSpan(AppMain.getContext(), R.drawable.achievement_to_share_rockets);
			int iconIndex = spannable.length() - 2;
			spannable.append("了");
			spannable.append(createAccelTimeSpannable());
			spannable.setSpan(rocketsSpan, iconIndex, iconIndex + 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
			return spannable;
		}
		
		private Spannable createAccelTimeSpannable(){
			int accelTimeSecondsAmount = ConfigManager.getInstance().getDebugGameAccelTimeSenconds();
			if(accelTimeSecondsAmount <= 0){
				accelTimeSecondsAmount = GameManager.getInstance().getAccelTimeSecondsAmount();
			}
			SpannableStringBuilder spannable = new SpannableStringBuilder();
	
			DateParams dateParams = DateParams.build(accelTimeSecondsAmount, "时");
			int color = AppMain.getContext().getResources().getColor(R.color.game_achieve_content_text_time_color);
			if (dateParams.hour > 0) {
				spannable.append(createSpannableByDateParams(dateParams.hour, color));
				spannable.append(dateParams.hourUnit);
			}
			if (dateParams.minute > 0) {
				spannable.append(createSpannableByDateParams(dateParams.minute,color));
				spannable.append(DateParams.UNIT_MINUTE);
				
			}
			if (dateParams.second > 0 || (dateParams.hour <= 0 && dateParams.minute <= 0)) {
				spannable.append(createSpannableByDateParams(dateParams.second,color));
				spannable.append(DateParams.UNIT_SECOND);
			}
			return spannable;
		}
		
		private SpannableStringBuilder createSpannableByDateParams(int time,int color) {
			SpannableStringBuilder spannable = new SpannableStringBuilder();
			String value = String.valueOf(time);
			spannable.append(value);
			int start = 0;
			int end = start + value.length();
			spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			spannable.setSpan(new AbsoluteSizeSpan(22,true), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			return spannable;
		}
		
		
	}

	private void initButtonView() {
		View buttonClose = findViewById(R.id.button_close);
		buttonClose.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ActivityNewGamePlayAchieve.this.finish();
			}
		});
		
		View buttonShare = findViewById(R.id.button_share);
		buttonShare.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AchieveShareDialog.showDialog(ActivityNewGamePlayAchieve.this);
			}
		});
	}


	private void initAchieve() {
		SpannableStringBuilder spannable = new SpannableStringBuilder();
		spannable.append("恭喜你击败了");
		int percentStart = spannable.length();
		int percent = ConfigManager.getInstance().getGamePlayAchievePercent();
		String percentDesc = String.valueOf(percent);
		spannable.append(percentDesc);
		int percentEnd = percentStart + percentDesc.length();
		int color = getResources().getColor(R.color.game_achieve_content_text_date_color);
		spannable.setSpan(new ForegroundColorSpan(color), percentStart, percentEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		spannable.setSpan(new AbsoluteSizeSpan(30,true), percentStart, percentEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		spannable.append("%");
		int percentUnitEnd = percentEnd + 1;
		spannable.setSpan(new ForegroundColorSpan(color), percentEnd, percentUnitEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		spannable.setSpan(new AbsoluteSizeSpan(15,true), percentEnd, percentUnitEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		spannable.append("的小伙伴");
		TextView textAchieve = (TextView) findViewById(R.id.text_achieve);
		textAchieve.setText(spannable);
	}

	private static final class MyImageSpan extends ImageSpan {
		public MyImageSpan(Context arg0, int arg1) {
			super(arg0, arg1);
		}

		@Override
		public int getSize(Paint paint, CharSequence text, int start, int end,
				FontMetricsInt fm) {
			Drawable d = getDrawable();
			Rect rect = d.getBounds();
			if (fm != null) {
				FontMetricsInt fmPaint = paint.getFontMetricsInt();
				int fontHeight = fmPaint.bottom - fmPaint.top;
				int drHeight = rect.bottom - rect.top;

				int top = drHeight / 2 - fontHeight / 4;
				int bottom = drHeight / 2 + fontHeight / 4;

				fm.ascent = -bottom;
				fm.top = -bottom;
				fm.bottom = top;
				fm.descent = top;
			}
			return rect.right;
		}

		@Override
		public void draw(Canvas canvas, CharSequence text, int start, int end,
				float x, int top, int y, int bottom, Paint paint) {
			Drawable b = getDrawable();
			canvas.save();
			int transY = 0;
			transY = ((bottom - top) - b.getBounds().bottom) / 2 + top;
			canvas.translate(x, transY);
			b.draw(canvas);
			canvas.restore();
		}
	}
	
}
