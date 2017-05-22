package cn.wsds.gamemaster.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.pb.Proto;
import cn.wsds.gamemaster.pb.Proto.ProcessCleanRecords.Builder;
import cn.wsds.gamemaster.tools.SystemInfoUtil;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.data.InstalledAppInfo;
import com.subao.utils.FileUtils;


/**
 * 进程清理名单列表
 *  包含黑白名单
 */
public class ProcessCleanRecords {
	
	/**数据文件名*/
	private static final String RECORD_DATA_FILE = "process_clean.data";
	private static final ProcessCleanRecords instance = new ProcessCleanRecords();
	/** 清理名单（黑名单和白名单） */
	private final HashMap<String,Record> cleanRecords = new HashMap<String,Record>();
	/** 针对手机每个应用的记录 */
	private final HashMap<String,Record> records = new HashMap<String,Record>();
	/**建议保留名单管理*/
	private final SuggestRecords suggestRecords = new SuggestRecords();
	/**
	 *  是否需要保存
	 *   初始值及数据保存成功时为false
	 *   数据有改动时为true
	 */
	private boolean needSave;
	
	private ProcessCleanRecords() {}
	public static ProcessCleanRecords getInstance() {
		return instance;
	}

	/**
	 * 保存
	 * @return
	 * true 保存成功 false 保存失败 或者不需要保存
	 */
	public boolean save(){
		if(!needSave)
			return false;
		Builder b = Proto.ProcessCleanRecords.newBuilder();
		Collection<Record> records = cleanRecords.values();
		for (Record record : records) {
			b.addRecord(record.serial());
		}
		byte[] data = b.build().toByteArray();
		File file = FileUtils.getDataFile(RECORD_DATA_FILE);
		boolean write = FileUtils.write(file, data);
		if(write){
			needSave = false;
		}
		return write;
	}
	
	/**
	 * 应用黑白名单及应用详情信息实体
	 */
	public static final class Record{
		private AppFlag flag;//应用标记
		private String label;
		/** 包名 */
		private String packageName;
		/** 是否可以被清理 */
		private boolean clean;
		private boolean init;
		
		private void load(cn.wsds.gamemaster.pb.Proto.ProcessCleanRecord record){
			packageName = record.getPackageName();
			clean = record.getClean();
		}
		private cn.wsds.gamemaster.pb.Proto.ProcessCleanRecord serial(){
			cn.wsds.gamemaster.pb.Proto.ProcessCleanRecord.Builder record = cn.wsds.gamemaster.pb.Proto.ProcessCleanRecord.newBuilder();
			record.setPackageName(packageName);
			record.setClean(clean);
			return record.build();
		}
		/**
		 * 应用标记
		 *  建议保留、建议清理
		 * @see #AppFlag.suggestProtected
		 * @see #AppFlag.suggestClean
		 */
		public AppFlag getFlag() {
			return flag;
		}
		/**
		 * 应用包名
		 * @return
		 */
		public String getPackageName(){
			return this.packageName;
		}
		/**
		 * 是否可以被清理
		 * @return
		 * true 可以被清理 false 不要清理
		 */
		public boolean clean(){
			return this.clean;
		}
		
		public void setClean(boolean clean){
			this.clean = clean;
		}
		/**
		 * 应用标签
		 * @return
		 */
		public String getLabel() {
			return label;
		}
		public boolean isInit() {
			return init;
		}
	}
	
	/**
	 * 应用标记
	 */
	public static enum AppFlag{
		/**该标记下的应用建议保留*/
		suggestProtected("建议保留"),
		/**该标记下的应用建议清理*/
		suggestClean();
		private String desc = "";
		private AppFlag() {
		}
		private AppFlag(String desc){
			this.desc = desc;
		}
		public String getDesc() {
			return desc;
		}
	}
	
	/**
	 * 去除卸载应用
	 */
	public void filterUninstallApp(){
		Set<String> keySet = new HashSet<String>(cleanRecords.keySet());
		InstalledAppInfo[] installedApps = GameManager.getInstance().getInstalledApps();
		for (InstalledAppInfo installedAppInfo : installedApps) {
			String packageName = installedAppInfo.getPackageName();
			keySet.remove(packageName);
		}
		
		if(keySet.isEmpty()){
			return;
		}
		needSave = true;
		for (String key : keySet) {
			cleanRecords.remove(key);
		}
	}
	
