package com.subao.common.msg;

import android.content.Context;

import com.subao.common.RoboBase;
import com.subao.common.data.AppType;
import com.subao.common.msg.Message_Installation.UserInfo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessageInstallationTester extends RoboBase {
	
	@Test
	public void testUserInfo(){
		String imsi = "abc";
		String sn = "sds";
		String mac = "mac";
		String deviceId = "deviceId";
		String androidId = "androidId" ;
		UserInfo userInfo = new  UserInfo(imsi, sn, mac, deviceId, androidId) ;
		assertTrue(userInfo!=null);
	}
	
	@Test
	public void testUserInfoCreate(){	 
		UserInfo userInfo = UserInfo.create(getContext());
		assertTrue(userInfo!=null);
	}
	
	@Test
	public void testMessageInstallation(){
		String versionName = "2.2.4";
		String channel = "g_official";
		List<Message_App> appList = new ArrayList<Message_App>();
		Message_App app = new Message_App("game","cn.wsds.games");
		appList.add(app);
		AppType appType = AppType.ANDROID_APP;
        Context context = getContext();
		Message_Installation installation = new Message_Installation(
            appType, System.currentTimeMillis() / 1000,
            UserInfo.create(context),
            new Message_DeviceInfo(context),
            Message_VersionInfo.create(versionName, channel),
            appList);
		assertNotNull(installation);
		
		testgetAppList(installation);
	}
	 
	
	public void testgetAppList(Message_Installation installation){
		if(installation==null){
			return ;
		}
		
		Iterable<Message_App> appList = installation.getAppList();
		assertNotNull(appList);
	}
}