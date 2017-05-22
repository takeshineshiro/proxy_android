package cn.wsds.gamemaster.data;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.common.net.Http;
import com.subao.net.NetManager;
import com.subao.utils.FileUtils;
import com.subao.utils.SubaoHttp;
import com.subao.utils.UrlConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.event.EventObserver;
import cn.wsds.gamemaster.event.TriggerManager;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.pb.Proto;
import cn.wsds.gamemaster.statistic.Statistic;

/**
 * 反馈数据
 */
public class UserFeedback {

	private static final boolean LOG = false;
	private static final String TAG = "UserFeedback";

	private static class Reply {
		public final UUID uuid;
		public final String content;
		public final int state;

		public Reply(Proto.UserFeedbackReply proto) {
			this.uuid = new UUID(proto.getUuid().getUuidMost(), proto.getUuid().getUuidLeast());
			this.content = proto.getContent();
			this.state = proto.getState();
		}
	}

	/** UUID */
	public final UUID uuid;

	/** 内容（反馈内容或回复内容） */
	public final String content;

	/** 联系方式 */
	public final String contact;

	private int flag;
	private final static int MASK_IS_REPLY = 1;
	private final static int MASK_REPLIED = 2;
	private final static int MASK_UNREAD = 4;

	/**
	 * 标记本对象为“已有回复”
	 */
	public synchronized void setReplied() {
		this.flag |= MASK_REPLIED;
	}

	/**
	 * 标记本对象为“已读”
	 */
	public synchronized void setRead() {
		this.flag &= ~MASK_UNREAD;
	}

	/**
	 * 本对象是一条回复吗？
	 * 
	 * @return
	 */
	public boolean isReply() {
		return 0 != (flag & MASK_IS_REPLY);
	}

	/**
	 * 本对象需要回复吗？
	 * 
	 * @return True表示本对象是一条“用户反馈”，并且尚未回复
	 */
	public boolean needReply() {
		return 0 == (flag & (MASK_IS_REPLY | MASK_REPLIED));
	}

	/**
	 * 本对象是否“已读”？
	 * 
	 * @return
	 */
	public boolean wasRead() {
		return 0 == (flag & MASK_UNREAD);
	}

	/**
	 * 取反馈内容。
	 * <p>
	 * 如果本条记录是一个反馈，返回content字段
	 * </p>
	 * <p>
	 * 如果本条记录是一个回复，返回对应的用户反馈的内容
	 * </p>
	 * 
	 * @return
	 */
	public String getFeedbackContent() {
		if (isReply()) {
			return this.contact;
		} else {
			return this.content;
		}
	}

	/**
	 * 根据UUID，生成客服ID
	 * @return 客服ID
	 */
	public int getServiceId() {
		return 1 + (int)(uuid.getMostSignificantBits() & 7);
	}
	
	/**
	 * 生成一条新的用户反馈，自动产生UUID
	 * 
	 * @param content
	 *            反馈内容
	 * @param contact
	 *            联系方式
	 */
	public static UserFeedback createFeedback(String content, String contact) {
		return new UserFeedback(UUID.randomUUID(), content, contact, 0);
	}

	/**
	 * 生成一条回复，自动产生UUID
	 *
	 *            回复内容
	 * @param feedbackContent
	 *            对应的用户反馈的内容
	 * @return
	 */
	public static UserFeedback createReply(Reply r, String feedbackContent) {
		return new UserFeedback(r.uuid, r.content, feedbackContent, MASK_IS_REPLY | MASK_UNREAD);
	}

	private UserFeedback(UUID uuid, String content, String contact, int flag) {
		this.uuid = uuid;
		this.content = content;
		this.contact = contact;
		this.flag = flag;
	}

	/**
	 * 从ProtoBuf生成
	 * 
	 * @param proto
	 */
	public UserFeedback(Proto.UserFeedback proto) {
		this(new UUID(proto.getUuid().getUuidMost(), proto.getUuid().getUuidLeast()), proto.getContent(), proto
			.getContact(), proto.getFlag());
	}

	public Proto.UserFeedback.Builder buildProtobuf() {
		return buildProtobuf(Proto.UserFeedback.newBuilder());
	}

	public Proto.UserFeedback.Builder buildProtobuf(Proto.UserFeedback.Builder builder) {
		Proto.UUID.Builder builderUUID = Proto.UUID.newBuilder();
		builderUUID.setUuidMost(uuid.getMostSignificantBits());
		builderUUID.setUuidLeast(uuid.getLeastSignificantBits());
		//
		builder.setUuid(builderUUID.build());
		builder.setContent(this.content);
		//
		if (!TextUtils.isEmpty(this.contact)) {
			builder.setContact(contact);
		}
		//
		builder.setFlag(this.flag);
		return builder;
	}

