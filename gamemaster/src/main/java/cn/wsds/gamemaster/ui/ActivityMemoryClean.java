package cn.wsds.gamemaster.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ResUsageChecker;
import cn.wsds.gamemaster.ResUsageChecker.ResUsage;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.data.ProcessCleanRecords;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.ProcessKiller;
import cn.wsds.gamemaster.tools.SystemInfoUtil;
import cn.wsds.gamemaster.ui.floatwindow.DelayTextSpannable;
import cn.wsds.gamemaster.ui.mainfragment.FragmentList;
import cn.wsds.gamemaster.ui.memoryclean.animation.AnimationManager;
import cn.wsds.gamemaster.ui.memoryclean.animation.OnAnimationEndListener;
import cn.wsds.gamemaster.ui.memoryclean.animation.SmoothGridView;
import cn.wsds.gamemaster.ui.view.CircleProgres;

import com.subao.data.InstalledAppInfo;

/**
 * 内存清理
 */
public class ActivityMemoryClean extends ActivityBase {
	
	private SmoothGridView gameGridView;
	private TextView textLabel;
	private ItemProgress itemMemoryUsage;
	private ItemProgress itemCPUTemperature;
	private ItemProgress itemBackgroundApplication;
	private AnimationManager animationManager;
	private DelayUpdateBackgroundAppCount delayUpdateBackgroundAppCount;

	private final class DelayUpdateBackgroundAppCount implements Runnable {

		private int appCount;

		public void delayUpdate(int appCount){
			this.appCount = appCount;
			itemBackgroundApplication.circleProgres.postDelayed(this, 1000);
		}
		
		@Override
		public void run() {
			itemBackgroundApplication.setProgressAndRange(appCount);
			setRunAppLabel(appCount);
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_memory_clean);
		setDisplayHomeArrow("内存清理");
		textLabel = (TextView) findViewById(R.id.text_label);
		gameGridView = (SmoothGridView) findViewById(R.id.game_grid_view);
		initMemoryUsage();
		initCPUUsage();
		
		//FIXME: 由上层通过intent把List<AppProfile>传过来，优化……
		Set<String> runningAppPackageNames = ProcessCleanRecords.getInstance().getCleanRecord(null);
		int runAppSize = runningAppPackageNames.size();
		InstalledAppInfo[] installedApps = GameManager.getInstance().getInstalledApps();
		initBackgroundApps(runAppSize,installedApps.length);
		List<Drawable> runAppDrawables = getRunAppDrawables(runningAppPackageNames,installedApps);
		setGameGirdView(runAppDrawables);
		
		animationManager = createAnimationManager();
		animationManager.execute(new OnAnimationEndListener() {
			
			@Override
			public void onAnimationEnd() {
				finish(true);
			}
		});
		
		ProcessKiller.execute(AppMain.getContext(), runningAppPackageNames);
		
	}
	
	public void finish(boolean result) {
		if(result){
			FragmentList.setMemoryCleanTimeMillis();
			Statistic.addEvent(getApplicationContext(), Statistic.Event.INTERACTIVE_CLEARRAM_OVER);
			setResult(RESULT_OK);
			overridePendingTransition(0, R.anim.memory_clean_exit);
		}else{
			setResult(RESULT_CANCELED);
		}
		finish();
	}

	private void setGameGirdView(List<Drawable> runAppDrawables) {
		int[] iconIds = new int[]{
				R.id.icon1,R.id.icon2,R.id.icon3,R.id.icon4
		};
		LinearLayout tableRow = createTabRow();
		gameGridView.addView(tableRow);
		int iconIndex = 0;
		for (Drawable drawable : runAppDrawables) {
			if(iconIndex == iconIds.length){
				iconIndex = 0;
				tableRow = createTabRow();
				gameGridView.addView(tableRow);
			}
			ImageView icon = (ImageView) tableRow.findViewById(iconIds[iconIndex++]);
			icon.setImageDrawable(drawable);
		}
	}

	private LinearLayout createTabRow() {
		return (LinearLayout) View.inflate(getApplicationContext(), R.layout.item_memory_clean_icon, null);
	}

	private void initMemoryUsage() {
		ProgressValueRange range = new ProgressValueRange(55, 80);
		itemMemoryUsage = new ItemProgress(findViewById(R.id.item_memory_usage), "内存占用","%",100,range);
		ResUsage debugUsage = ResUsageChecker.getInstance().getDebugUsage();
		int memoryUsage = debugUsage!=null ? debugUsage.memoryUsage: SystemInfoUtil.getMemoryUsage(getApplicationContext());
		memoryUsage = Math.max(0, memoryUsage);
		itemMemoryUsage.setProgressAndRange(memoryUsage);
	}

	private void initCPUUsage() {
		ProgressValueRange range = new ProgressValueRange(40, 70);
		itemCPUTemperature = new ItemProgress(findViewById(R.id.item_cpu_temperature), "CPU占用率","%",100,range);
		ResUsage debugUsage = ResUsageChecker.getInstance().getDebugUsage();
		int cpuUsage = debugUsage!=null ? debugUsage.cpuUsage: SystemInfoUtil.getCpuUsage();
		cpuUsage =  Math.max(0, cpuUsage);
		itemCPUTemperature.setProgressAndRange(cpuUsage);
	}

