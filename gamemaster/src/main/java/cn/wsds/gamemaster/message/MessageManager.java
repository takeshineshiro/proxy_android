package cn.wsds.gamemaster.message;

import android.content.res.Resources;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.subao.common.data.ParallelConfigDownloader;
import com.subao.utils.FileUtils;
import com.subao.utils.Misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.UUID;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;

public class MessageManager extends Observable {

	private static final String FILENAME = "msg";

	private static final String FILENAME_OF_LOCAL_RECORD_CREATED = "msg_local";

	/** 消息记录ID：特殊的本地消息，点击跳转到帮助页面 */
	private static final int ID_GOTO_HELP = -1;
	/** 消息记录ID：特殊的本地消息，点击跳转到防清理页面 */
	private static final int ID_GOTO_PREVENT_CLEAN = -2;
	/** 消息记录ID：特殊的本地消息，点击跳转到常见问题 */
	private static final int ID_GOTO_QA = -3;
	/** 消息记录ID：特殊的本地消息，点击跳转到通知内嵌SDK的游戏支持加速 */
	private static final int ID_GOTO_NOTIFY_SDKEMBEDGAME_SUPPORT = -4;
	/** 消息记录ID：特殊的本地消息，点击跳转到通知IOS版上线 */
	private static final int ID_GOTO_NOTIFY_IOS_RELEASE = -5;
	/** 消息记录ID：特殊的本地消息，点击跳转到问卷调查页 */
	private static final int ID_GOTO_QUESTION_SUVERY = -6;
    /** 消息记录ID：特殊的本地消息，点击跳转到“APP改名啦” */
	private static final int ID_GOTO_APP_RENAME = -7;

    /** 消息记录ID：特殊的本地消息，点击跳转到“双链路介绍页” */
    public static final int ID_GOTO_DOUBLE_LINK = -8;
    
	private static final MessageManager instance = new MessageManager();

	public static MessageManager getInstance() {
		return instance;
	}

	/**
	 * 消息记录
	 */
	public static class Record {

		/** 记录类型：本地信息图文混排 */
		public static final int TYPE_GRAPHICS_TEXT_MIXED = -5;
		/**
		 * 记录类型：特殊的本地消息，点击跳转到反馈回复查看页面
		 */
		public static final int TYPE_GOTO_FEEDBACK_REPLY = -4;

        public static final int TYPE_GOTO_DOUBLE_LINK = -8;
        /**
         * 记录类型：点击跳转到问卷调查查看页面
         */
//        public static final int TYPE_GOTO_QUESTION_SUVERY = -6;
        
        /** 记录类型：点击跳转到APP更名 */
        public static final int TYPE_APP_RENAME = -7;
        
		/**
		 * 记录类型：本地信息，跳转到“常见问题”页面
		 * <p>
		 * <b>这个值的定义因为笔误写成03了，其实应该是-3。为了兼容以前的版本，所以这里不做修正</b>
		 * </p>
		 */
		public static final int TYPE_GOTO_QA = 03;

		/** 记录类型：本地信息防清理 */
		public static final int TYPE_PREVENT_CLEAN = -2;

		/** 记录类型：本地帮助信息 */
		public static final int TYPE_HELP = -1;

		/** 记录类型：超链接 */
		public static final int TYPE_URL = 1;

		/** 记录类型：HTML或纯文本 */
		public static final int TYPE_HTML = 2;
		
		/** 记录类型：极光通知文本及超链接 */
		public static final int TYPE_JPUSH_NOTIFY_URL = 4;
		
		/** 记录类型：通知文本 */
		public static final int TYPE_JPUSH_NOTIFY_TEXT = 5;

		public final int id;			// 唯一ID。如果消息来自服务器，ID为正；如果来自本地，ID为负。
		public final int type;			// 类型，参见类型定义
		public final long time;			// 消息生成时间，UTC毫秒值
		public final String title;		// 消息标题
		public final String content;	// 内容
		public final String extra;		// 附加内容

