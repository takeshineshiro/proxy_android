//package cn.wsds.gamemaster.netcheck;
//
//import cn.wsds.gamemaster.netcheck.NetworkCheckEvent.NetExceptionDesc;
//
//import com.subao.vpn.VPNJni;
//import com.subao.vpn.VPNManager;
//
//public class SocketChecker extends NetworkChecker {
//
//	@Override
//	public Action run(NetworkCheckEventReceiver networkCheckEventReceiver) {
//		int state = VPNManager.getInstance().checkSocketState();
//		if(state == VPNJni.SOCKET_STATE_FIREWALL) {
//			//this.addEvent(NetworkCheckEvent.FIREWALL, false, "防火墙阻挡了应用上网权限");
//			addEvent(networkCheckEventReceiver, NetworkCheckEvent.FIREWALL, false, "网络权限被禁止", NetExceptionDesc.NetworkAuthorizationForbidded);
//			return Action.ABORT;
//		}		
//		//this.addEvent(NetworkCheckEvent.FIREWALL, true, "防火墙配置正确");
//
//		switch(state) {
//		case VPNJni.SOCKET_STATE_OK:
//			//this.addEvent(NetworkCheckEvent.SOCKET_STATE, true, "Socket状态正常");
//			return Action.NEXT;
//		case VPNJni.SOCKET_STATE_ENFILE:
//			addEvent(networkCheckEventReceiver, NetworkCheckEvent.SOCKET_STATE, false, "系统可创建Socket达到上限", NetExceptionDesc.SocketOverload);
//			return Action.ABORT;
//		case VPNJni.SOCKET_STATE_ENOMEM:
//			addEvent(networkCheckEventReceiver, NetworkCheckEvent.SOCKET_STATE, false, "系统无可用内存创建Socket", NetExceptionDesc.NoMemoryToCreateSocket);
//			return Action.ABORT;
//		case VPNJni.SOCKET_STATE_OTHER:
//			addEvent(networkCheckEventReceiver, NetworkCheckEvent.SOCKET_STATE, false, "创建Socket失败", NetExceptionDesc.FailToCreateSocket);
//			return Action.ABORT;
//		case VPNJni.SOCKET_STATE_PROTECT_ERROR:
//			addEvent(networkCheckEventReceiver, NetworkCheckEvent.SOCKET_STATE, false, "保护Socket失败", NetExceptionDesc.FailToProtectSocket);
//			return Action.ABORT;
//		}
//		return Action.ABORT;
//	}
//}
