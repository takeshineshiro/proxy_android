package cn.wsds.gamemaster.social;


/**
 * Created by lidahe on 15/12/4.
 */
public class UserSocialBean {
	public final SOCIAL_MEDIA socailMedia;
	public final String openId;
	public final String name;
    public final String token;
    public final String avatarUrl;
//    private GENDER gender;
//    private String province;
//    private String city;
//    private Bitmap drawableAvatar;
    
    public UserSocialBean(SOCIAL_MEDIA media, String openId, String name, String token, String avatarUrl) {
    	this.socailMedia = media;
    	this.openId = openId;
    	this.name = name;
    	this.token = token;
    	this.avatarUrl = avatarUrl;
    }

//    public String getAvatarUrl() {
//        return avatarUrl;
//    }
//
//    public void setAvatarUrl(String avatarUrl) {
//        this.avatarUrl = avatarUrl;
//    }

//    public GENDER getGender() {
//        return gender;
//    }
//
//    public void setGender(GENDER gender) {
//        this.gender = gender;
//    }
//
//    public String getProvince() {
//        return province;
//    }
//
//    public void setProvince(String province) {
//        this.province = province;
//    }
//
//    public String getCity() {
//        return city;
//    }
//
//    public void setCity(String city) {
//        this.city = city;
//    }
    
//	public Bitmap getDrawableAvatar() {
//		return drawableAvatar;
//	}
//
//	public void setDrawableAvatar(Bitmap drawableAvatar) {
//		this.drawableAvatar = drawableAvatar;
//	}

	@Override
	public String toString() {
		return com.subao.utils.Misc.printObject(this);
//		StringBuilder builder = new StringBuilder();
//		builder.append("UserSocialBean [socailMedia=");
//		builder.append(socailMedia);
//		builder.append(", openId=");
//		builder.append(openId);
//		builder.append(", token=");
//		builder.append(token);
//		builder.append(", name=");
//		builder.append(name);
//		builder.append(", avatarUrl=");
//		builder.append(avatarUrl);
//		builder.append(", gender=");
//		builder.append(gender);
//		builder.append(", province=");
//		builder.append(province);
//		builder.append(", city=");
//		builder.append(city);
//		builder.append("]");
//		return builder.toString();
	}

    
}

