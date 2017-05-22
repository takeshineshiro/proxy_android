package cn.wsds.gamemaster.ui;

import hr.client.appuser.Games.Game;
import hr.client.appuser.Games.GetGameListResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.DataCache;
import cn.wsds.gamemaster.data.RecommandGameInfo;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.adapter.AdpaterRecommandGames;
import cn.wsds.gamemaster.ui.adapter.AdpaterRecommandGames.DownloadClickListener;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.data.InstalledAppInfo;

public class ActivityAddNewGame extends ActivityListRefreshBase<RecommandGameInfo> implements DownloadClickListener{

	//private 
	//private boolean isUserLogin =false;
	private ImageView addAllGame;
	private static List<GameInfo> installedGameInfos;
	private TextView textLoadFailed;
	private TextView textGameIsLoading ;
	
	private EventObserver eventObserver = new EventObserver(){

		@Override
		public void onAppInstalled(InstalledAppInfo info) {			
			onGameInstalled(info.getAppLabel());
			 
		}

		@Override
		public void onAppRemoved(String packageName) {		
			onGameUninstalled(packageName);
			super.onAppRemoved(packageName);
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//isUserLogin = UserSession.isLogined();
		addAllGame = (ImageView)findViewById(R.id.add_all_game);
		textLoadFailed = (TextView)findViewById(R.id.text_load_failed);
		textGameIsLoading = (TextView)findViewById(R.id.text_loading);
		
		AdpaterRecommandGames adapter = new AdpaterRecommandGames(this);
		adapter.setDownloadClickListener(this);
		setListAdapter(adapter);
		
		findViewById(R.id.add_game).setOnClickListener(getAddGameListener());
		
		installedGameInfos = GameManager.getInstance().getSupportedAndReallyInstalledGames();
		
		List<RecommandGameInfo> cacheData = DataCache.getRecommandgameinfoscache().getCacheData();
		if(cacheData.isEmpty()){
			doRefresh();
		}else{			
			checkAndSetCacheData(cacheData);			
		}
		
		 
		//initDataTest();
	}
	
	/*private void initDataTest(){
		List<RecommandGameInfo> gameInfos = new ArrayList<RecommandGameInfo>();
		for(int i = 0 ; i<6 ; i++){
			String name = "游戏" + i;
			String reason = "推荐理由"+i;
			String id = ""+i;
			RecommandGameInfo gameInfo = new RecommandGameInfo();
			gameInfo.setName(name);
			gameInfo.setReason(reason);
			gameInfo.setGameId(id);
			gameInfos.add(gameInfo);
		}
		
		adapter.setData(gameInfos);		
	}*/
	
	private OnClickListener getAddGameListener() {
		return  new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new NewGameSubmit(ActivityAddNewGame.this).execute();
			}
		};
	}
	
	@Override
	protected int getLayoutResID() {
		return R.layout.activity_add_game;
	}
	
	@Override
	protected LoadMoreRequestor<RecommandGameInfo> createLoadMoreRequestor() {
		return new GameInfosLoadMore(this);
	}

	@Override
	protected RefreshRequestor<RecommandGameInfo> createRefreshRequestor() {
		return new GameInfosRefresh(this);	
	}

	private class GameInfosRefresh extends RefreshRequestor<RecommandGameInfo>{

		public GameInfosRefresh(ActivityListRefreshBase<RecommandGameInfo> activity) {
			super(activity);			
		}
	
		@Override
		protected void setData(byte[] body)
				throws InvalidProtocolBufferException {	
			List<RecommandGameInfo> gameInfos = ActivityAddNewGame.doParse(body);
			 
			if(gameInfos == null ){
				if(adapter.getCount()==0){
					onGameIsLoading();
					return;
				}else{
					UIUtils.showToast(R.string.refresh_games_info_neterror);
					return;
				}
			}			
			
			DataCache.getRecommandgameinfoscache().setData(gameInfos);
			
			int size = gameInfos.size();
			if((adapter.getCount()==size)&&(size>0)){
				UIUtils.showToast(R.string.already_latest);
				return;
			}
					
			List<RecommandGameInfo> adapterGameInfos = new ArrayList<RecommandGameInfo>();
			adapterGameInfos.addAll(gameInfos);		
			for(RecommandGameInfo gameInfo : adapterGameInfos){
				if(checkRepetedGameInfo(gameInfo.getName())){
					adapterGameInfos.remove(gameInfo);
				}
			}
				
			if(adapterGameInfos.isEmpty()){
				onAllGameInstalled();
				return ;
			}
			
			ActivityListRefreshBase<RecommandGameInfo> ref = activityRef.get();
			if(ref!=null){
				ref.setData(adapterGameInfos);
			}
		}

		@Override
		protected boolean doRequest() {			
			 return HttpApiService.requestGameList(this);
		}

		@Override
		protected void clearCache() {
			DataCache.getRecommandgameinfoscache().clear();
		} 
	}
	
	private class GameInfosLoadMore extends LoadMoreRequestor<RecommandGameInfo>{

		public GameInfosLoadMore(ActivityListRefreshBase<RecommandGameInfo> activity) {
			super(activity);			
		}

		@Override
		protected boolean doRequest() {			
			return HttpApiService.requestGameList(this);
		}

		@Override
		protected void setData(byte[] body)
				throws InvalidProtocolBufferException {			
			List<RecommandGameInfo> gameInfos = ActivityAddNewGame.doParse(body);
			if(gameInfos == null ){
				if(adapter.getCount()==0){
					onGameIsLoading();
					return;
				}else{
					UIUtils.showToast(R.string.refresh_games_info_neterror);
					return;
				}
			}					
			
			DataCache<RecommandGameInfo> recommandgameinfoscache = DataCache.getRecommandgameinfoscache();
			recommandgameinfoscache.setData(gameInfos);
			
			int size = gameInfos.size();
			if((adapter.getCount()==size)&&(size>0)){
				UIUtils.showToast(R.string.already_latest);
				return;
			}			
			
			if(gameInfos.isEmpty()){
				if(adapter.getCount()>0){
					UIUtils.showToast(R.string.already_latest);
					return;
				}
				gameInfos = recommandgameinfoscache.getCacheData();
				checkAndSetCacheData(gameInfos);
			}else{	
				
				
				List<RecommandGameInfo> adapterGameInfos = new ArrayList<RecommandGameInfo>();
				adapterGameInfos.addAll(gameInfos);		
				for(RecommandGameInfo gameInfo : adapterGameInfos){
					if(checkRepetedGameInfo(gameInfo.getName())){
						adapterGameInfos.remove(gameInfo);
					}
				}
					
				if(adapterGameInfos.isEmpty()){
					onAllGameInstalled();
					return ;
				}
				
				getListAdapter().setData(adapterGameInfos);
			}
							
		}		
		
	}
	
	@Override
	public void onFailure() {
		onDataLoadFailed();		 
	}


	@Override
	public void onNetworkUnavailable() {
		onNetError();		 
	}

	@Override
	public void OnClickDownloadButton(View parentView, String url, RecommandGameInfo recommandGameInfo) {
		
		String name = recommandGameInfo.getName();
		if(checkDownLoaded(name)){
			Toast.makeText(this, "您已经下载过游戏"+name+"！", Toast.LENGTH_SHORT).show();
			return ;
		}
		
		UIUtils.showToast(R.string.downloading);
		 
		for(int i = 0 ; i<1000 ;i++){
			System.out.println("qinyanjun : i = "+i);
		}
			 
		 
		UIUtils.showToast(recommandGameInfo.getName()+"安装已完成!");		 
		adapter.removeItem(recommandGameInfo);
						
		if(adapter.getCount()==0){
			onAllGameInstalled();
		}
		
		/*Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(recommandGameInfo.getPackageUrl()));
		startActivity(intent);*/
		
		//UIUtils.showToast(R.string.downloading);
	}
	
	@Override
	protected void onStart() {		
		TriggerManager.getInstance().addObserver(eventObserver);	
		super.onStart();
	}

	@Override
	protected void onStop() {		
		TriggerManager.getInstance().deleteObserver(eventObserver);
		super.onStop();
	}
	
	public static List<RecommandGameInfo> doParse(byte[] body) throws InvalidProtocolBufferException {	
		GetGameListResponse parse = GetGameListResponse.parseFrom(body);
		if(0==parse.getResultCode()){
			List<RecommandGameInfo>  gameInfos = new ArrayList<RecommandGameInfo>();
			int gameCount = parse.getGameCount();
			if(gameCount==0){			
				return null;
			}
			
			for(int i = 0 ;i<gameCount ; i++){
				Game game = parse.getGame(i);
				
				RecommandGameInfo gameInfo = new RecommandGameInfo();
				gameInfo.setSequenceNum(game.getSequenceNum());
				gameInfo.setGameId(game.getGameId());
				gameInfo.setName(game.getName());
				gameInfo.setIconUrl(game.getIconUrl());
				gameInfo.setReason(game.getDescription());
				gameInfo.setPackageUrl(game.getPackageUrl());
				gameInfos.add(gameInfo);				 
			}
			
			Collections.sort(gameInfos);
				 
			return gameInfos;
		}
		
		return null;
	}
	
	private static boolean checkRepetedGameInfo(String name){
		 for(GameInfo installedGameInfo :installedGameInfos){
			 if(installedGameInfo.getAppLabel().equals(name)){
				 return true;
			 }
		 }
		 
		 return false ;
	}
	
	private void onNetError(){
		
		if(isNetConnected()){
			if(adapter.getCount()==0){		 
				addAllGame.setVisibility(View.GONE);			
				textLoadFailed.setVisibility(View.VISIBLE);
				textGameIsLoading.setVisibility(View.GONE);
				textLoadFailed.setText(getResources().getString(R.string.refresh_games_info_neterror));
			}else{
				UIUtils.showToast(R.string.refresh_games_info_neterror);
			}
		}else{
			if(adapter.getCount()==0){		 
				addAllGame.setVisibility(View.GONE);			
				textLoadFailed.setVisibility(View.VISIBLE);
				textGameIsLoading.setVisibility(View.GONE);
				textLoadFailed.setText(getResources().getString(R.string.net_disconnected));
			}else{
				UIUtils.showToast(R.string.net_disconnected);
			}
		}
		 
	}
	
	private void onDataLoadFailed(){
		if(adapter.getCount()==0){		 
			addAllGame.setVisibility(View.GONE);			
			textLoadFailed.setVisibility(View.VISIBLE);
			textGameIsLoading.setVisibility(View.GONE);
			textLoadFailed.setText(getResources().getString(R.string.refresh_games_info_server_exception));
		}else{
			UIUtils.showToast(R.string.refresh_games_info_server_exception);
		}
	}
	
	private void onGameIsLoading(){
		listView.setVisibility(View.GONE);
		addAllGame.setVisibility(View.GONE);			
		textLoadFailed.setVisibility(View.GONE);
		textGameIsLoading.setVisibility(View.VISIBLE);
	}
	
    private void onGameInstalled(String name){
		
		int count = adapter.getCount();
		if(count==0){
			return ;
		}
			
	    for(int i = 0 ; i<count ;i++){
			RecommandGameInfo gameInfo = adapter.getItem(i);
			if(gameInfo.getName().equals(name)){
				adapter.removeItem(gameInfo);				 
				break;
			}
		}
			
		if(adapter.getCount()==0){
			onAllGameInstalled();
		}		 
				 
	}
	
	private void onGameUninstalled(String packageName){
		 
		GameInfo gameInfo = GameManager.getInstance().getGameInfo(packageName);
		if(gameInfo==null){
			return;
		}
		String name = gameInfo.getAppLabel();
		if (TextUtils.isEmpty(name)) {
			return;
		}
		
		SharedPreferences sharedPreferences ;
		if(UserSession.isLogined()){		
			sharedPreferences = getApplicationContext().getSharedPreferences(
					"NewGameInstalledInfos"+UserSession.getInstance().getUserId(), 0);
		}else{
			sharedPreferences = getApplicationContext().getSharedPreferences(
					"NewGameInstalledInfos", 0);
		}
		SharedPreferences.Editor editor = sharedPreferences.edit();
		
		long time = sharedPreferences.getLong(name, 0);
		if(time!=0){
			editor.remove(name);
			editor.commit();
		}
		
		List<RecommandGameInfo> cacheData = DataCache.getRecommandgameinfoscache().getCacheData();
		if(cacheData.isEmpty()){
			doRefresh();
		}else{
			
			for(RecommandGameInfo game: cacheData){
				if(name.equals(game.getName())){
					List<RecommandGameInfo> gameList = new ArrayList<RecommandGameInfo>();
					gameList.add(game);
					adapter.addData(gameList);
					return;
				}
			}
		}		
	}
	
	private void onAllGameInstalled(){
		listView.setVisibility(View.GONE);
		addAllGame.setVisibility(View.VISIBLE);
		textLoadFailed.setVisibility(View.GONE);
		textGameIsLoading.setVisibility(View.GONE);
	}
	 
	private boolean checkDownLoaded(String name){
		 
		SharedPreferences sharedPreferences;
		if(UserSession.isLogined()){
			sharedPreferences = getApplicationContext().getSharedPreferences(
					"NewGameInstalledInfos"+UserSession.getInstance().getUserId(), 0);
		}else{
			sharedPreferences = getApplicationContext().getSharedPreferences(
					"NewGameInstalledInfos", 0);
		}
		
		SharedPreferences.Editor editor = sharedPreferences.edit();
		
		long time = sharedPreferences.getLong(name, 0);
		
		if(time == 0){
			editor.putLong(name, System.currentTimeMillis());
			editor.commit();
			Statistic.addEvent(this, Statistic.Event.USER_FIRST_DOWNLOAD_GAME);
			return false;
		}else{
			return true;
		}  
	}
	
	private void checkAndSetCacheData(List<RecommandGameInfo> cacheData){
		
		if(cacheData==null||cacheData.isEmpty()){
			return ;
		}
		
		List<RecommandGameInfo> gameInfos = new ArrayList<RecommandGameInfo>();
		gameInfos.addAll(cacheData);
		
		for(RecommandGameInfo gameInfo : gameInfos){
			if(checkRepetedGameInfo(gameInfo.getName())){
				gameInfos.remove(gameInfo);
			}
		}
		
		if(gameInfos.isEmpty()){
			onAllGameInstalled();
		}else{
			setData(cacheData);
		}
	}
	
	private boolean isNetConnected(){
		
		boolean isNetConnected = false; 
		
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);	
		NetworkInfo  mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo  wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        		
		boolean bWIFIIsConnected = false;
		boolean bMobNetIsConnected = false;
        if( !wifiNetInfo.isConnected()) 
        {
        	bWIFIIsConnected = false;
        	
        	if(mobNetInfo!=null)
        	{
        		if (!mobNetInfo.isConnected() )
      	        {
      	        	bMobNetIsConnected = false;
      	        }
      	        else 
      	        {
      	        	bMobNetIsConnected = true;
      	        }
        	}
        	else
        	{
        		bMobNetIsConnected = false;
        	}  	

        }else {
        	bWIFIIsConnected = true;
        }
        
        isNetConnected = (bWIFIIsConnected||bMobNetIsConnected);
        
        return isNetConnected;
	}
	
	
}
