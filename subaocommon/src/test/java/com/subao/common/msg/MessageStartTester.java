package com.subao.common.msg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.subao.common.data.AppType;
import com.subao.common.msg.Message_Start.ScriptResult;

public class MessageStartTester{
	@Test
	public void testResult(){
		assertEquals(5,Message_Start.Result.values().length);
		
		assertEquals(0,Message_Start.Result.UNKNOWN_EXCE_RESULT.id);
		assertEquals(1,Message_Start.Result.NO_SCRIPT.id);
		assertEquals(2,Message_Start.Result.SCRIPT_DOWNLOAD_FAIL.id);
		assertEquals(3,Message_Start.Result.SCRIPT_EXEC_SUCCESS.id);
		assertEquals(4,Message_Start.Result.SCRIPT_EXEC_FAIL.id);
	}
	
	@Test
	public void testStartType(){
		assertEquals(3,Message_Start.StartType.values().length);
		
		assertEquals(0,Message_Start.StartType.UNKNOWN_START_TYPE.id);
		assertEquals(1,Message_Start.StartType.START.id);
		assertEquals(2,Message_Start.StartType.DAILY.id);
	}
	
	@Test
	public void testScriptResult(){
		Message_Start.Result result = Message_Start.Result.SCRIPT_EXEC_SUCCESS;
		String note = "avs";
		ScriptResult  sResult = new ScriptResult(result,note);
		assertNotNull(sResult);
	}
	
	@Test
	public void testMessageStart() {
		String subaoId = "abc";
		MessageUserId.setCurrentSubaoId(subaoId);
		Message_Start.StartType startType = Message_Start.StartType.START;
		int nodeNum = 20;
		int gameNum = 10;
		Message_Start.Result result = Message_Start.Result.SCRIPT_EXEC_SUCCESS;
		String note = "avs";
		ScriptResult scriptResult = new ScriptResult(result, note);
		List<Message_App> appList = new ArrayList<Message_App>();
		Message_App app = new Message_App("game", "cn.wsds.games");
		appList.add(app);
		Message_VersionInfo version = Message_VersionInfo.create("2.2.4", "g_official");
		AppType appType = AppType.ANDROID_APP;
		Message_Start msgStart = new Message_Start(
			MessageUserId.build(),
			startType,
			nodeNum, gameNum,
			scriptResult,
			appList,
			version,
			appType);

		assertNotNull(msgStart);

		testgetAppList(msgStart);
	}
	
	
	public void testgetAppList(Message_Start msgStart){
		if(msgStart == null){
			return;
		}
		
		Iterable<Message_App> appList  =  msgStart.getAppList();
		assertNotNull(appList);
	}
}
