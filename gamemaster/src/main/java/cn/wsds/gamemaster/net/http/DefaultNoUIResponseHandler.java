package cn.wsds.gamemaster.net.http;

/**
 * 没有任何UI呈现的ResponseHandler
 */
public abstract class DefaultNoUIResponseHandler extends ResponseHandler{
	
	public DefaultNoUIResponseHandler() {
		this(null);
	}
	
	public DefaultNoUIResponseHandler(OnHttpUnauthorizedCallBack onHttpUnauthorizedCallBack){
		super(null, onHttpUnauthorizedCallBack);
	}

	@Override
	protected CharSequence getToastText_RequestFail() {
		return null;
	}
	
	@Override
	public void onNetworkUnavailable() {}
	
}