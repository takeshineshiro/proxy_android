package cn.wsds.gamemaster.ui.accel;

import java.util.ArrayList;
import java.util.List;

import com.subao.common.Logger;
import com.subao.net.NetManager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.RemoteException;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.service.GameVpnService;
import cn.wsds.gamemaster.service.VPNGlobalDefines.CloseReason;
import cn.wsds.gamemaster.service.aidl.IGameVpnService;
import cn.wsds.gamemaster.statistic.StatisticAccProcessStart;
import cn.wsds.gamemaster.tools.SystemInfoUtil;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailProcesser;
import cn.wsds.gamemaster.ui.accel.failprocessor.FailType;
import cn.wsds.gamemaster.ui.accel.failprocessor.VpnFailProcesser;
import cn.wsds.gamemaster.ui.floatwindow.FloatWindowVpnImpowerHelp;

public class VpnOpener extends AccelOpener {

	private static final String TAG = "VpnOpener";
    private Activity activity;
    private Context context;

	private static boolean lastVPNOpenerFlag; //最近一次是否开启VPN加速

	public static boolean isLastVPNOpenerFlag() {
		return lastVPNOpenerFlag;
	}

	public VpnOpener(){
    	this(null, null, null);
    }

    public VpnOpener(Listener l, OpenSource openSource, AccelOpener.Tester tester) {
        super(l, openSource, false, tester);
    }

    public boolean isStarted() {
        //return GameVpnService.isVPNStarted();
        
        IGameVpnService iVpnService= VPNUtils.getIGameVpnService();
		if(iVpnService == null){
			return false;
		}
		try {
			return iVpnService.isVPNStarted();
		} catch (RemoteException e) {
		    Logger.e(TAG, e.toString());
		    return false ;
		}
		
    }

    public void close(CloseReason reason) {
    	IGameVpnService iVpnService= VPNUtils.getIGameVpnService();
		if(iVpnService == null){
			return ;
		}
		try {
			iVpnService.closeVPN(reason.ordinal());
		} catch (RemoteException e) {
			Logger.e(TAG, e.toString());	   
		}
       // GameVpnService.closeVPN(reason);
    }