	// ///////////////////////////////////////////////////////

	/**
	 * 一个在“非主线程”里定时轮询的Handler
	 */
	public static class History extends Handler implements Iterable<UserFeedback> {

		private static final String FILE_NAME = ".uf.his.data";
		public static final History instance = new History();

		private EventObserver eventObserver = new EventObserver() {
			@Override
			public void onNewFeedbackReply(List<UUID> newReplyUUIDList) {
				for (UUID uuid : newReplyUUIDList) {
					MessageManager.getInstance().createLocalMessage_ShowFeedbackReply(uuid);
				}
				AppNotificationManager.sendNoticeHasNewFeedbackReply();
			}
		};

		/**
		 * 封装容器。为避免提前优化，先不实现“快速检索”功能，直接用简单的单容器 （注意这个容器应该是线程安全的）
		 */
		private static class Container implements Iterable<UserFeedback> {
			private final ConcurrentLinkedQueue<UserFeedback> vector = new ConcurrentLinkedQueue<UserFeedback>();

			private int hasUnread = -1;
			private int needReply = -1;

			/**
			 * 将所有条目标记为“已读”
			 * 
			 * @return
			 */
			public boolean setAllRead() {
				if (hasUnread()) {
					for (UserFeedback reply : vector) {
						reply.setRead();
					}
					hasUnread = 0;
					return true;
				}
				return false;
			}

			/**
			 * 判断容器里是否有“未读”回复
			 * 
			 * @return
			 */
			public boolean hasUnread() {
				if (hasUnread < 0) {
					for (UserFeedback uf : vector) {
						if (!uf.wasRead()) {
							hasUnread = 1;
							return true;
						}
					}
					hasUnread = 0;
					return false;
				}
				return hasUnread > 0;
			}

			public boolean needReply() {
				if (needReply < 0) {
					for (UserFeedback uf : vector) {
						if (uf.needReply()) {
							needReply = 1;
							return true;
						}
					}
					needReply = 0;
					return false;
				}
				return needReply > 0;
			}

			public boolean isEmpty() {
				return vector.isEmpty();
			}

			private void addReply(UserFeedback reply) {
				boolean already_exists = false;
				boolean feedback_found = false;
				for (UserFeedback exist : vector) {
					if (exist.uuid.equals(reply.uuid)) {
						if (exist.isReply()) {
							already_exists = true;
							if (feedback_found) {
								break;
							}
						} else if (exist.needReply()) {
							feedback_found = true;
							exist.setReplied();
							if (already_exists) {
								break;
							}
						}
					}
				}
				if (!already_exists) {
					vector.add(reply);
				}
			}

			private void addFeedback(UserFeedback feedback) {
				UserFeedback exist = null;
				for (UserFeedback uf : vector) {
					if (uf.uuid.equals(feedback.uuid)) {
						if (uf.isReply()) {
							if (exist == null) {
								feedback.setReplied();
							} else {
								exist.setReplied();
								break;
							}
						} else {
							exist = uf;
						}
					}
				}
				if (exist == null) {
					vector.add(feedback);
				}
			}

			public void add(UserFeedback uf) {
				if (uf.isReply()) {
					addReply(uf);
				} else {
					addFeedback(uf);
				}
				this.hasUnread = -1;
				this.needReply = -1;
			}

			@Override
			public Iterator<UserFeedback> iterator() {
				return vector.iterator();
			}

		}

		private final Container container = new Container();

		private boolean already_loaded; // 为真时表示已经从文件里加载过了

		private static HandlerThread createAndStartHandlerThread() {
			HandlerThread ht = new HandlerThread("UserFeedback.History");
			ht.start();
			return ht;
		}

		private History() {
			super(createAndStartHandlerThread().getLooper());
			this.sendEmptyMessage(MSG_LOAD_FROM_FILE);
			TriggerManager.getInstance().addObserver(eventObserver);
		}

		/**
		 * 是不是有未读的回复？
		 * 
		 * @return
		 */
		public boolean hasUnread() {
			return container.hasUnread();
		}

		public boolean isEmpty() {
			return container.isEmpty();
		}

		/**
		 * 设置所有的回复为已读
		 */
		public void setAllRead() {
			if (container.setAllRead()) {
				this.sendEmptyMessage(MSG_SAVE_TO_FILE);
			}
		}

		public void init() {}