		private boolean read;			// 是否已读

		/**
		 * 如果本消息已经读过，返回true，否则返回false
		 */
		public boolean isRead() {
			return this.read;
		}

		public Record(int id, int type, long time, String title, String content, String extra, boolean read) {
			this.id = id;
			this.type = type;
			this.time = time;
			this.title = title;
			this.content = content;
			this.extra = extra;
			this.read = read;
		}

		private boolean markToRead() {
			if (this.read) {
				return false;
			} else {
				this.read = true;
				return true;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (o instanceof Record) {
				Record rec = (Record) o;
				return this.id == rec.id && this.type == rec.type && this.time == rec.time && this.read == rec.read
					&& com.subao.common.utils.StringUtils.isStringEqual(this.title, rec.title)
					&& com.subao.common.utils.StringUtils.isStringEqual(this.content, rec.content);
			} else {
				return false;
			}
		}
	}

	/**
	 * 负责将{@link Record}序列化的类
	 */
	public static class RecordSerializer {

		private static final String JSON_KEY_READ = "Read";

		private static final String JSON_KEY_CONTENT = "Content";

		private static final String JSON_KEY_TITLE = "Title";

		private static final String JSON_KEY_TIME = "Time";

		private static final String JSON_KEY_TYPE = "Type";

		private static final String JSON_KEY_ID = "Id";

		private static final String JSON_KEY_EXTRA = "extra";

