package cn.wsds.gamemaster.data;

/**
 * intent extra name
 */
public class IntentExtraName {

	/** 当前启动是否通知栏 */
	public static final String START_FROM_NOTIFICATION = "cn.wsds.gamemaster.start_from_notification";
	/** 传递应用packageName */
	public static final String INTENT_EXTRANAME_PACKAGE_NAME = "cn.wsds.gamemaster.package_name";
	/** 当前启动是否从boot */
	public static final String START_FROM_BOOT = "cn.wsds.gamemaster.start_from_boot";
	/** 来自“APP内开启游戏”通知 */
	public static final String START_FROM_OPEN_GAME_INSIDE = "cn.wsds.gamemaster.start_from_notification_open_game_inside";
	/** 消息中心 - 附加数据 */
	public static final String NOTICE_INTENT_EXTRANAME_EXTRA_DATA = "cn.wsds.gamemaster.extra_data";
	/** 消息中心 - 消息ID */
	public static final String NOTICE_INTENT_EXTRANAME_ID = "cn.wsds.gamemaster.message_id";
	/** 后台应用个数 */
	public static final String INTENT_EXTRANAME_APP_CLEAN_COUNT = "cn.wsds.gamemaster.appCleanCount";
	/** 公用，表示“来自通知栏点击” */
	//public static final String CALL_FROM_NOTIFICATION = "cn.wsds.gamemaster.call_from_notification";
	/** 公用，表示“来自悬浮窗对话框” */
	public static final String CALL_FROM_FLOATWIDOW_DIALOG = "cn.wsds.gamemaster.call_from_floatwidow_dialog";
	/** 商品详情*/
	public static final String INTENT_EXTRANAME_PRODUCT_DETAIL = "cn.wsds.gamemaster.product_detail";
	/** 订单详情” */
	public static final String INTENT_EXTRANAME_ORDER_DETAIL = "cn.wsds.gamemaster.order_detail";
}
