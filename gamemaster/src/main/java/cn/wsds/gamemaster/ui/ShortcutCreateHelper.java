package cn.wsds.gamemaster.ui;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;

public class ShortcutCreateHelper {
	
	private static final String shortcutName = "游戏加速";
	
	/**
	 * {@link createShortcut} 函数的返回值
	 */
	public static enum Result {
		/** 成功 */
		OK,
		/** 没有找到可加速的游戏 */
		NO_GAME_FOUND,
	}
	
//	public void showDialog(final Context context) {
//		CommonDialog dialog = new CommonAlertDialog(context);
//		dialog.setContentSize(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
//		dialog.setTitle("提示");
//		dialog.setMessage("是否在桌面添加快速启动工具？方便您更快捷的使用游戏加速。");
//		dialog.setPositiveButton("立即创建",
//				new DialogInterface.OnClickListener() {
//
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						StatisticDefault.addEvent(context, Event.DIALOG_SHORTCUT, "cancel");
//						dialog.dismiss();
//						createShortcut(context);
//					}
//				});
//		dialog.setNegativeButton("暂不",
//				new DialogInterface.OnClickListener() {
//
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						StatisticDefault.addEvent(context, Event.DIALOG_SHORTCUT, "create");
//						dialog.dismiss();
//					}
//				});
//		dialog.show();
//	}
	
	/**
	 * 创建桌面快捷方式
	 * @param context {@link Context}
	 * @return {@link Result} 创建的结果（成功还是失败？失败的原因……）
	 */
	public static Result createShortcut(Context context) {
		 List<GameInfo> gameList = GameManager.getInstance().getSupportedAndReallyInstalledGames();
		 if (gameList.isEmpty()) {
			 return Result.NO_GAME_FOUND;
		 }
    	Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
    	intent.putExtra("duplicate", false);
    	intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
    	Bitmap bitmap = composeBitmap(context, gameList);
    	intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
    	Intent intent2 = new Intent(AppMain.getContext(), ActivityShortcut.class);
    	intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent2);
    	context.sendBroadcast(intent);
    	return Result.OK;
    }
	
	 private static Bitmap composeBitmap(Context context, List<GameInfo> gameList){

		 Bitmap bitmapBg = BitmapFactory.decodeResource(context.getResources(), R.drawable.shortcut_icon_bg_96);
		 int width = bitmapBg.getWidth();
		 int height = bitmapBg.getHeight();
		 Bitmap bitmapNew = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		 Canvas canvas = new Canvas(bitmapNew);
		 canvas.drawBitmap(bitmapBg, null, new Rect(0, 0, width, height), null);
		 int length = Math.min(gameList.size(), 4);
		 final int margin = (int) (width * 0.03f);
		 final int marginOutter = margin * 3;
		 for (int i = 0; i < length; i++){
			 GameInfo gameInfo = gameList.get(i);
			 BitmapDrawable bitmapDrawable = (BitmapDrawable) gameInfo.getAppIcon(context);
			 Bitmap bitmap = bitmapDrawable.getBitmap();
			 switch (i) {
			 case 0:
				 canvas.drawBitmap(bitmap, null, new Rect(marginOutter, marginOutter, width / 2 - margin,  height / 2 - margin), null);
				 break;
			 case 1:
				 canvas.drawBitmap(bitmap, null, new Rect(width / 2 + margin, marginOutter, width - marginOutter, height / 2 - margin), null);
				 break;
			 case 2:
				 canvas.drawBitmap(bitmap, null, new Rect(marginOutter, height / 2 + margin, width / 2 - margin, height - marginOutter), null);
				 break;
			 case 3:
				 canvas.drawBitmap(bitmap, null, new Rect(width / 2 + margin , height / 2 + margin , width - marginOutter, height - marginOutter), null);
				 break;
			 default:
				 break;
			}
		 }
		 Bitmap bitmapIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.shortcut_icon_angle_96);
		 canvas.drawBitmap(bitmapIcon, null, new Rect(0, 0, width, height), null);
		 canvas.save(Canvas.ALL_SAVE_FLAG);
		 canvas.restore();
		 return bitmapNew;
	    }
	 
//	 private boolean hasShortcut(Context context, String shortcutName) 
//	 { 
//		 String url = getAuthorityFromPermission(context, "com.android.launcher.permission.READ_SETTINGS");
//		 if (url == null){
//			 return false;
//		 }
//		 ContentResolver resolver = context.getContentResolver(); 
//		 Cursor cursor = resolver.query(Uri.parse("content://" + url + "/favorites?notify=true"), null, "title=?",new String[] {shortcutName}, null); 
//		 if (cursor != null && cursor.moveToFirst()) { 
//			 cursor.close(); 
//			 return true; 
//		 } 
//		 	return false; 
//	} 
//	
//	private static String getAuthorityFromPermission(Context context, String permission){
//	     if (permission == null) return null;
//	     List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
//	     if (packs != null) {
//	         for (PackageInfo pack : packs) { 
//	             ProviderInfo[] providers = pack.providers; 
//	             if (providers != null) { 
//	                 for (ProviderInfo provider : providers) { 
//	                     if (permission.equals(provider.readPermission)) return provider.authority;
//	                     if (permission.equals(provider.writePermission)) return provider.authority;
//	                 } 
//	             }
//	         }
//	     }
//	     return null;
//	 } 
//	
} 