		/**
		 * 将Record序列化到JsonWriter
		 */
		public static boolean writeToJson(Record record, JsonWriter writer) {
			try {
				writer.beginObject();
				writer.name(JSON_KEY_ID).value(record.id);
				writer.name(JSON_KEY_TYPE).value(record.type);
				writer.name(JSON_KEY_TIME).value(record.time);
				if (record.extra != null) {
					writer.name(JSON_KEY_EXTRA).value(record.extra);
				}
				if (record.title != null) {
					writer.name(JSON_KEY_TITLE).value(record.title);
				}
				if (record.content != null) {
					writer.name(JSON_KEY_CONTENT).value(record.content);
				}
				if (record.read) {
					writer.name(JSON_KEY_READ).value(record.read);
				}
				writer.endObject();
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		/**
		 * 从JsonReader创建一个Record
		 */
		public static Record createFromJson(JsonReader reader) {
			int id = 0, type = 0;
			String title = null, content = null;
			boolean read = false;
			long time = 0;
			String extra = null;
			try {
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (JSON_KEY_ID.equals(name)) {
						id = reader.nextInt();
					} else if (JSON_KEY_TYPE.equals(name)) {
						type = reader.nextInt();
					} else if (JSON_KEY_TITLE.equals(name)) {
						title = reader.nextString();
					} else if (JSON_KEY_CONTENT.equals(name)) {
						content = reader.nextString();
					} else if (JSON_KEY_READ.equals(name)) {
						read = reader.nextBoolean();
					} else if (JSON_KEY_TIME.equals(name)) {
						time = reader.nextLong();
					} else if (JSON_KEY_EXTRA.equals(name)) {
						extra = reader.nextString();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			} catch (RuntimeException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			if (id != 0 && type != 0) {
				switch (id) {
				case ID_GOTO_NOTIFY_SDKEMBEDGAME_SUPPORT:
				case ID_GOTO_QUESTION_SUVERY:
					return null;
				default:
					return new Record(id, type, time, title, content, extra, read);
				}
			} else {
				return null;
			}
		}

	}

	/**
	 * 消息列表 所有{@link Record}按ID的倒序,且负值ID前置;
	 */
	public static class RecordList implements Iterable<Record> {

		public interface Saver {
			public void save(RecordList recordList);
		}

		private final Saver saver;
		private final List<Record> list = new ArrayList<Record>(8);

		RecordList(Saver saver) {
			this.saver = saver;
		}

		RecordList(Saver saver, JsonReader reader) {
			this(saver);
			loadFromJson(reader);
		}

		private void loadFromJson(JsonReader reader) {
			list.clear();
			try {
				reader.beginArray();
				while (reader.hasNext()) {
					Record rec = RecordSerializer.createFromJson(reader);
					if(rec.id==Record.TYPE_APP_RENAME){
						continue;
					}
					this.add(rec, false);
				}
				reader.endArray();
			} catch (RuntimeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (!list.isEmpty()) {
				this.save();
			}
		}

		private void raiseMessageManagerChanged() {
			MessageManager manager = MessageManager.getInstance();
			if (manager != null && this == manager.recordList) {
				manager.setChanged();
				manager.notifyObservers(this);
			}
		}

		public void save() {
			if (saver != null) {
				saver.save(this);
			}
		}

		private boolean doAdd(Record rec) {
			int idx = indexOf(rec.id);
			if (idx >= 0) {
				return false;
			} else {
				list.add(~idx, rec);
				return true;
			}
		}

		/**
		 * 添加一条记录
		 * 
		 * @param rec
		 *            {@link Record}
		 * @param autoSaveWhenChanged
		 *            true表示“如果成功添加，则自动调用save()函数存盘”
		 * @return true表示添加成功，false表示失败（相同ID的记录已存在）
		 */
		boolean add(Record rec, boolean autoSaveWhenChanged) {
			if (rec == null) {
				return false;
			}
			if (doAdd(rec)) {
				save();
				raiseMessageManagerChanged();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Iterator<Record> iterator() {
			return list.iterator();
		}

		/**
		 * 序列化
		 */
		public boolean writeToJson(JsonWriter writer) {
			try {
				writer.beginArray();
				for (Record rec : this.list) {
					RecordSerializer.writeToJson(rec, writer);
				}
				writer.endArray();
				return true;
			} catch (IOException e) {
				return false;
			}
		}

		/** 返回消息记录个数 */
		public int getCount() {
			return this.list.size();
		}

		/**
		 * 根据索引（下标）取记录
		 */
		public Record get(int index) {
			return this.list.get(index);
		}

		/**
		 * 从列表中删除指定ID的记录
		 * 
		 * @param recordId
		 *            记录ID
		 * @return 如果成功删除返回true，否则返回false
		 */
		public boolean deleteWithId(int recordId) {
			int index = indexOf(recordId);
			if (index >= 0) {
				list.remove(index);
				save();
				raiseMessageManagerChanged();
				return true;
			}
			return false;
		}

		//		/**
		//		 * 从列表中，根据索引删除记录
		//		 * 
		//		 * @param index
		//		 *            索引值
		//		 */
		//		public void deleteWithIndex(int index) {
		//			list.remove(index);
		//			save();
		//			raiseMessageManagerChanged();
		//		}

		/**
		 * 清除所有记录
		 * 
		 * @return 如果操作影响了容器则返回true，否则返回false
		 */
		public boolean clear() {
			if (!list.isEmpty()) {
				list.clear();
				save();
				raiseMessageManagerChanged();
				return true;
			}
			return false;
		}

		/**
		 * 将指定记录标记为“已读”
		 * 
		 * @param recordId
		 *            记录ID
		 * @return 标记成功返回true，失败（未找到记录或记录已经是“已读”）返回false
		 */
		public boolean markToRead(int recordId) {
			Record rec = getWithId(recordId);
			if (rec != null) {
				if (rec.markToRead()) {
					save();
					raiseMessageManagerChanged();
					return true;
				}
			}
			return false;
		}

		/**
		 * 列表是否为空？
		 * 
		 * @return 如果为空返回true，否则返回false
		 */
		public boolean isEmpty() {
			return list.isEmpty();
		}

		/**
		 * 根据指定的记录ID取{@link Record}
		 * 
		 * @param recordId
		 *            记录ID
		 * @return {@link Record}或null
		 */
		public Record getWithId(int recordId) {
			int index = indexOf(recordId);
			if (index >= 0) {
				return list.get(index);
			}
			return null;
		}

		/**
		 * 查找给定{@link Record}在列表中的下标
		 * 
		 * @param rec
		 *            要查找的{@link Record}，{@link Record#id}相同的视为相同记录
		 * @return 找到相同id时返回非负的下标值，否则返回一个负数，该负数按位取反后是应该插入的位置索引
		 */
		public int indexOf(Record rec) {
			return indexOf(rec.id);
		}

		/**
		 * 查找给定{@link Record#id}在列表中的下标
		 * 
		 * @param recordId
		 *            id
		 * @return 如果找到，返回非负的下标值；找不到返回一个负数，该负数按位取反后是应该插入的位置索引
		 */
		public int indexOf(int recordId) {
			if(list.size()==0){
				return -1 ;
			}
			
			int mid = criticalIndex() ;
			int midId = list.get(mid).id ;		
			int low = recordId>0 ? (midId>0 ? mid : mid+1) : 0 ;
			int high = recordId<0 ? (midId<0 ? mid : (mid-1)):(list.size()-1) ;
			int result = -1;
			
			while(low<=high){				
				mid = getMedian(low,high);
				midId = list.get(mid).id;
				result = midId - recordId;
					 
				if(recordId<0){
					result = -result ;
				}
				
				if (result > 0) {
					low = mid + 1;					
				} else if (result == 0) {
					return mid;
				} else {			 
					high = mid-1;	
				}
			}
			
			if(recordId<0){				
				return -mid - (result < 0 ? 1 : 2);
			}else{ 
				return -mid - (((midId>0)&&(result<0)) ? 1 : 2);
			}					
		}
		
		/**
		 * 查找给定{@link RecordList#list}列表中正负id的临界下标
		 *
		 * @return 如果列表中的id全负,则返回最大下标值,反之则返回最小下标值；
		 */
		private int criticalIndex(){   
			//TODO ： 此处及前面的方法都用到了二分法查找，
			//应该还有优化空间，在找到更优方案之前，暂时如此。
			int low = 0, mid = list.size(), high = mid - 1 ;
			
			while(low<=high){
				mid = getMedian(low,high);
				
				if(list.get(mid).id<0){
					low = mid +1;
				}else{
					high = mid -1 ;
				}
			}	
			return mid ;		
		}
		
		private int getMedian(int low , int high){
			return ((low+high)>>>1);
		}

		/**
		 * 是否至少含有一条未读记录
		 * 
		 * @return 如果至少有一条未读记录返回true，否则返回false
		 */
		public boolean hasUnread() {
			for (Record rec : list) {
				if (!rec.read) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * RecordList的序列化器实现
	 */
	private static class RecordListSaver implements RecordList.Saver {

		@Override
		public void save(RecordList recordList) {
			if (recordList == null) {
				return;
			}
			//
			JsonWriter writer = null;
			try {
				writer = new JsonWriter(new FileWriter(FileUtils.getDataFile(FILENAME), false));
				recordList.writeToJson(writer);
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				Misc.safeClose(writer);
			}
		}

	}

	private final RecordList recordList = new RecordList(new RecordListSaver());

	private MessageManager() {
		// 从本地加载RecordList
		loadRecordListFromLocal();
		// 看看有哪些需要生成的本地记录
		List<Integer> idListOfAlreadyCreated = loadIdListOfAlreadyCreated();
		if (createLocalRecords(recordList, idListOfAlreadyCreated)) {
			recordList.save();
			saveIdListOfAlreadyCreated(idListOfAlreadyCreated);
		}
	}

	/**
	 * 创建一条消息，指引用户点击后跳转到反馈回复查看页面
	 * 
	 * @param uuid
	 *            反馈的UUID
	 * @return true表示成功创建
	 */
	public boolean createLocalMessage_ShowFeedbackReply(UUID uuid) {	
		String uuidStr = uuid.toString();
		int min_id = -1000;
		for (Record rec : recordList) {
			if (rec.type == Record.TYPE_GOTO_FEEDBACK_REPLY) {
				if (uuidStr.equals(rec.extra)) {
					return false;
				}
			}
			if (rec.id < min_id) {
				min_id = rec.id;
			}
		}
		Record rec = new Record(min_id - 1, Record.TYPE_GOTO_FEEDBACK_REPLY, System.currentTimeMillis(), "您的反馈收到回复啦~",
			"点击查看", uuid.toString(), false);
		return recordList.add(rec, false);
	}
	
	/**
	 * 创建一条消息，对应收到的极光通知消息
	 * 
	 * @param  recordType：记录类型；
	 * @param  title ： 通知标题；
	 * @param  content ： 内容；
	 * @param  extra ： 附加信息；          
	 * @return true表示成功创建
	 */
	public boolean createLocalMessage_Notify(int recordType , String title , String content , String extra){
		int max_id = 0 ;	 
		
		for(Record rec : recordList){		
			if(max_id<rec.id){
				max_id = rec.id ;
			}
		}

		Record rec ; 
		if(Record.TYPE_JPUSH_NOTIFY_URL==recordType){
			rec = new Record(max_id+1, recordType, System.currentTimeMillis(), title,
					content, extra, false);
		}else{
			rec = new Record(max_id+1, recordType, System.currentTimeMillis(), title,
					"点击查看", extra, false);
		}
		  
	    return recordList.add(rec, false);
	}

	/**
	 * 根据需要创建本地消息
	 * 
	 * @param idListOfAlreadyCreated
	 *            历史上已经创建过的本地消息（不需要重复创建了）
	 * @return true表示创建了新的本地消息, false表示没有
	 */
	private static boolean createLocalRecords(RecordList recordList, List<Integer> idListOfAlreadyCreated) {
		boolean createInstructions = createLocalMessage_Instructions(recordList, idListOfAlreadyCreated);
		boolean createPreventClean = createLocalMessage_PreventClean(recordList, idListOfAlreadyCreated);
		//boolean createAppRename = createLocalMessage_AppRename(recordList, idListOfAlreadyCreated);
		boolean createGotoQA = createLocalMessage_GotoQA(recordList, idListOfAlreadyCreated);
		//boolean createGameSupport = createLocalMessage_GotoSdkembedgameSupport(recordList, idListOfAlreadyCreated);
		boolean createIOSRelease = createLocalMessage_GotoIOSRelease(recordList, idListOfAlreadyCreated);
        boolean createDoubelLink = false;
        if(ParallelConfigDownloader.isPhoneParallelSupported()) {
            createDoubelLink = createLocalMessage_DoubleLink(recordList, idListOfAlreadyCreated);
        }
		return createInstructions || createPreventClean || createGotoQA /*||createAppRename|| createGameSupport*/ 
				|| createIOSRelease || createDoubelLink;
	}

//    /**
//     * 根据需要创建本地消息
//	 * 1.5.4 去除该功能
//     * @return true表示创建了新的本地消息, false表示没有
//     */
//	@Deprecated
//    public static boolean createLocalMessage_QuestionSuvery() {
//        int id = ID_GOTO_QUESTION_SUVERY;
//        List<Integer> idListOfAlreadyCreated = loadIdListOfAlreadyCreated();
//        if (idListOfAlreadyCreated.indexOf(id) < 0) {
//            idListOfAlreadyCreated.add(id);
//            Record rec = new Record(id, Record.TYPE_GOTO_QUESTION_SUVERY, System.currentTimeMillis(), "有奖问卷调查",
//                    "点击查看", null, false);
//            return instance.recordList.add(rec, false);
//        }
//
//        return false;
//    }
    
//    public void markQuestionSuveryToRead() {
//        recordList.markToRead(ID_GOTO_QUESTION_SUVERY);
//    }
    
	private static boolean createLocalMessage_GotoIOSRelease(RecordList recordList, List<Integer> idListOfAlreadyCreated) {
		int id = ID_GOTO_NOTIFY_IOS_RELEASE;
		if (idListOfAlreadyCreated.indexOf(id) < 0) {
			idListOfAlreadyCreated.add(id);
			Record rec = new Record(id, Record.TYPE_URL, System.currentTimeMillis(), "IOS版极速来袭",
				"http://game.m.wsds.cn/ios.html", null, false);
			return recordList.add(rec, false);
		}
		return false;
	}

//	@Deprecated
//	private static boolean createLocalMessage_GotoSdkembedgameSupport(RecordList recordList,
//		List<Integer> idListOfAlreadyCreated) {
//		if (idListOfAlreadyCreated.indexOf(ID_GOTO_NOTIFY_SDKEMBEDGAME_SUPPORT) > -1) {
//			return false;
//		}
//		if (!GameManager.getInstance().hasSDKEmbedGameInstalled()) {
//			return false;
//		}
//		idListOfAlreadyCreated.add(ID_GOTO_NOTIFY_SDKEMBEDGAME_SUPPORT);
//		Record rec = new Record(ID_GOTO_NOTIFY_SDKEMBEDGAME_SUPPORT, Record.TYPE_GRAPHICS_TEXT_MIXED,
//			System.currentTimeMillis(), "内嵌加速的游戏支持加速啦~", "点击查看", null, false);
//		return recordList.add(rec, false);
//	}

	private static boolean createLocalMessage_Instructions(RecordList recordList, List<Integer> idListOfAlreadyCreated) {
		if (idListOfAlreadyCreated.indexOf(ID_GOTO_HELP) < 0) {
			idListOfAlreadyCreated.add(ID_GOTO_HELP);
			Resources res = AppMain.getContext().getResources();
			Record rec = new Record(ID_GOTO_HELP, Record.TYPE_HELP, System.currentTimeMillis(),
				res.getString(R.string.message_title_instructions), "", null, false);
			return recordList.add(rec, false);
		}
		return false;
	}

	private static boolean createLocalMessage_PreventClean(RecordList recordList, List<Integer> idListOfAlreadyCreated) {
		if (idListOfAlreadyCreated.indexOf(ID_GOTO_PREVENT_CLEAN) < 0) {
			idListOfAlreadyCreated.add(ID_GOTO_PREVENT_CLEAN);
			Resources res = AppMain.getContext().getResources();
			Record rec = new Record(ID_GOTO_PREVENT_CLEAN, Record.TYPE_PREVENT_CLEAN, System.currentTimeMillis(),
				res.getString(R.string.message_title_prevent_clean), "", null, false);
			return recordList.add(rec, false);
		}
		return false;
	}

	/*private static boolean createLocalMessage_AppRename(RecordList recordList, List<Integer> idListOfAlreadyCreated) {
		if (idListOfAlreadyCreated.indexOf(ID_GOTO_APP_RENAME) < 0) {
			idListOfAlreadyCreated.add(ID_GOTO_APP_RENAME);
			Resources res = AppMain.getContext().getResources();
			Record rec = new Record(ID_GOTO_APP_RENAME, Record.TYPE_APP_RENAME, System.currentTimeMillis(),
				res.getString(R.string.message_title_app_rename), "", null, false);
			return recordList.add(rec, false);
		}
		return false;
	}*/

    /**
     * 双链路本地消息
     * @param recordList
     * @param idListOfAlreadyCreated
     * @return
     */
    private static boolean createLocalMessage_DoubleLink(RecordList recordList, List<Integer> idListOfAlreadyCreated) {
        if (idListOfAlreadyCreated.indexOf(ID_GOTO_DOUBLE_LINK) < 0) {
            idListOfAlreadyCreated.add(ID_GOTO_DOUBLE_LINK);
            Resources res = AppMain.getContext().getResources();
            Record rec = new Record(ID_GOTO_DOUBLE_LINK, Record.TYPE_GOTO_DOUBLE_LINK, System.currentTimeMillis(),
                    res.getString(R.string.message_title_network_exception), "", null, false);
            return recordList.add(rec, false);
        }
        return false;
    }

    /**
     * 标记双链路消息以读
     */
    public void markMessageDoubleReaded(){
        recordList.markToRead(ID_GOTO_DOUBLE_LINK);
    }
	private static boolean createLocalMessage_GotoQA(RecordList recordList, List<Integer> idListOfAlreadyCreated) {
		if (idListOfAlreadyCreated.indexOf(ID_GOTO_QA) < 0) {
			idListOfAlreadyCreated.add(ID_GOTO_QA);
			Record rec = new Record(ID_GOTO_QA, Record.TYPE_GOTO_QA, System.currentTimeMillis(), "常见问题答疑", "点击查看",
				null, false);
			return recordList.add(rec, false);
		}
		return false;
	}
	
	/**
	 * APP更名消息是否阅读过？
	 */
	public boolean hasMessageAppRenameRead() {
		Record record = recordList.getWithId(ID_GOTO_APP_RENAME);
		if (record == null) {
			return true;
		}
		return record.read;
	}
	
	/**
	 * 令APP更名消息为“已阅读”
	 */
	public void markMessageAppRenameRead() {
		recordList.markToRead(ID_GOTO_APP_RENAME);
	}

	/**
	 * 从磁盘中载入“已创建过的本地记录ID列表”
	 * 
	 * @return ID列表（不会为null）
	 */
	private static List<Integer> loadIdListOfAlreadyCreated() {
		List<Integer> result = new ArrayList<Integer>(8);
		File file = FileUtils.getDataFile(FILENAME_OF_LOCAL_RECORD_CREATED);
		if (!file.exists()) {
			return result;
		}
		//
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
			byte[] buf = new byte[1024];
			int bytes = input.read(buf);
			if (bytes <= 0) {
				return result;
			}
			String[] fields = new String(buf, 0, bytes).split(",");
			if (fields.length == 0) {
				return result;
			}
			for (String s : fields) {
				try {
					int n = Integer.parseInt(s);
					if (result.indexOf(n) < 0) {
						result.add(n);
					}
				} catch (NumberFormatException e) {}
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Misc.safeClose(input);
		}
		return result;
	}

	/**
	 * 保存“已经创建过的本地消息列表”
	 */
	private static void saveIdListOfAlreadyCreated(List<Integer> list) {
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(FileUtils.getDataFile(FILENAME_OF_LOCAL_RECORD_CREATED), false);
			StringBuilder sb = new StringBuilder(list.size() << 2);
			for (Integer id : list) {
				sb.append(id).append(',');
			}
			output.write(sb.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Misc.safeClose(output);
		}
	}

	/**
	 * 从本地文件加载
	 */
	private boolean loadRecordListFromLocal() {
		JsonReader reader = null;
		try {
			reader = new JsonReader(new FileReader(FileUtils.getDataFile(FILENAME)));
			reader.setLenient(true);
			this.recordList.loadFromJson(reader);
			return true;
		} catch (IOException e) {
			return false;
		} catch (RuntimeException e) {
			return false;
		} finally {
			Misc.safeClose(reader);
		}
	}

	/**
	 * 返回RecordList
	 */
	public RecordList getRecordList() {
		return recordList;
	}

	public void addDebugCreateMessage(String title, String content, int type) {
		int count = recordList.getCount();
		int id = 0;
		if (count > 0) {
			id = recordList.get(count - 1).id + 1;
		}
		long timeInMillis = Calendar.getInstance().getTimeInMillis();
		recordList.add(new Record(id, type, timeInMillis, title, content, null, false), true);
	}

}
