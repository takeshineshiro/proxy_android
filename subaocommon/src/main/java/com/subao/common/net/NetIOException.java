package com.subao.common.net;

import java.io.IOException;

public class NetIOException extends IOException {

	private static final long serialVersionUID = -6431629986119175685L;
	
	public NetIOException() {
		super("网络权限可能被禁用");
	}

}
