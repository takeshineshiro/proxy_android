package cn.wsds.gamemaster.social;

/**
 * Created by lidahe on 15/12/4.
 */
public enum SOCIAL_MEDIA {
    WEIBO(1), QQ(2), WEIXIN(3);

    private int code;

    SOCIAL_MEDIA(int _code) {
        this.code = _code;
    }

    public int getCode() {
        return code;
    }

}