	/**
	 * 通过安装信息及建议保存列表黑白名单获得除系统应用和支持的游戏以外所有的应用信息
	 * @return
	 */
	public List<Record> getAllRecord(){
		Context context = AppMain.getContext();
		List<Record> records = new ArrayList<Record>();
		InstalledAppInfo[] installedApps = GameManager.getInstance().getInstalledApps();
		for (InstalledAppInfo installedAppInfo : installedApps) {
			if(isSupportGame(installedAppInfo.getPackageName()))//支持的应用不处理
				continue;
			Record record = createRecord(context, installedAppInfo);
			records.add(record);
		}
		return records;
	}
	
	/**
	 * 通过安装信息和黑白名单信息创建一个记录
	 * @param context
	 * @param installedAppInfo
	 * @return
	 */
	private Record createRecord(Context context,InstalledAppInfo installedAppInfo) {
		String appLabel = installedAppInfo.getAppLabel();
		String pn = installedAppInfo.getPackageName();
		Record record = cleanRecords.get(pn);
		if(record == null)//如果黑白名单为空则读所有的
			record = records.get(pn);
		if(record==null){//所有的预加载没有说明这是一个新应用则需要重新创建
			record = new Record();
			record.packageName = pn;
			boolean canClean = canClean(pn,appLabel);
			record.clean = canClean;
			records.put(pn, record);
		}
		
		if(!record.isInit()){
			record.init = true;
			record.flag = getRecordFlag(pn,appLabel);
			record.label = appLabel;
		}
		return record;
	}
	
	private AppFlag getRecordFlag(String pn, String appLabel) {
		return suggestRecords.contains(pn, appLabel) ? AppFlag.suggestProtected : AppFlag.suggestClean;
	}
	
	/**
	 * 获得去除白名单之外的活跃进程
	 * 
	 * @param runningAppList
	 *            当前正在运行的APP列表，如果为null，本函数会自己再去取一次
	 * 
	 * @return 返回进程包名集合，没有则返回长度为0的集合
	 */
	public Set<String> getCleanRecord(List<AppProfile> runningAppList) {
//		Set<String> cleanRecord = new HashSet<String>();
//		List<String> runningApps = SystemInfoUntil.getRunningApp();
//		InstalledAppInfo[] installedApps = GameManager.getInstance().getInstalledApps();
//		for (String packageName : runningApps) {
//			InstalledAppInfo installInfo = getInstallInfo(installedApps, packageName);
//			if(installInfo==null)//info 为空说明应用为系统应用
//				continue;
//			if(canClean(packageName,installInfo.getAppLabel()))
//				cleanRecord.add(packageName);
//		} 
//		return cleanRecord;
		
		Set<String> cleanRecord = new HashSet<String>();
		if (runningAppList == null) {
			runningAppList = SystemInfoUtil.getRunningAppList();
		}
		if (runningAppList != null) {
			for (AppProfile ap : runningAppList) {
				if (canClean(ap.packageName, ap.appLabel)) {
					cleanRecord.add(ap.packageName);
				}
			}
		}
		return cleanRecord;
	}
	

	/**
	 * 是否可以被清理
	 * @param packageName
	 * @param label 
	 * @return
	 */
	private boolean canClean(String packageName, String label) {
		Record record = cleanRecords.get(packageName);
		if(record == null){// 黑白名单不包含该记录
			if(suggestRecords.contains(packageName, label)){
				return false;
			}else{
				return !isSupportGame(packageName);//不属于支持的游戏
			}
		}else{
			return record.clean;//属于黑名单
		}
	}
	private boolean isSupportGame(String packageName) {
		return GameManager.getInstance().getGameInfo(packageName) != null;
	}
	
	/**
	 * 添加黑名单
	 * @param packageName 包名
	 */
	public void addBlacklist(String packageName){
		needSave = true;
		addRecord(packageName,true);
	}
	
	/**
	 * 添加白名单
	 * @param packageName 包名
	 */
	public void addWhitelist(String packageName, String label){
		needSave = true;
		if(suggestRecords.contains(packageName, label)){
			cleanRecords.remove(packageName);
		}else{
			addRecord(packageName,false);
		}
	}

