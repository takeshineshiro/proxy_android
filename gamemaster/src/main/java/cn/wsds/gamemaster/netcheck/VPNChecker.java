//package cn.wsds.gamemaster.netcheck;
//
//import cn.wsds.gamemaster.netcheck.NetworkCheckEvent.NetExceptionDesc;
//import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
//
//public class VPNChecker extends NetworkChecker {
//	
//	@Override
//	public Action run(NetworkCheckEventReceiver networkCheckEventReceiver) {
//		if (!AccelOpenManager.isStarted()) {
//			addEvent(networkCheckEventReceiver, NetworkCheckEvent.VPN_NOT_STARTED, false, "加速服务未开启", NetExceptionDesc.AccelOff);
//			return Action.ABORT;
//		}
//		
//		return Action.NEXT;
//	}
//	
//}
