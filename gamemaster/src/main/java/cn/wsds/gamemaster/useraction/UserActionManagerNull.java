package cn.wsds.gamemaster.useraction;

class UserActionManagerNull extends UserActionManager {

	@Override
	public void updateSubaoId(String subaoId) {
		// do nothing
	}
	
	@Override
	public void udpateUserId(String userId) {
		// do nothing		
	}

	@Override
	public void onWiFiActivated() {
		// do nothing
	}

	@Override
	public void stopAndWait(long milliseconds) {
		// do nothing
	}

	@Override
	public void addAction(long timeUTCSeconds, String actionName, String param) {
		// do nothing
	}

}
