package cn.wsds.gamemaster.debugger;


public class DebugParams {

	private static boolean openAccelAlwaysFail;

	public static void setOpenAccelAlwaysFail(boolean alwaysFail) {
		openAccelAlwaysFail = alwaysFail;
	}

	public static boolean getOpenAccelAlwaysFail() {
		return openAccelAlwaysFail;
	}

}
