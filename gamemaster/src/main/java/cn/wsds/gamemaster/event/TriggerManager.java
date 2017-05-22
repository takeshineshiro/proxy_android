package cn.wsds.gamemaster.event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.AppProfile;
import cn.wsds.gamemaster.event.EventObserver.ReconnectResult;

import com.subao.airplane.SwitchState;
import com.subao.common.net.NetTypeDetector;
import com.subao.common.utils.ThreadUtils;
import com.subao.data.InstalledAppInfo;

/**
 * 全局的事件触发器，可被观察<br />
 * 注意：<br />
 * 1、所有事件在UI线程被触发<br />
 * 2、触发是异步的，即raiseXXX()函数里并不会触发事件，要等到Handler处理下一轮消息时才会触发
 */
public class TriggerManager implements EventObservable {
    private final static boolean LOG = false;
    private final static String TAG = "TriggerManager";

    private static final int ServiceCreate = 1;
    private static final int VpnOpen = 2;
    private static final int VpnClose = 3;
    private static final int NetChange = 4;
    private static final int APStateChange = 5;
    private static final int AirplaneModeChanged = 6;
    private static final int NewFeedbackReply = 7;
    private static final int AppInstalled = 8;
    private static final int APP_REMOVED = 9;
//    private static final int NetNodeChange = 10;
    private static final int StartVPNFailed = 11;
    private static final int WifiEnableChanged = 12;
    private static final int ScreenOn = 13;
    private static final int ScreenOff = 14;
    private static final int TopTaskChange = 15;
    private static final int StartNewGame = 16;
    private static final int NetDelayChange = 17;    // 网络延迟发生改变
    //	private static final int ShortConnGameNetRequestStart	= 18;	// 短连接游戏网络请求开始
    private static final int ShortConnGameNetRequestEnd = 19;    // 短连接游戏网络请求结束
    //	private static final int GameNodeDetectBegin = 20;	// 节点检测开始
    //	private static final int GameNodeDetectEnd = 21;	// 节点检测完成
    private static final int SupportedGameListUpdate = 22;    // 支持的游戏列表更新了
    private static final int NetRightsDisabled = 23; // 本程序的网络权限被禁止了
    private static final int ReconnectResult = 24;// 断线重连结果返回
    private static final int AccelSwitchChange = 25;//加速开关变化
    //	private static final int TcpConnectTestResult = 26; // TCP连接测速结果
    private static final int MEDIA_MOUNTED = 27;    // SD卡挂载成功
    private static final int RemoteNetDelayChange = 28;    // 加速网络延迟发生改变
    private static final int AUTO_CLEAN_PROCESS = 30; //自动清理内存
    private static final int WIFI_ACCEL_STATE = 31; //wifi 加速状态

    private static final TriggerManager instance = new TriggerManager();

    //唯一的成员变量，本身是线程安全的，如果以后加新的成员变量，要用锁
    private final EventObservable core = new EventObservable();

    public static TriggerManager getInstance() {
        if (GlobalDefines.CHECK_MAIN_THREAD) {
            if (!ThreadUtils.isInAndroidUIThread()) {
                MainHandler.getInstance().showDebugMessage("TriggerManager.getInstance()");
            }
        }
        return instance;
    }