	private void initBackgroundApps(int runAppSize, int length) {
		int maxNormal = length / 3;
		ProgressValueRange range = new ProgressValueRange(maxNormal, maxNormal *2);
		itemBackgroundApplication = new ItemProgress(findViewById(R.id.item_background_application), "后台应用","个",length,range);
		int appCleanCount = getIntent().getIntExtra(IntentExtraName.INTENT_EXTRANAME_APP_CLEAN_COUNT, -1);
		if(appCleanCount!=-1&&appCleanCount!=runAppSize){
			delayUpdateBackgroundAppCount = new DelayUpdateBackgroundAppCount();
			delayUpdateBackgroundAppCount.delayUpdate(runAppSize);
		}else{
			appCleanCount = runAppSize;
		}
		itemBackgroundApplication.setProgressAndRange(appCleanCount);
		setRunAppLabel(appCleanCount);
	}

	private AnimationManager createAnimationManager() {
		View starGroup = findViewById(R.id.star_group);
		ViewFlipper viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
		AnimationManager animationManager = AnimationManager.createInstance(gameGridView,textLabel,viewFlipper,starGroup,itemMemoryUsage,itemCPUTemperature,itemBackgroundApplication,findViewById(R.id.view_group));
		return animationManager;
	}
	
	/**
	 * @param size
	 */
	private void setRunAppLabel(int size) {
		Spannable spannable = DelayTextSpannable.getSpecialInMid("有", String.valueOf(size), "个应用正在运行", getResources().getColor(R.color.color_game_8));
		textLabel.setText(spannable);
	}

	private List<Drawable> getRunAppDrawables(Set<String> runningAppPackageNames,InstalledAppInfo[] installedApps){
		List<Drawable> listDrawables = new ArrayList<Drawable>();
		Context context = AppMain.getContext();
		for (String packageNames: runningAppPackageNames) {
			for (InstalledAppInfo installedAppInfo : installedApps) {
				if(packageNames.equals(installedAppInfo.getPackageName())){
					listDrawables.add(installedAppInfo.getAppIcon(context));
					break;
				}
			}
		}
		return listDrawables;
	}
	
	private static final class ProgressValueRange {
		private final int maxNormal;
		private final int maxPoor;
		private ProgressValueRange(int maxNormal, int maxPoor) {
			this.maxNormal = maxNormal;
			this.maxPoor = maxPoor;
		}
	}
	public static final class ItemProgress {
		
		public final CircleProgres circleProgres;
		public final TextView textValue;
		public final int maxProgress;
		public final int minProgress;
		public final int minValue;
		private final ProgressValueRange progressValueRange;
		public enum ProgressRange {
			bad,poor,normal;
		}
		
		public ItemProgress(View grounpView,String title,String unit,int maxProgress,ProgressValueRange progressValueRange) {
			if(grounpView == null){
				throw new RuntimeException("内存清理 项 组件为空");
			}

			if(maxProgress < 0){
				throw new RuntimeException("最大进度必须大于0");
			}
			this.progressValueRange = progressValueRange;
			this.maxProgress = maxProgress;
			this.minProgress = 0;
			this.minValue = 0;
			circleProgres = (CircleProgres) grounpView.findViewById(R.id.circleProgres);
			circleProgres.setMaxProgress(maxProgress);
			circleProgres.setMinProgress(minProgress);
			textValue = (TextView) grounpView.findViewById(R.id.text_value);

			TextView textTitle = (TextView) grounpView.findViewById(R.id.text_title);
			textTitle.setText(title);
			TextView textUnit = (TextView) grounpView.findViewById(R.id.text_unit);
			textUnit.setText(unit);
		}
		
		/**
		 * 根据当前进度 获得进度区间范围
		 * @param progress
		 * @return
		 */
		public ProgressRange getProgressRange(int progress){
			if(progress == minProgress){
				return ProgressRange.normal;
			}
			if(progress == maxProgress || progress <=0){
				return ProgressRange.bad;
			}
			if(progress <= progressValueRange.maxNormal){
				return ProgressRange.normal;
			}else if(progress <= progressValueRange.maxPoor){
				return ProgressRange.poor;
			}else{
				return ProgressRange.bad;
			}
		}
		
		public void setProgressAndRange(int progress){
			ProgressRange progressRange = getProgressRange(progress);
			int progressRangeColor = getProgressRangeColor(progressRange);
			circleProgres.setProgress(progress);
			textValue.setText(String.valueOf(progress));
			circleProgres.setRingColor(circleProgres.getResources().getColor(progressRangeColor));
		}
		
		public int getProgressRangeColor(ProgressRange progressRange){
			switch (progressRange) {
			case normal:
				return R.color.memory_clean_progress_color_noraml;
			case poor:
				return R.color.memory_clean_progress_color_poor;
			case bad:
			default:
				return R.color.memory_clean_progress_color_bad;
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		// 必须要重载父类的返回键
		autoBack();
	}
	
	@Override
	protected void onActionBackOnClick() {
		// 必须重载的父类方法
		autoBack();
	}
	
	private void autoBack(){
//		if(animationManager.isRunning()){
//			animationManager.interrupt();
//			UIUtils.showToast("本次清理失败，请重新清理");
//		}
//		finish(false);
	}
}
