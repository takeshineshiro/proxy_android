package cn.wsds.gamemaster.tools;

//import java.io.File;
import android.content.Context;
import android.util.Log;
//import android.content.Intent;
//import android.net.Uri;
//import android.provider.MediaStore;

public class ScreenCapUtils {
	private final static boolean LOG = false; //test
	private final static String TAG = "ScreenCapUtils";
	
	/** 供测试用的代码：截屏总是失败 */
	public static boolean alwaysFail;
	
	private static Context context;
	private static OnScreenCapListener listener;
	private static String filePath;
	
	/** 
	 * 截屏回调，成功返回true
	 * @author lcy
	 *
	 */
	public static interface OnScreenCapListener {
		public void onScreenCap(boolean result);
	}
	
	/**
	 * 截屏并保存到图库，结束时回调
	 * @param c context
	 * @param path screencap保存为png图片, 全路径
	 * @param l 回调监听器
	 */
	public static boolean screenCap(Context c, String path, OnScreenCapListener l) {
		if (listener != null/*上次截图还没完成*/ || alwaysFail) {
			l.onScreenCap(false);
			return false;
		}
		
		context = c.getApplicationContext();
		filePath = path;
		listener = l;
		if (context == null || filePath == null || listener == null) {
			return false;
		}
//		if (alwaysFail) {
//			listener.onScreenCap(false);
//			return false;
//		}
		
		// XXX: android 5.0的screencap卡住不返回！adb shell直接执行命令也一样，所以暂时不能支持5.0以上
		// 以Root权限执行命令
		RootUtil.postExecuteInThread(String.format("screencap -p %s", path), new RootUtil.OnExecCommandListener() {
			@Override
			public void onExecCommand(int result) {
				if (LOG) {
		        	Log.i(TAG, String.format("截图结果:%d", result));
		        }
				
				boolean ret = false;
				if (result == 0) { // 执行成功
					ret = true;
//					try {
//				    	File file = new File(filePath);
//				    	// 把文件插入到系统图库
//				        String url = MediaStore.Images.Media.insertImage(context.getContentResolver(), filePath, file.getName(), null); 
//				        if (url != null) {
//					        // 最后通知图库更新
//						    //sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
//						    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
//						    ret = true;
//				        }				        
//				    } catch (Exception e) {
//				        e.printStackTrace();
//				    }
				}			    
			    
			    // 回调，释放资源
			    listener.onScreenCap(ret);
			    listener = null;
			    context = null;
			}
		});
		return true;
	}

}
