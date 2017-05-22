package cn.wsds.gamemaster;

import android.os.SystemClock;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;

/**
 * 自动内存/进程清理的定时触发器
 */
public class AutoProcessCleanTrigger {

    private final static long AUTO_PROCESS_CLEAN_PERIOD_FIVE =  60 * 1000 * 5;
    private final static long AUTO_PROCESS_CLEAN_PERIOD_TEN =  60 * 1000 * 10;
    private final static long AUTO_PROCESS_CLEAN_PERIOD_FIFTEEN =  60 * 1000 * 15;


    private static final AutoProcessCleanTrigger instance = new AutoProcessCleanTrigger();

    private long lastRaiseTime = -AUTO_PROCESS_CLEAN_PERIOD_FIVE;
	/**
	 * 被定时调用：触发TriggerManager的自动清理激活事件
	 */
	private final Runnable autoProcessCleanTimer = new Runnable() {
		
		@Override
		public void run() {
			long now = SystemClock.elapsedRealtime();
			long elapsed = now - lastRaiseTime;
            long period = getAutoProcessCleanPeriod();
			if (elapsed >= period) {
				//FIXME 这里应该改在一个线程里先把正在运行的APP列举出来
				TriggerManager.getInstance().raiseAutoProcessClean(null);
				lastRaiseTime = now;
				elapsed = 0;
			}
			MainHandler.getInstance().postDelayed(this, period - elapsed);
		}
	};

    private class Observer extends EventObserver implements ConfigManager.Observer {

		@Override
		public void onShowDelayInFloatWindowChange(boolean show) {}

		@Override
		public void onAutoCleanProgressSwitchChange(boolean on) {
			switchAutoProcessCleanTimer(on && AccelOpenManager.isStarted());
		}

		@Override
		public void onAccelSwitchChanged(boolean state) {
			switchAutoProcessCleanTimer(state && ConfigManager.getInstance().getAutoProcessClean());
		}
		
		@Override
		public void onFloatWindowSwitchChange(boolean on) {}
	};

	public static AutoProcessCleanTrigger getInstance() {
		return instance;
	}

	private AutoProcessCleanTrigger() {
        Observer observer = new Observer();
        TriggerManager.getInstance().addObserver(observer);
		ConfigManager.getInstance().registerObserver(observer);
		if (ConfigManager.getInstance().getAutoProcessClean() && AccelOpenManager.isStarted()) {
			switchAutoProcessCleanTimer(true);
		}
	}

    /**
     * 从配置中获取并计算自动内存清理时间间隔
     * @return
     */
    private long getAutoProcessCleanPeriod() {
        long period;
        int index = ConfigManager.getInstance().getAutoCleanProcessInternal();
        switch (index) {
            case 2:
                period = AUTO_PROCESS_CLEAN_PERIOD_TEN;
                break;
            case 3:
                period = AUTO_PROCESS_CLEAN_PERIOD_FIFTEEN;
                break;
            case 1:
            default:
                period = AUTO_PROCESS_CLEAN_PERIOD_FIVE;
                break;
        }

        return period;
    }

	private void switchAutoProcessCleanTimer(boolean on) {
		MainHandler mh = MainHandler.getInstance();
		mh.removeCallbacks(autoProcessCleanTimer);
		if (on) {
			mh.post(autoProcessCleanTimer);
			lastRaiseTime = -getAutoProcessCleanPeriod();
		}
	}
}
