package cn.wsds.gamemaster.tools;

import java.util.HashMap;
import java.util.Map;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.dialog.CommonDesktopDialog;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.Statistic.Event;
import cn.wsds.gamemaster.ui.UIUtils;

public class ContactsUtils {
	private final static boolean LOG = false;
	private final static String TAG = "ContactsUtils";
	
	private static AsyncQueryHandler asyncQueryHandler; // 异步查询数据库类对象
	private static Map<String, String> contactMap = new HashMap<String, String>();	
	private static final int KEY_LENGTH = 7; // 从后往前匹配N位数字
	private static Context context;
	private static boolean hasStrangeNumber = false;
	
	public static void init(Context c) {
		context = c;
	}
	
	public static void destoryDialog(){
		if (dialog != null){
			dialog.dismiss();
		}
	}
	
	public static void query() {
		if (hasStrangeNumber){
			hasStrangeNumber = false;
			if (!ConfigManager.getInstance().isPromptReadContacts()){
				ConfigManager.getInstance().setPormptReadContacts();
				showDialog();
			} else {
				startQuery();
			}
		}
	}
	
	private static CommonDesktopDialog dialog;
	private static void showDialog(){
		Statistic.addEvent(AppMain.getContext(), Event.DIALOG_INGAME_CALL);
		dialog = new CommonDesktopDialog();
		dialog.setMessage("来电管理可以防止游戏被来电弹出，此功能需要获取联系人信息。");
		dialog.setPositiveButton("确定", null);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				startQuery();
				ContactsUtils.dialog = null;
			}
		});
		dialog.setImage(R.drawable.call_read_contact);
		dialog.show();
	}
	
	private static void startQuery(){
		if (LOG) {
			Log.i(TAG, "ContactsUtils.query():读取联系人信息...");
		}
		
		Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI; // 联系人Uri
		// 查询的字段
		String[] projection = { ContactsContract.CommonDataKinds.Phone._ID, // 0
								ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, // 1
								//ContactsContract.CommonDataKinds.Phone.DATA1,
								ContactsContract.CommonDataKinds.Phone.NUMBER }; // 2
		
		asyncQueryHandler = new MyAsyncQueryHandler(context.getContentResolver());
		asyncQueryHandler.startQuery(0, null, uri, projection, null, null, null);
	}
	
	/**
	 * 根据号码返回联系人名字,找不到联系人则返回原号码
	 * @param number 电话号码
	 * @return
	 */
	public static String getDisplayName(String number) {
		if (number == null) {
			return null;
		}
		
		// 截取末尾N位数字作为key
		String key = number;
		int length = key.length();
		if (length > KEY_LENGTH) {
			key = key.substring(length - KEY_LENGTH, length); 
		}
		
		String name = contactMap.get(key);
		if (name == null) {
			hasStrangeNumber = true;
			return number; // 找不到则返回原号码
		} else {
			return name;
		}		
	}
	
//	public static String lastSubString(String str, int count) {
//		int length = str.length();
//		if (length > count) {
//			str = str.substring(length - count, length); // 截取末尾N位数字
//		}
//		return str;
//	}

	
	static class MyAsyncQueryHandler extends AsyncQueryHandler {

		public MyAsyncQueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					String name = cursor.getString(1);
					String number = cursor.getString(2);
					if (TextUtils.isEmpty(name) || TextUtils.isEmpty(number)) {
						continue;
					}
					number = number.replace(" ", ""); // 有些号码会被空格分开
					int length = number.length();
					if (length == 0) {
						continue;
					}
					if (length > KEY_LENGTH) {
						number = number.substring(length - KEY_LENGTH, length); // 截取末尾N位数字
					}
					
					if (!contactMap.containsKey(number)) {
						contactMap.put(number, name);
						if (LOG) {
							Log.i(TAG, String.format("读取联系人：%s, 号码末%d位：%s", name, KEY_LENGTH, number));
						}
					}
				} while (cursor.moveToNext());

				//UIUtils.showToast("读取联系人成功", Toast.LENGTH_SHORT);
				Statistic.addEvent(context, Event.GET_CONTACT_INFO, "true");
			} else {
				UIUtils.showToast("读取联系人失败", Toast.LENGTH_SHORT);
				Statistic.addEvent(context, Event.GET_CONTACT_INFO, "false");
			}

			super.onQueryComplete(token, cookie, cursor);
		}
	}
}