	private void addRecord(String packageName,boolean clean) {
		Record record = new Record();
		record.packageName = packageName;
		record.clean = clean;
		cleanRecords.put(packageName, record);
	}
	
	/**
	 * 加载本地列表
	 * @return 
	 */
	private boolean loadLocal(){
		File file = FileUtils.getDataFile(RECORD_DATA_FILE);
		byte[] data = FileUtils.read(file);
		if (data == null) {
			return false;
		}
		cleanRecords.clear();
		cleanRecords.putAll(loadData(data));
		return cleanRecords.isEmpty();
	}
	
	private HashMap<String,Record> loadData(byte[] data) {
		HashMap<String,Record> hashMap = new HashMap<String,Record>();
		try {
			Proto.ProcessCleanRecords records = Proto.ProcessCleanRecords.parseFrom(data);
			List<cn.wsds.gamemaster.pb.Proto.ProcessCleanRecord> recordList = records.getRecordList();
			for (cn.wsds.gamemaster.pb.Proto.ProcessCleanRecord record:recordList) {
				Record r = new Record();
				r.load(record);
				hashMap.put(record.getPackageName(), r);
			}
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		return hashMap;
	}

	/**
	 */
	private void loadRecord() {
		Context context = AppMain.getContext();
		InstalledAppInfo[] installedApps = GameManager.getInstance().getInstalledApps();
		records.clear();
		for (InstalledAppInfo installedAppInfo : installedApps) {
			String packageName = installedAppInfo.getPackageName();
			if(isSupportGame(packageName))//支持的应用不处理
				continue;
			Record record = createRecord(context, installedAppInfo);
			records.put(packageName, record);
		}
	}

	/**
	 * 初始化
	 */
	public void init() {
		loadLocal();
		loadRecord();
	}
}

/**
 * 建议保留名单
 */
class SuggestRecords {
	
	/** 建议保留包名集合*/
	private final List<String> packageNames = new ArrayList<String>();
	/** 建议保留应用名集合*/ 
	private final List<String> labels = new ArrayList<String>();
	public SuggestRecords() {
		loadPackgaeName();
		loadLabels();
	}

	/**
	 * 加载建议保留包名集合
	 */
	private void loadLabels() {
		labels.add("天气");
		labels.add("闹钟");
		labels.add("输入法");
		labels.add("拼音");
		labels.add("日历");
		labels.add("日程");
		labels.add("提醒");
		labels.add("通知");
		labels.add("短信");
		labels.add("信息");
		labels.add("通讯录");
		labels.add("电话本");
	}
	/**
	 * 加载建议保留应用名集合
	 */
	private void loadPackgaeName() {
		packageNames.add("com.tencent.mobileqq");
		packageNames.add("com.tencent.eim");
		packageNames.add("com.tencent.qqlite");
		packageNames.add("com.tencent.mobileqqi");
		packageNames.add("com.tencent.minihd.qq");
		packageNames.add("com.tencent.hd.qq");
		packageNames.add("com.tencent.mobileqq");
		packageNames.add("com.tencent.android.pad");
		packageNames.add("com.tencent.qq.kddi");
		packageNames.add("com.tencent.mm");
		packageNames.add("com.tencent.WBlog");
		packageNames.add("com.sina.weibo");
		packageNames.add("com.sina.weibog3");
		packageNames.add("com.tencent.microblog");
		packageNames.add("com.sina.weibotab");
	}

	
	/**
	 * 是否包含于保留名单里面
	 * @param packageName 包名
	 * @return
	 */
	public boolean contains(String packageName,String label){
		boolean matchPackageName = matchPackageName(packageName);
		if(matchPackageName){
			return true;
		}
		return matchLabel(label);
	}

	/**
	 * 验证包名匹配
	 * @param packageName
	 * @return
	 */
	private boolean matchPackageName(String packageName) {
		for (String pn : packageNames) {
			if(pn.equals(packageName)){
				return true;
			}
		}
		return false;
	}

	/**
	 * 验证应用名匹配
	 * @param label
	 * @return
	 */
	private boolean matchLabel(String label) {
		for (String l : labels) {
			if(label.contains(l)){
				return true;
			}
		}
		return false;
	}

}