		private static final int MSG_LOAD_FROM_FILE = 1;
		private static final int MSG_SAVE_TO_FILE = 2;
		private static final int MSG_QUERY_REPLY = 3;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_LOAD_FROM_FILE:
				if (LOG)
					Log.d(TAG, "Load from file in work thread");
				loadFromFileInWorkThread();
				break;
			case MSG_SAVE_TO_FILE:
				if (LOG)
					Log.d(TAG, "Save to file in work thread");
				saveToFileInWorkThread();
				break;
			case MSG_QUERY_REPLY:
				if (LOG)
					Log.d(TAG, "Try to query");
				this.removeMessages(MSG_QUERY_REPLY);
				tryQuery();
				break;
			}
		}

		private void tryQuery() {
			QueryReplyProcesser.QueryReplyFuncResult r = QueryReplyProcesser.execute(this, this.container);
			switch (r) {
			case DonotNeedQuery:	// 勿需查询
				if (LOG)
					Log.d(TAG, "Do not need query");
				break;
			case QueryPostFailed:		// 查询操作失败，5分钟后重试
				if (LOG)
					Log.d(TAG, "Post query failed, retry after five minutes");
				sendEmptyMessageDelayed(MSG_QUERY_REPLY, 5 * 60 * 1000);
				break;
			case NoReply:			// 没有回复，1小时后重试
				if (LOG)
					Log.d(TAG, "There are not any replies, retry after one hours");
				sendEmptyMessageDelayed(MSG_QUERY_REPLY, 60 * 60 * 1000);
				break;
			case HasReply:			// 有回复，马上重试
				if (LOG)
					Log.d(TAG, "There are some replies, retry now");
				sendEmptyMessage(MSG_QUERY_REPLY);
				break;
			}
		}

		private boolean loadFromFileInWorkThread() {
			if (already_loaded) {
				return true;
			}
			File file = FileUtils.getDataFile(FILE_NAME);
			byte[] data = FileUtils.read(file);
			if (data == null) {
				already_loaded = true;
				return true;
			}
			// 载入
			try {
				Proto.UserFeedbackList proto = Proto.UserFeedbackList.parseFrom(data);
				for (Proto.UserFeedback proto_uf : proto.getListList()) {
					UserFeedback uf = new UserFeedback(proto_uf);
					this.container.add(uf);
				}
				already_loaded = true;
			} catch (InvalidProtocolBufferException e) {}
			//
			sendEmptyMessage(MSG_QUERY_REPLY);
			return already_loaded;
		}

		private void saveToFileInWorkThread() {
			if (!loadFromFileInWorkThread()) {
				return;
			}
			File file = FileUtils.getDataFile(FILE_NAME);
			if (this.container.isEmpty()) {
				FileUtils.deleteFileOrDirectory(file);
			} else {
				Proto.UserFeedbackList.Builder builder = Proto.UserFeedbackList.newBuilder();
				for (UserFeedback uf : this.container) {
					uf.buildProtobuf(builder.addListBuilder());
				}
				byte[] data = builder.build().toByteArray();
				FileUtils.write(file, data);
			}
		}

		private static class QueryReplyProcesser {

			public static enum QueryReplyFuncResult {
				DonotNeedQuery,				// 勿需查询
				QueryPostFailed,			// 查询失败（比如无网络环境）
				NoReply,					// 查询成功，但尚无回复
				HasReply,					// 查询成功，且有回复
			}

			public static QueryReplyFuncResult execute(History owner, Container container) {
				// 判断是否需要查询
				if (container.isEmpty() || !container.needReply()) {
					return QueryReplyFuncResult.DonotNeedQuery;
				}
				// 判断是否有网络连接
				if (!NetManager.getInstance().isConnected()) {
					return QueryReplyFuncResult.QueryPostFailed;
				}
				byte[] post_data = buildPostData(container);
				if (post_data == null) {
					return QueryReplyFuncResult.DonotNeedQuery;
				}
				//
				com.subao.common.net.Http.Response response;
				try {
					response = SubaoHttp.createHttp().doPost(
						SubaoHttp.createURL(SubaoHttp.InterfaceType.HAS_TIMESTAMP_KEY, GlobalDefines.APP_NAME_FOR_HTTP_REQUEST, null, UrlConfig.instance.getDomainOfFeedBack()
							+ "/feedbackGetUnread", null),
							post_data,
							Http.ContentType.PROTOBUF.str);
				} catch (IOException e) {
					return QueryReplyFuncResult.QueryPostFailed;

				}
				if (response.code != 200) {
					return QueryReplyFuncResult.QueryPostFailed;
				}
				if (response.data == null) {
					return QueryReplyFuncResult.NoReply; // 成功了，但是没有查询结果
				}
				Collection<UserFeedback.Reply> reply_list = buildReplyListFromServerResponse(response.data);
				if (reply_list != null) {
					owner.sendEmptyMessage(MSG_SAVE_TO_FILE);
					final List<UUID> newReplyUUIDList = processReplyList(container, reply_list);
					if (!newReplyUUIDList.isEmpty()) {
						MainHandler.getInstance().post(new Runnable() {
							@Override
							public void run() {
								TriggerManager.getInstance().raiseNewFeedbackReply(newReplyUUIDList);
							}
						});
						return QueryReplyFuncResult.HasReply;
					}
				}
				return QueryReplyFuncResult.NoReply;
			}

			private static List<UUID> processReplyList(Container container, Collection<UserFeedback.Reply> reply_list) {
				List<UUID> newReplyUUIDList = new ArrayList<UUID>(reply_list.size());
				for (UserFeedback.Reply r : reply_list) {
					if (processReply(container, r)) {
						newReplyUUIDList.add(r.uuid);
					}
				}
				return newReplyUUIDList;
			}

			private static boolean processReply(Container container, UserFeedback.Reply r) {
				boolean same_reply_exists = false;
				boolean feedback_found = false;
				String feedbackContent = null;
				for (UserFeedback uf : container) {
					if (uf.uuid.equals(r.uuid)) {
						if (uf.isReply()) {
							same_reply_exists = true;
							if (feedback_found) {
								break;
							}
						} else {
							feedback_found = true;
							uf.setReplied();
							feedbackContent = uf.content;
							if (same_reply_exists) {
								break;
							}
						}
					}
				}
				//
				if (!same_reply_exists && r.state != 3 && !TextUtils.isEmpty(r.content)) {
					container.add(UserFeedback.createReply(r, feedbackContent));
					return true;
				}
				return false;
			}

			/**
			 * 根据Server发来的Proto，生成Reply列表
			 */
			private static Collection<UserFeedback.Reply> buildReplyListFromServerResponse(byte[] response) {
				Proto.UserFeedbackReplyList proto;
				try {
					proto = Proto.UserFeedbackReplyList.parseFrom(response);
				} catch (InvalidProtocolBufferException e) {
					return null;
				}
				if (proto.getListCount() <= 0) {
					return null;
				}
				Collection<UserFeedback.Reply> result = new ArrayList<UserFeedback.Reply>(proto.getListCount());
				for (Proto.UserFeedbackReply reply_proto : proto.getListList()) {
					UserFeedback.Reply r = new UserFeedback.Reply(reply_proto);
					result.add(r);
				}
				return result.size() == 0 ? null : result;
			}

			//			private static byte[] serverResponseToBytes(HttpEntity he) {
			//				int size = Long.valueOf(he.getContentLength()).intValue();
			//				if (size <= 0) {
			//					return null;
			//				}
			//				ByteArrayOutputStream output = new ByteArrayOutputStream(size);
			//				try {
			//					he.writeTo(output);
			//				} catch (IOException e) {
			//					return null;
			//				}
			//				return output.toByteArray();
			//			}

			private static byte[] buildPostData(Container uf_list) {
				Proto.UserFeedbackQueryReply.Builder builder = null;
				for (UserFeedback uf : uf_list) {
					if (uf.needReply()) {
						if (builder == null) {
							builder = Proto.UserFeedbackQueryReply.newBuilder();
							builder.setUserId(Statistic.getDeviceId());
						}
						Proto.UUID.Builder b = builder.addUuidBuilder();
						b.setUuidMost(uf.uuid.getMostSignificantBits());
						b.setUuidLeast(uf.uuid.getLeastSignificantBits());
					}
				}
				if (builder == null) {
					return null;
				}
				Proto.UserFeedbackQueryReply proto = builder.build();
				return proto.toByteArray();
			}

		}

		/** 加入一个UserFeedback，线程安全 */
		public void add(UserFeedback uf) {
			container.add(uf);
			this.sendEmptyMessage(MSG_SAVE_TO_FILE);
			if (!uf.isReply()) {
				if (LOG) {
					Log.d(TAG, "It's not reply, try query now");
				}
				this.sendEmptyMessage(MSG_QUERY_REPLY);
			}
		}

		@Override
		public Iterator<UserFeedback> iterator() {
			return container.iterator();
		}

		/**
		 * 返回一个包含所有UserFeedback的List副本
		 * 
		 * @return null表示没有
		 */
		public List<UserFeedback> cloneAllItems() {
			if (!this.container.isEmpty()) {
				List<UserFeedback> list = new ArrayList<UserFeedback>(30);
				for (UserFeedback uf : this.container) {
					list.add(uf);
				}
				return list.isEmpty() ? null : list;
			} else {
				return null;
			}
		}

	}

	/**
	 * 通过uuid查找客服回复的反馈记录
	 * @param uuid
	 * @return
	 */
	public static UserFeedback searchFeedbackReplyByUUID(UUID uuid){
		if(null == uuid){
			return null;
		}
		Collection<UserFeedback> record = History.instance.container.vector;
		for (UserFeedback userFeedback : record) {
			if(!userFeedback.isReply()){
				continue;
			}
			if(uuid.equals(userFeedback.uuid)){
				return userFeedback;
			}
		}
		return null;
	}

}