    @Override
    public void doOpen(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
        // 需要系统授权吗？
        Intent intentImpower = null;
        try {
        	// 注意：在某些机型手机上，在我们已经连上VPN（有小钥匙）的情况下，用户卸载我们
        	// 然后再安装我们，VpnService.prepare()函数仍然会返回null，但真正开启的时候会失败
            intentImpower = VpnService.prepare(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (intentImpower == null) {
            startVPNAndEndWorkflow();
            return;
        }
        doImpower(activity, intentImpower);
    }

    @Override
    protected FailType checkFailCondition(Context context) {
        // 检查WiFi热点
        if (NetManager.getInstance().isWifiAPEnabledOrEnabling()) {
            return FailType.WifiAP;
        }
        // 检查网络权限
       /* if (!VPNManager.getInstance().networkCheck()) {
            return FailType.NetworkCheck;
        }*/
        
        if(!VPNUtils.networkCheck(TAG)){
        	return FailType.NetworkCheck;
        }
        // 检查WAP
        if (NetManager.getInstance().isWapCurrent()) {
            return FailType.WAP;
        }
        return null;
    }

    private void doImpower(Activity activity, Intent intent) {
        if (activity != null) {
        	StatisticAccProcessStart.getInstance().addStep(StatisticAccProcessStart.STEP_VPN_IMPOWER_REMIND);
            try {
                activity.startActivityForResult(intent, GlobalDefines.START_ACTIVITY_REQUEST_CODE_VPN_IMPOWER);
                return;
            } catch (Exception e) {
            }
        }
        this.listener.onStartFail(VpnOpener.this, FailType.ImpowerError);
		StatisticAccProcessStart.getInstance().end(StatisticAccProcessStart.STEP_VPN_IMPOWER_ERROR,StatisticAccProcessStart.STEP_VPN_ACCEL_ERROR);
    }

    private void startVPNAndEndWorkflow() {
        //GameVpnService.StartVpnResult svr = GameVpnService.startVPN();
    	IGameVpnService iVpnService= VPNUtils.getIGameVpnService();
		if(iVpnService == null){
			return ;
		}
	 
		int svr = 0;
		try {	
			svr = iVpnService.startVPN(getPackgeNamesIfNeedAddWhiteList());
		} catch (RemoteException e) {
		    Logger.e(TAG, e.toString());			 
		}
		
        boolean succeed = (svr == GameVpnService.StartVpnResult.OK.ordinal());
        if (succeed) {
            listener.onStartSucceed();
            StatisticAccProcessStart.getInstance().end(StatisticAccProcessStart.STEP_VPN_ACCEL_SUCCEED);
			lastVPNOpenerFlag = true;
        } else {
            listener.onStartFail(VpnOpener.this, FailType.StartError);
        }
    }

    public void onImpowerReject() {
        listener.onStartFail(VpnOpener.this, FailType.ImpowerReject);
    }

    private ListenerReject currentListener = new ListenerRejectFirst();

    private interface ListenerReject {
        void onImpowerReject();
    }

    private class ListenerRejectSecond implements ListenerReject {
        @Override
        public void onImpowerReject() {
            UIUtils.showToast("您与好礼只差一个授权了o(>_<)o ~");
            listener.onStartFail(VpnOpener.this, FailType.ImpowerReject);
        }

    }

    /**
     * 第一次授权被拒绝监听
     */
    private class ListenerRejectFirst implements ListenerReject {
    	
        @Override
        public void onImpowerReject() {//第一次授权被拒绝监听需要引导用户重新开启加速
        	StatisticAccProcessStart.getInstance().addStep(StatisticAccProcessStart.STEP_VPN_IMPOWER_REJECTED_PORMPT);
        	
        	DialogInterface.OnClickListener clickListner = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
	                    currentListener = new ListenerRejectSecond();
	                    doOpen(activity, context);
					} else if (which == DialogInterface.BUTTON_NEGATIVE) {
						cancel();
					}
					
				}
			};
			DialogWhenImpowerReject.showInstance(activity, clickListner, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel();
                }
            });
		}

        private void cancel() {
        	StatisticAccProcessStart.getInstance().end();
            UIUtils.showToast("未获得授权，开启失败");
            listener.onStartFail(VpnOpener.this, FailType.ImpowerCancel);
        }
    }

    private void onImpowerOK() {
        startVPNAndEndWorkflow();
    }

    @Override
    public void checkResult(int request, int result, Intent data) {
        if (request == GlobalDefines.START_ACTIVITY_REQUEST_CODE_VPN_IMPOWER) {
            FloatWindowVpnImpowerHelp.destroyInstance();
            switch (result) {
            case Activity.RESULT_OK:
                this.onImpowerOK();
                break;
            case Activity.RESULT_CANCELED:
                currentListener.onImpowerReject();
                break;   
            }
        }
    }

    @Override
    public boolean isGotPermission() {
        try {
            return VpnService.prepare(AppMain.getContext()) == null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void interrupt() {
        if (currentListener instanceof ListenerRejectFirst) {
            UIUtils.showToast("未获得授权，开启失败");

        } else if (currentListener instanceof ListenerRejectSecond) {
            UIUtils.showToast("您与好礼只差一个授权了o(>_<)o ~");
        }
        this.onImpowerReject();
    }

    @Override
    public boolean hasModel() {
        String packageName = "com.android.vpndialogs";
        if (packageName == null || "".equals(packageName)) {
            return false;
        }

        try {
            ApplicationInfo info = AppMain.getContext().getPackageManager()
                    .getApplicationInfo(packageName,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

    }

    @Override
    public FailProcesser getFailProcesser() {
        return new VpnFailProcesser();
    }

	private static List<String> getPackgeNamesIfNeedAddWhiteList(){
		List<String> packgeNames = null ;
		
		if(SystemInfoUtil.isStrictOs()){  //5.0以上Android版本，将游戏包名加入VPNService白名单 , VPNService只服务于游戏
			List<GameInfo> supportedAndReallyInstalledGames = GameManager.getInstance()
					.getSupportedAndReallyInstalledGames();
			if(supportedAndReallyInstalledGames!=null){
				int count = supportedAndReallyInstalledGames.size();
				if(count>0){
					packgeNames = new ArrayList<String>(count) ;
					for(int i = 0; i<count ; i++){
						String packgeName = supportedAndReallyInstalledGames.get(i).getPackageName();
						packgeNames.add(packgeName);
					}
				}				
			}
		}
		
		return packgeNames ;  //5.0以下版本无需加VPNService白名单，返回null即可
	}

}
