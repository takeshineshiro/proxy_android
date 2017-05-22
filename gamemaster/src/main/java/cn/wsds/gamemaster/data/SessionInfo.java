package cn.wsds.gamemaster.data;

import java.io.File;

import android.text.TextUtils;
import cn.wsds.gamemaster.pb.Proto;

import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.utils.FileUtils;


/**
 * 用户登录状态下会话信息
 */
public class SessionInfo {

	/** 存储session信息的文件名 */
	private static final String SESSION_DATA_FILE = "sessioninfo";
	
	/** 用户注册时，服务器分配给用户的系统唯一标识；仅登陆成功时不为空 */
	private final String userId;
	/** 接入令牌，非常重要，登陆后消息交互过程在HTTP头要携带；仅登陆成功时不为空 */
    private String accessToken;
    /** 用于刷新接入令牌；仅登陆成功时不为空 */
    private String refreshToken;
    /** 接入令牌超时时间；仅登陆成功时不为空 */
    private int expiresIn;
	/** 本地登录会话标识，用于用户退出登录时使用；仅登陆成功时不为空 */
	private final String sessionId;
	
	/**
	 * 令牌更新时间
	 */
	private long updateTokenTimeMillis;
	
	protected SessionInfo(hr.client.appuser.SessionInfoOuterClass.SessionInfo protoInfo) {
		this(protoInfo.getUserId(),protoInfo.getAccessToken(),protoInfo.getRefreshToken(),protoInfo.getExpiresIn(),protoInfo.getSessionId());
	}
	protected SessionInfo(String userId, String accessToken, String refreshToken,
			int expiresIn, String sessionId) {
		this.userId = userId;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expiresIn = expiresIn;
		this.sessionId = sessionId;
		setUpdateTokenTimeMillis();
	}
	
	public String getUserId() {
		return userId;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public String getRefreshToken() {
		return refreshToken;
	}
	public int getExpiresIn() {
		return expiresIn;
	}
	public String getSessionId() {
		return sessionId;
	}
	
	protected boolean updateToken(String accessToken,String refreshToken,int expiresIn){
		if(com.subao.common.utils.StringUtils.isStringEqual(this.accessToken, accessToken)){
			return false;
		}
		
		if(com.subao.common.utils.StringUtils.isStringEqual(this.refreshToken, refreshToken)){
			return false;
		}
		
		if(this.expiresIn == expiresIn){
			return false;
		}
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expiresIn = expiresIn;
		setUpdateTokenTimeMillis();
		return true;
	}
	
	public long getUpdateTokenTimeMillis() {
		return updateTokenTimeMillis;
	}
	
	private void setUpdateTokenTimeMillis() {
		this.updateTokenTimeMillis = System.currentTimeMillis();
	}
	
	public static SessionInfo loadFromProto(){
		File file = FileUtils.getDataFile(SESSION_DATA_FILE);
		byte[] data = FileUtils.read(file);
		if (data == null) {
			return null;
		}

		try {
			Proto.SessionInfo proto = Proto.SessionInfo.parseFrom(data);
			String userId = proto.getUserId();
			String accessToken = proto.getAccessToken();
			String refreshToken = proto.getRefreshToken();
			int expiresIn = proto.getExpiresIn();
			String sessionId = proto.getSessionId();
			SessionInfo info = new SessionInfo(userId, accessToken, refreshToken, expiresIn, sessionId);
			info.updateTokenTimeMillis = proto.getUpdateTokenTimeMillis();
			return info;
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void save(){
		byte[] data = serial();
		File file = FileUtils.getDataFile(SESSION_DATA_FILE);
		FileUtils.write(file, data);
	}
	
	public byte[] serial(){
		Proto.SessionInfo.Builder builder = Proto.SessionInfo.newBuilder();
		if(!TextUtils.isEmpty(this.accessToken)){
			builder.setAccessToken(this.accessToken);
		}
		if(!TextUtils.isEmpty(this.refreshToken)){
			builder.setRefreshToken(this.refreshToken);
		}
		if(!TextUtils.isEmpty(this.userId)){
			builder.setUserId(this.userId);
		}
		builder.setExpiresIn(this.expiresIn);
		if(!TextUtils.isEmpty(this.sessionId)){
			builder.setSessionId(this.sessionId);
		}
		builder.setUpdateTokenTimeMillis(this.updateTokenTimeMillis);
		return builder.build().toByteArray();
	}
	
	public static boolean clear() {
		File file = FileUtils.getDataFile(SESSION_DATA_FILE);
		return file.delete();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (!(o instanceof SessionInfo)) {
			return false;
		}
		SessionInfo info = (SessionInfo) o;

		if (this.expiresIn != info.expiresIn) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.accessToken, info.accessToken)) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.refreshToken, info.refreshToken)) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.sessionId, info.sessionId)) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.userId, info.userId)) {
			return false;
		}

		return true;
	}
	
}
