package cn.wsds.gamemaster.ui;

import java.util.List;

import com.subao.net.NetManager;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import cn.wsds.gamemaster.AppInitializer;
import cn.wsds.gamemaster.AppInitializer.InitReason;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.ui.AccelProcessHelper.OnCloseAnimationListener;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenWorker;
import cn.wsds.gamemaster.ui.accel.AccelOpener;
import cn.wsds.gamemaster.ui.accel.AccelOpener.OpenSource;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailProcesser;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailType;
import cn.wsds.gamemaster.ui.adapter.GamePageAdapter;
import cn.wsds.gamemaster.ui.adapter.gamegrid.factory.ShortcutGameGridAdpterCreater;
import cn.wsds.gamemaster.ui.view.Switch;
import cn.wsds.gamemaster.ui.view.Switch.OnBeforeCheckChangeListener;
import cn.wsds.gamemaster.ui.view.ViewWaitForInit;

public class ActivityShortcut extends ActivityBase {

	private ViewGroup wholeOutterLayout, wholeInnerLayout;
	private ViewWaitForInit viewWaitForInit;

	private ViewPager viewPager;
	private LinearLayout dotLinearLayout;
	private View launcher;
	private Switch accelSwitch;

	private List<GameInfo> gameWholeList;
	private int pageNum;
	private AccelProcessHelper accelOpenAssistant;
	
	private EventObserver eventObserver = new EventObserver() {

		@Override
		public void onAccelSwitchChanged(boolean state) {
			accelSwitch.setChecked(state);
		}

	};


	@Override
	protected boolean autoInvokeAppInitIfNeed() {
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Statistic.addEvent(this, Event.INTERACTIVE_SHORTCUT_CLICK_OPEN);
		setContentView(R.layout.layout_shortcut);
		this.wholeOutterLayout = (ViewGroup) findViewById(R.id.whole_outter_layout);
		this.wholeInnerLayout = (ViewGroup) findViewById(R.id.whole_inner_layout);
		if (AppInitializer.instance.isInitialized()) {
			gotoNormal();
		} else {
			this.wholeInnerLayout.setVisibility(View.GONE);
			((ViewStub)findViewById(R.id.stub_waitting)).setVisibility(View.VISIBLE);
			viewWaitForInit = (ViewWaitForInit)findViewById(R.id.wait_for_init);
			 
			Runnable r = new Runnable() {
				public void run() {
					AppInitializer.instance.execute(InitReason.OTHER_ACTIVITY, ActivityShortcut.this);
					IGameVpnService iVpnService= AppInitializer.instance.getIVpnService();	
					if(iVpnService == null){
						wholeOutterLayout.postDelayed(this, 100);
					}else{
						viewWaitForInit.stop();
						wholeInnerLayout.setVisibility(View.VISIBLE);
						ActivityShortcut.this.gotoNormal();
					}				
				}
			};
			
			this.wholeOutterLayout.post(r);
		}
	}

