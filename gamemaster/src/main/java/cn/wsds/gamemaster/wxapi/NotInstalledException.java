package cn.wsds.gamemaster.wxapi;

public abstract class NotInstalledException extends Exception {

	private static final long serialVersionUID = 864400303281806735L;
	
	private NotInstalledException(String message) {
		super(message);
	}
	
	static class Weixin extends NotInstalledException {

		private static final long serialVersionUID = -9054447390543051529L;

		Weixin() {
			super("未安装微信客户端");
		}
	}
	
	static class QQ extends NotInstalledException {

		private static final long serialVersionUID = 9125809855178066663L;

		QQ() {
			super("未安装QQ客户端");
		}
	}
	
}