    private TriggerManager() {
    }

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            EventObserver[] obs = core.getNotifyObservers();
            if (obs == null)
                return;
            int intValue;
            String strValue;
            GameInfo gameInfo;
            switch (msg.what) {
                case ServiceCreate:
                    for (EventObserver o : obs)
                        o.onVpnServiceCreate();
                    break;
                case VpnOpen:
                    for (EventObserver o : obs)
                        o.onVPNOpen();
                    break;
                case VpnClose:
                    for (EventObserver o : obs)
                        o.onVPNClose();
                    break;
                case NetChange:
                    NetTypeDetector.NetType netType = (NetTypeDetector.NetType) msg.obj;
                    for (EventObserver o : obs) {
                        o.onNetChange(netType);
                    }
                    break;
                case APStateChange:
                    intValue = (Integer) msg.obj;
                    for (EventObserver o : obs)
                        o.onAPStateChange(intValue);
                    break;
                case AirplaneModeChanged:
                    SwitchState ss = (SwitchState) msg.obj;
                    for (EventObserver o : obs)
                        o.onAirplaneModeChanged(ss);
                    break;
                case NewFeedbackReply:
                    if (msg.obj == null)
                        break;
                    @SuppressWarnings("unchecked")
                    List<UUID> newReplyUUIDList = (List<UUID>) msg.obj;
                    for (EventObserver o : obs)
                        o.onNewFeedbackReply(newReplyUUIDList);
                    break;
                case AppInstalled:
                    InstalledAppInfo iai = (InstalledAppInfo) msg.obj;
                    for (EventObserver o : obs)
                        o.onAppInstalled(iai);
                    break;
                case APP_REMOVED:
                    strValue = (String) msg.obj;
                    for (EventObserver o : obs) {
                        o.onAppRemoved(strValue);
                    }
                    break;
//                case NetNodeChange:
//                    NetTypeDetector.NetType netType = (NetTypeDetector.NetType) msg.obj;
//                    for (EventObserver o : obs) {
//                        o.onNetChange(netType);
//                    }
//                    break;
                case StartVPNFailed:
                    boolean impowerReject = (Boolean) msg.obj;
                    for (EventObserver o : obs)
                        o.onStartVPNFailed(impowerReject);
                    break;
                case WifiEnableChanged:
                    intValue = (Integer) msg.obj;
                    for (EventObserver o : obs)
                        o.onWifiEnableChanged(intValue);
                    break;
                case TopTaskChange:
                    gameInfo = (GameInfo) msg.obj;
                    for (EventObserver o : obs)
                        o.onTopTaskChange(gameInfo);
                    break;
                case StartNewGame:
                    gameInfo = (GameInfo) msg.obj;
                    for (EventObserver o : obs)
                        o.onStartNewGame(gameInfo);
                    break;
                case ScreenOn:
                    for (EventObserver o : obs)
                        o.onScreenOn();
                    break;
                case ScreenOff:
                    for (EventObserver o : obs)
                        o.onScreenOff();
                    break;
                case NetDelayChange:
                    intValue = (Integer) msg.obj;
                    for (EventObserver o : obs) {
                        o.onFirstSegmentNetDelayChange(intValue);
                    }
                    break;
                case RemoteNetDelayChange:
                    GameManager.SecondSegmentNetDelay secondDelay = (GameManager.SecondSegmentNetDelay) msg.obj;
                    for (EventObserver o : obs) {
                        o.onSecondSegmentNetDelayChange(msg.arg1, secondDelay);
                    }
                    break;
                case ShortConnGameNetRequestEnd:
                    for (EventObserver o : obs) {
                        o.onShortConnGameNetRequestEnd();
                    }
                    break;
                case SupportedGameListUpdate:
                    for (EventObserver o : obs) {
                        o.onSupportedGameUpdate();
                    }
                    break;
                case NetRightsDisabled:
                    for (EventObserver o : obs) {
                        o.onNetRightsDisabled();
                    }
                    break;
                case ReconnectResult:
                    ReconnectResult result = (ReconnectResult) msg.obj;
                    for (EventObserver o : obs) {
                        o.onReconnectResult(result);
                    }
                    break;
                case AccelSwitchChange:
                    boolean state = (Boolean) msg.obj;
                    for (EventObserver o : obs) {
                        o.onAccelSwitchChanged(state);
                    }
                    break;
                case MEDIA_MOUNTED:
                    for (EventObserver o : obs) {
                        o.onMediaMounted();
                    }
                    break;
                case AUTO_CLEAN_PROCESS:
                	@SuppressWarnings("unchecked")
					List<AppProfile> runningAppList = (List<AppProfile>)msg.obj;
                    for(EventObserver o : obs) {
                        o.onAutoProcessClean(runningAppList);
                    }
                    break;
                case WIFI_ACCEL_STATE:
                    boolean isEnable = (Boolean) msg.obj;
                    for (EventObserver o : obs) {
                        o.onGetWifiAccelState(isEnable);
                    }
                    break;
                default:
                    break;
            }

        }
    };

    private void execute(int event, Object param) {
        handler.sendMessage(handler.obtainMessage(event, param));
    }

    @Override
    public void addObserver(EventObserver o) {
        core.addObserver(o);
    }

    @Override
    public void addObserver(int location, EventObserver o) {
        core.addObserver(location, o);
    }

    @Override
    public void deleteObserver(EventObserver o) {
        core.deleteObserver(o);
    }

    public void deleteObservers() {
        core.deleteObservers();
    }

    //----------通知接口--------------
    public void raiseVpnServiceCreate() {
        this.execute(TriggerManager.ServiceCreate, null);
    }

    public void raiseVPNOpen() {
        this.execute(TriggerManager.VpnOpen, null);
    }

    public void raiseVPNClose() {
        this.execute(TriggerManager.VpnClose, null);
    }

    public void raiseAccelSwitchChanged(boolean state) {
        this.execute(TriggerManager.AccelSwitchChange, state);
    }

    public void raiseStartVPNFailed(boolean impowerCancel) {
        this.execute(TriggerManager.StartVPNFailed, impowerCancel);
    }

    public void raiseNetChange(NetTypeDetector.NetType state) {
        this.execute(TriggerManager.NetChange, state);
    }

    public void raiseWifiEnableChanged(int state) {
        this.execute(TriggerManager.WifiEnableChanged, state);
    }

    public void raiseAPStateChange(int state) {
        this.execute(TriggerManager.APStateChange, state);
    }

    public void raiseAirplaneModeChanged(SwitchState state) {
        this.execute(TriggerManager.AirplaneModeChanged, state);
    }

    public void raiseNewFeedbackReply(List<UUID> newReplyUUIDList) {
        this.execute(TriggerManager.NewFeedbackReply, newReplyUUIDList);
    }

    public void raiseAppInstalled(com.subao.data.InstalledAppInfo info) {
        this.execute(TriggerManager.AppInstalled, info);
    }

    /**
     * 触发事件：应用卸载
     *
     * @param packageName 包名
     */
    public void raiseAppRemoved(String packageName) {
        this.execute(TriggerManager.APP_REMOVED, packageName);
    }

    public void raiseTopTaskChange(GameInfo info) {
        if (LOG) {
            Log.d(TAG, "Raise top task change: " + (info == null ? "null" : info.getPackageName()));
        }
        this.execute(TriggerManager.TopTaskChange, info);
    }

    public void raiseScreenOn() {
        this.execute(TriggerManager.ScreenOn, null);
    }

    public void raiseScreenOff() {
        this.execute(TriggerManager.ScreenOff, null);
    }

    public void raiseStartNewGame(GameInfo info) {
        this.execute(TriggerManager.StartNewGame, info);
    }

    /**
     * 触发事件：延迟发生改变
     */
    public void raiseFirstSegmentNetDelayChange(int delayMilliseconds) {
        this.execute(TriggerManager.NetDelayChange, Integer.valueOf(delayMilliseconds));
    }

    //	/** 触发事件：短连接游戏的网络请求开始 */
    //	public void raiseShortConnGameNetRequestStart(int connectTime) {
    //		this.execute(TriggerManager.ShortConnGameNetRequestStart, connectTime);
    //	}

    /**
     * 触发事件：短连接游戏的网络请求结束
     */
    public void raiseShortConnGameNetRequestEnd() {
        this.execute(TriggerManager.ShortConnGameNetRequestEnd, null);
    }

    /**
     * 触发事件：支持的游戏更新
     */
    public void raiseSupportedGameUpdate() {
        this.execute(TriggerManager.SupportedGameListUpdate, null);
    }

    /**
     * 触发事件：网络权限被禁了
     */
    public void raiseNetRightsDisabled() {
        this.execute(TriggerManager.NetRightsDisabled, null);
    }

    /**
     * 触发事件： 断线重连
     */
    public void raiseReconnectResult(ReconnectResult result) {
        this.execute(TriggerManager.ReconnectResult, result);
    }

    /**
     * 触发事件：SD卡可用了
     */
    public void raiseMediaMounted() {
        this.execute(MEDIA_MOUNTED, null);
    }

    /**
     * 触发事件：底层通知节点选择完成了（成功或失败）
     *
     * @param code    底层自定义代码，用于上报统计事件
     * @param uid     哪个游戏？
     * @param succeed 是否成功
     */
    public static void raiseOnNodeDetectEvent(final int code, final int uid, final boolean succeed) {
        instance.handler.post(new Runnable() {
            @Override
            public void run() {
                EventObserver[] obs = instance.core.getNotifyObservers();
                if (obs != null) {
                    for (EventObserver o : obs) {
                        o.onNodeDetectResult(code, uid, succeed);
                    }
                }
            }
        });
    }

    //从系统的Observable源码简化过来
    private static class EventObservable {
        private final List<EventObserver> observers = new ArrayList<EventObserver>(32);

        public EventObservable() {
        }

        public void addObserver(EventObserver observer) {
            addObserver(observers.size(), observer);
        }

        public void addObserver(int location, EventObserver observer) {
            if (observer == null) {
                throw new IllegalArgumentException("The observer can not be null");
            }
            synchronized (this) {
                if (!observers.contains(observer)) {
                    observers.add(location, observer);
                }
            }
        }

        public synchronized void deleteObserver(EventObserver observer) {
            observers.remove(observer);
        }

        public synchronized void deleteObservers() {
            observers.clear();
        }

        public EventObserver[] getNotifyObservers() {
            EventObserver[] arrays = null;
            synchronized (this) {
                int size = observers.size();
                arrays = new EventObserver[size];
                observers.toArray(arrays);
            }
            return arrays;
        }
    }

    public void raiseSecondSegmentNetDelayChange(int uid, GameManager.SecondSegmentNetDelay secondDelay) {
        this.handler.sendMessage(handler.obtainMessage(TriggerManager.RemoteNetDelayChange, uid, 0, secondDelay));
    }
	
    /**
     * 触发定时事件：自动定时清理内存
     */
    public void raiseAutoProcessClean(List<AppProfile> runningAppList) {
        this.execute(AUTO_CLEAN_PROCESS, runningAppList);
    }

    public void raiseWifiAccelState(boolean isEnable) {
        this.execute(WIFI_ACCEL_STATE, isEnable);
    }
}