	private void gotoNormal() {
		if (viewPager != null) {
			return;
		}
		viewPager = (ViewPager) findViewById(R.id.shortcut_viewpager);
		dotLinearLayout = (LinearLayout) findViewById(R.id.shortcut_layout_dot);
		launcher = findViewById(R.id.shortcut_launch);
		accelSwitch = (Switch) findViewById(R.id.shortcut_autoaccel);
		accelSwitch.setChecked(AccelOpenManager.isStarted());
		wholeOutterLayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});

		gameWholeList = GameManager.getInstance().getSupportedAndReallyInstalledGames();
		if (gameWholeList.isEmpty()) {
			findViewById(R.id.stub_no_games).setVisibility(View.VISIBLE);
		}

		GamePageAdapter gamePageAdapter = new GamePageAdapter(gameWholeList,new ShortcutGameGridAdpterCreater());
		gamePageAdapter.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				parent.setEnabled(false);
				Statistic.addEvent(ActivityShortcut.this, Event.ACC_SHORTCUT_CLICK_GAME_START);
                /*记录启动游戏次数*/
                int startNum = ConfigManager.getInstance().getStartGameCount();
                startNum = (startNum + 1) % 65535;
                ConfigManager.getInstance().setStartGameCount(startNum);
				//FIXME 取下标方式
				int location = viewPager.getCurrentItem() * GamePageAdapter.ICONS_PER_PAGE + position;
				GameInfo gameInfo = gameWholeList.get(location);
				if (AccelOpenManager.isStarted()) {
					
					StartNodeDetectUI.Owner owner = new StartNodeDetectUI.Owner() {
						
						@Override
						public boolean isCurrentNetworkOk() {
							return NetManager.getInstance().isConnected();
						}
						
						@Override
						public Activity getActivity() {
							return ActivityShortcut.this;
						}
					};
					
					StartNodeDetectUI.createInstanceIfNotExists(owner, gameInfo,
						new DialogInterface.OnDismissListener() {

							@Override
							public void onDismiss(DialogInterface dialog) {
								ActivityShortcut.this.finish();
							}
						});

				} else {
					accelOpenWorker.openAccel();
					parent.setEnabled(true);
				}
			}
		});
		viewPager.setAdapter(gamePageAdapter);
		pageNum = gamePageAdapter.getPageNum();
		viewPager.addOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				highlightedDot(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}

			@Override
			public void onPageScrollStateChanged(int arg0) {}
		});
		setDisplayDot();
		launcher.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(ActivityShortcut.this, ActivityStart.class));
			}
		});
		final OnCloseAnimationListener onCloseAnimationListener = new OnCloseAnimationListener() {
			
			@Override
			public void onCloseAnimation() {
				accelSwitch.setChecked(false);
			}
		};
		accelSwitch.setOnBeforeCheckChangeListener(new OnBeforeCheckChangeListener() {

			@Override
			public boolean onBeforeCheckChange(Switch checkSwitch, boolean expectation) {
				if (!expectation) {
					AccelProcessHelper.closeAccel(CloseReason.DESKTOP_SHORTCUT_CLOSE,ActivityShortcut.this,onCloseAnimationListener);
				} else {
					accelOpenWorker.openAccel();
				}
				return false;
			}
		});
		accelOpenAssistant = new AccelProcessHelper(this, OpenSource.Shortcut,new AccelOpenerListener());
		TriggerManager.getInstance().addObserver(eventObserver);
		//
		if (viewWaitForInit != null) {
			wholeOutterLayout.post(new Runnable() {
				@Override
				public void run() {
					if (viewWaitForInit != null) {
						wholeOutterLayout.removeView(viewWaitForInit);
						viewWaitForInit = null;	
					}
				}
			});
		}
	}
	
	private final AccelOpenWorker accelOpenWorker = new AccelOpenWorker() {

		@Override
		public void openAccel() {
			accelOpenAssistant.openAccel(this);
		}
	};


	@Override
	protected void onDestroy() {
		super.onDestroy();
		StartNodeDetectUI.destroyInstance(this);
		TriggerManager.getInstance().deleteObserver(eventObserver);
	}

	private SparseArray<ImageView> sparseArray = new SparseArray<ImageView>();

	private void setDisplayDot() {
		if (pageNum <= 1) {
			return;
		}
		for (int i = 0; i < pageNum; i++) {
			ImageView imageView = createImageView();
			dotLinearLayout.addView(imageView, i);
			sparseArray.append(i, imageView);
			if (i == 0) {
				imageView.setImageResource(R.drawable.shape_shortcut_dot_selected);
			} else {
				imageView.setImageResource(R.drawable.shape_shortcut_dot_unselected);
			}
			if (i != pageNum - 1) {
				LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
				layoutParams.setMargins(0, 0, getResources().getDimensionPixelSize(R.dimen.space_size_5), 0);
				imageView.setLayoutParams(layoutParams);
			}
		}
	}

	private void highlightedDot(int position) {
		if (pageNum <= 1) {
			return;
		}
		for (int i = 0; i < pageNum; i++) {
			ImageView imageView = sparseArray.get(i, null);
			if (imageView != null) {
				if (i == position) {
					imageView.setImageResource(R.drawable.shape_shortcut_dot_selected);
				} else {
					imageView.setImageResource(R.drawable.shape_shortcut_dot_unselected);
				}
			}
		}
	}

	private ImageView createImageView() {
		ImageView imageView = new ImageView(this);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.space_size_10), getResources()
			.getDimensionPixelSize(R.dimen.space_size_4));
		imageView.setLayoutParams(layoutParams);
		return imageView;
	}


	private class AccelOpenerListener implements AccelOpener.Listener {

		@Override
		public void onStartFail(AccelOpener accelOpener, FailType type) {
			if (accelOpener != null) {
				accelOpenAssistant.clearAccelOpener();
				FailProcesser failProcesser = accelOpener.getFailProcesser();
				failProcesser.process(accelOpener, type, ActivityShortcut.this);
			}
		}

		@Override
		public void onStartSucceed() {
			if (!isFinishing()) {
				accelOpenAssistant.clearAccelOpener();
				accelSwitch.setChecked(true);
			}
		}
	};

	@Override
	public void onActivityResult(int request, int result, Intent data) {
		if (accelOpenAssistant == null) {
			return;
		}
		AccelOpener accelOpener = accelOpenAssistant.getAccelOpener();
		if (accelOpener != null) {
			accelOpener.checkResult(request, result, data);
		}
	}

}
