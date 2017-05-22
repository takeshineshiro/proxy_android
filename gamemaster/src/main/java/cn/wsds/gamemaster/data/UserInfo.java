package cn.wsds.gamemaster.data;

import hr.client.appuser.UserInfoOuterClass;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import cn.wsds.gamemaster.pb.Proto;
import cn.wsds.gamemaster.social.SOCIAL_MEDIA;
import cn.wsds.gamemaster.ui.BitmapUtil;
import cn.wsds.gamemaster.ui.UIUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.subao.utils.FileUtils;

/**
 * Created by lidahe on 15/12/9.
 * 用户基础信息
 */
public class UserInfo {
	
	/** 用户信息存储 */
    private static final String USER_DATA_FILE = "userinfo";
    
    public final String userId;
    /** 积分 */
	private int score;
    /** 三方账号登陆 - 用户头像 */
	private Bitmap drawableAvatar = null;
	/** 手机号 */
	final String phoneNumber;
	/** 用户昵称 */
	final String nickName;
	/** 三方用户昵称 */
	final String thirdPartNickName;
	/** 签到时间戳 UTC秒 */
	private long timestampSign;

	/**
	 * 是否是三方登录
	 */
	public boolean thirdPart;

	private final SOCIAL_MEDIA social_MEDIA;
	
    private UserInfo(Proto.UserInfo proto) {
    	String userId = proto.getUserId();
    	this.userId = (userId == null) ? "" : userId;
    	this.thirdPart = proto.getThirdPart();
    	this.thirdPartNickName = proto.getThirdPartNickName();
    	this.nickName = proto.getNickName();
		this.phoneNumber = proto.getPhoneNumber();
		this.score = proto.getScore();
		this.timestampSign = proto.getTimestampSignin();
		if(proto.hasAvatar()){
			byte[] byteArray = proto.getAvatar().toByteArray();
			if(byteArray!=null){
				Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
				this.drawableAvatar = bitmap;
			}
		}
		int thirdPartCode = proto.getThirdPartCode();
		social_MEDIA = getSocialMedia(thirdPartCode);
    }

	private SOCIAL_MEDIA getSocialMedia(int thirdPartCode) {
		if(SOCIAL_MEDIA.QQ.getCode() == thirdPartCode){
			return SOCIAL_MEDIA.QQ;
		}else if(SOCIAL_MEDIA.WEIBO.getCode() == thirdPartCode){
			return SOCIAL_MEDIA.WEIBO;
		}else if(SOCIAL_MEDIA.WEIXIN.getCode() == thirdPartCode){
			return SOCIAL_MEDIA.WEIXIN;
		}else{
			return null;
		}

	}

	UserInfo(UserInfoOuterClass.UserInfo protoInfo,String name, SOCIAL_MEDIA social_MEDIA, Bitmap drawableAvatar) {
    	String userId = protoInfo.getUserId();
    	this.userId = (userId == null) ? "" : userId;
    	this.score = protoInfo.getTotalPoints();
    	this.phoneNumber = protoInfo.getPhoneNumber();
    	this.nickName = protoInfo.getNickName();
    	this.thirdPart = name!=null;
    	this.drawableAvatar = drawableAvatar;
    	this.thirdPartNickName = name;
    	this.social_MEDIA = social_MEDIA;
	}

	public int getScore() {
        return score;
    }

    public long getTimestampSign() {
        return timestampSign;
    }
    protected boolean updateScore(int score) {
    	if(this.score == score){
    		return false;
    	}
        this.score = score;
        return true;
    }
    
    protected boolean updateTimestampSignin(long timestampSign) {
		if(this.timestampSign == timestampSign){
			return false;
		}
		this.timestampSign = timestampSign;
		return true;
	}

	void updateDrawableAvatar(Bitmap drawableAvatar) {
		this.drawableAvatar = drawableAvatar;
	}
	
	public Bitmap getDrawableAvatar() {
		return drawableAvatar;
	}

	public void save() {
		byte[] data = serial();
		File file = FileUtils.getDataFile(USER_DATA_FILE);
		FileUtils.write(file, data);
	}

	private byte[] serial() {
		Proto.UserInfo.Builder builder = Proto.UserInfo.newBuilder();
		if(this.drawableAvatar != null){
			byte[] bitmap2Bytes = BitmapUtil.bitmap2Bytes(this.drawableAvatar);
			ByteString byteAvatar = ByteString.copyFrom(bitmap2Bytes);
			builder.setAvatar(byteAvatar);
		}
		if(!TextUtils.isEmpty(this.nickName)){
			builder.setNickName(this.nickName);
		}
		if(!TextUtils.isEmpty(this.phoneNumber)){
			builder.setPhoneNumber(this.phoneNumber);
		}
		if(!TextUtils.isEmpty(this.userId)){
			builder.setUserId(this.userId);
		}
		builder.setScore(this.score);
		builder.setTimestampSignin(this.timestampSign);
		if(thirdPart){
			builder.setThirdPart(this.thirdPart);
			if(!TextUtils.isEmpty(this.thirdPartNickName)){
				builder.setThirdPartNickName(this.thirdPartNickName);
			}
			if(this.social_MEDIA!=null){
				builder.setThirdPartCode(this.social_MEDIA.getCode());
			}
		}
		return builder.build().toByteArray();
	}

	public static UserInfo loadFromProto() {
		File file = FileUtils.getDataFile(USER_DATA_FILE);
		byte[] data = FileUtils.read(file);
		if (data == null) {
			return null;
		}

		try {
			Proto.UserInfo proto = Proto.UserInfo.parseFrom(data);
			UserInfo info = new UserInfo(proto);
			return info;
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean clear() {
		File file = FileUtils.getDataFile(USER_DATA_FILE);
		return file.delete();
	}
	
	public String getUserId() {
		return userId;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getNickName() {
		return nickName;
	}

	public String getThirdPartNickName() {
		return thirdPartNickName;
	}

	public boolean isThirdPart() {
		return thirdPart;
	}
	
	public SOCIAL_MEDIA getSocial_MEDIA() {
		return social_MEDIA;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (!(o instanceof UserInfo)) {
			return false;
		}
		UserInfo info = (UserInfo) o;
		if (info.score != this.score) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.nickName, info.nickName)) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.phoneNumber, info.phoneNumber)) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.userId, info.userId)) {
			return false;
		}

		if (!com.subao.common.utils.StringUtils.isStringEqual(this.thirdPartNickName, info.thirdPartNickName)) {
			return false;
		}
		return true;
	}

    public String getUserName() {

        if (TextUtils.isEmpty(getNickName())) {
            if (thirdPart) {
                return getThirdPartNickName();
            } else {
                return UIUtils.getFormatPhoneNumber(getPhoneNumber());
            }
        } else {
            return getNickName();
        }
    }
}