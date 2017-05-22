//package cn.wsds.gamemaster.data;
//
//import cn.wsds.gamemaster.tools.DataUploader;
//import cn.wsds.gamemaster.ui.UIUtils;
//
//import com.subao.utils.FileUtils;
//
//public class ErrorLogUploader {
//
//	private static final ErrorLogUploader instance = new ErrorLogUploader();
//	
//	public static ErrorLogUploader getInstance() {
//		return instance;
//	}
//	
//	private class Callback implements DataUploader.OnUploadCompletedCallback {
//
//
//		@Override
//		public boolean onUploadCompleted(boolean succeeded, byte[] data) {
//			callback = null;
//			if (succeeded) {
//				FileUtils.deleteErrorLog();
//				UIUtils.showToast("错误日志上传成功");
//			} else {
//				UIUtils.showToast("上传失败，请重试");
//			}
//			return false; // DataUplodaer不要再重试了，上层自己会处理
//		}
//		
//	}
//	
//	private Callback callback;
//	
//	private ErrorLogUploader() { }
//	
//	/** 是不是正在上传中？ */
//	public boolean isRunning() { return callback != null; }
//	
//	/** 执行上传操作 */
//	public boolean execute(byte[] data) {
//		if (callback != null) {
//			return false;
//		}
//		callback = new Callback();
//		DataUploader.getInstance().addErrorLog(data, callback);
//		return true;
//	}
//}
