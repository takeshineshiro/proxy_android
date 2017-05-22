package cn.wsds.gamemaster.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.SelfUpgrade;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;

import com.subao.common.LogTag;
import com.subao.common.Logger;
import com.subao.common.Misc;
import com.subao.common.net.Http;
import com.subao.common.thread.ThreadPool;
import com.subao.common.utils.StringUtils;
import com.subao.net.NetManager;
import com.subao.upgrade.PortalUpgradeConfig;

public class DialogUpgrade extends CommonAlertDialog {

	private static final String TAG = LogTag.UPGRADE;
	private static final float KB = 1024f;
	private static final float MB = KB * 1024;

	private final PortalUpgradeConfig.Item item;

	private final ViewFlipper flipper;

	private final View buttonOk, buttonCancel;
	private final CheckBox checkBoxIgnoreThisVersion;

	private final ProgressBar progressBar;
	private final ImageView buttonAbortDownload;
	private final TextView textSpeed, textProgress;

	private Downloader downloader;

	public DialogUpgrade(Activity activity, int currentVer, int minVer, PortalUpgradeConfig.Item item, boolean ignoreCheckBoxVisible) {
		super(activity);
		this.item = item;
		this.setContentView(R.layout.dialog_upgrade);
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(false);
		//
		this.flipper = (ViewFlipper) findViewById(R.id.flipper);
		//
		// 版本号、发布时间、大小
		setText(R.id.text_version_name, item.version);
		setText(R.id.text_publish_time, item.publishTime);
		setText(R.id.text_size, String.format("%3.2f MB", item.size / MB));
		// 更新内容
		setText(R.id.text_content, item.instructions);
		// 按钮
		buttonCancel = findViewById(R.id.button_cancel);
		buttonOk = findViewById(R.id.button_ok);
		checkBoxIgnoreThisVersion = (CheckBox) findViewById(R.id.check_ignore_this_version);
		if (currentVer < minVer) {
			checkBoxIgnoreThisVersion.setVisibility(View.GONE);
			buttonCancel.setVisibility(View.GONE);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(buttonOk.getLayoutParams());
			lp.weight = 0;
			lp.width = LayoutParams.MATCH_PARENT;
			lp.leftMargin += 80;
			lp.rightMargin += 80;
			buttonOk.setLayoutParams(lp);
		} else {
			if (!ignoreCheckBoxVisible) {
				checkBoxIgnoreThisVersion.setVisibility(View.GONE);
			}
			buttonCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (checkBoxIgnoreThisVersion.isChecked()) {
						SelfUpgrade.getInstance().ignoreVersion(DialogUpgrade.this.item);
					}
					dismiss();
				}
			});
		}
		//		
		buttonOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!NetManager.getInstance().isConnected()) {
					showErrorBox("更新包下载", "网络未连接，无法下载更新包");
					return;
				}
				if (downloader == null) {
					setProgressCtrls(0, DialogUpgrade.this.item.size);
					setTextSpeed(0);
					downloader = new Downloader(v.getContext());
					downloader.executeOnExecutor(ThreadPool.getExecutor());
					flipper.setDisplayedChild(1);
				}
			}
		});
		//
		// 下载相关的控件
		this.textSpeed = (TextView) findViewById(R.id.text_speed);
		this.textProgress = (TextView) findViewById(R.id.text_progress);
		this.progressBar = (ProgressBar) findViewById(R.id.progress_download);
		this.buttonAbortDownload = (ImageView) findViewById(R.id.button_abort_download);
		this.buttonAbortDownload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (downloader != null) {
					if (!downloader.isCancelled()) {
						downloader.cancel(true);
					}
					downloader = null;
					flipper.setDisplayedChild(0);
				}
			}
		});
	}

	private void setText(int resId, CharSequence text) {
		((TextView) findViewById(resId)).setText(text);
	}

	private void setProgressCtrls(int currentSize, int totalSize) {
		if (currentSize > totalSize) {
			currentSize = totalSize;
		}
		this.textProgress.setText(String.format("%3.2fMB/%3.2fMB", currentSize / MB, totalSize / MB));
		int percent = (totalSize <= 0) ? 0 : (currentSize * 100 / totalSize);
		this.progressBar.setProgress(percent);
	}

	private void setTextSpeed(float speed) {
		this.textSpeed.setText(String.format("%3.2fKB/s", speed));
	}

	private static long now() {
		return SystemClock.elapsedRealtime();
	}

	private void showErrorBox(String title, String message) {
		Activity activity = getActivity();
		if (activity != null) {
			CommonAlertDialog dlg = new CommonAlertDialog(getActivity());
			dlg.setTitle(title);
			dlg.setMessage(message);
			dlg.setCancelable(true);
			dlg.setCanceledOnTouchOutside(true);
			dlg.show();
		} else {
			UIUtils.showToast(message);
		}
	}

	/////////////////////////////////////////////////

	private static final int PROGRESS_SUCCEED = 0;
	private static final int PROGRESS_WAIT_FOR_DOWNLOAD = 1;
	private static final int PROGRESS_DOWNLOADING = 2;
	private static final int PROGRESS_VERIFY = 3;
	private static final int PROGRESS_VERIFY_ERROR = 4;
	private static final int PROGRESS_DOWNLOAD_FAIL = 5;

	private class Downloader extends AsyncTask<Void, Integer, Integer> {

		private final Context context;
		private final File fileLocal;
		private final MessageDigest messageDigest;

		public Downloader(Context context) {
			this.context = context.getApplicationContext();
			this.fileLocal = createLocalFile(context);
			this.messageDigest = createMessageDigest();
		}

		private MessageDigest createMessageDigest() {
			if (item.md5 != null && item.md5.length() == 32) {
				try {
					return MessageDigest.getInstance("MD5");
				} catch (NoSuchAlgorithmException ignored) {}
			}
			return null;
		}

		private File createLocalFile(Context context) {
			File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			if (!dir.exists() || !dir.isDirectory()) {
				dir.mkdirs();
			}
			File file = new File(dir, String.format("xunyou_gamemaster_%s_vc%d_%s.apk", item.version, item.verCode, item.md5));
			return file;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			if (Logger.isLoggableDebug(TAG)) {
				Log.d(TAG, "Download start: " + item.url);
			}
			try {
				publishProgress(PROGRESS_WAIT_FOR_DOWNLOAD);
				download();
				if (messageDigest != null) {
					publishProgress(PROGRESS_VERIFY);
					byte[] md5 = messageDigest.digest();
					if (md5 == null || !StringUtils.toHexString(md5, false).equalsIgnoreCase(item.md5)) {
						return PROGRESS_VERIFY_ERROR;
					}
				}
				return PROGRESS_SUCCEED;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			return PROGRESS_DOWNLOAD_FAIL;
		}

		@Override
		protected void onCancelled() {
			cleanup();
		}

		@Override
		protected void onPostExecute(Integer result) {
			switch (result) {
			case PROGRESS_VERIFY_ERROR:
				Log.w(TAG, "MD5 is not equals");
				showErrorBox("校验错误", "下载后的更新包校验错误，请重新下载。");
				break;
			case PROGRESS_SUCCEED:
				doSucceed();
				break;
			case PROGRESS_DOWNLOAD_FAIL:
				doFail("下载失败", "下载更新包失败，请稍后重试。");
				break;
			}
			cleanup();
		}

		private void cleanup() {
			if (this == downloader) {
				downloader = null;
			}
		}

		private void download() throws IOException {
			URL url = new URL(item.url);
			HttpURLConnection conn = new Http(8000, 8000).createHttpUrlConnection(url, Http.Method.GET, null);
			try {
				int responseCode = Http.getResponseCode(conn);
				if (responseCode < 200 || responseCode >= 300) {
					throw new IOException("Response code unknown");
				}
				OutputStream output = new FileOutputStream(fileLocal);
				try {
					int totalBytes = conn.getContentLength();
					int currentBytes = 0;
					InputStream input = conn.getInputStream();
					try {
						byte[] buffer = new byte[16 * 1024];
						long beginTime = now();
						while (!isCancelled()) {
							int readBytes = input.read(buffer);
							if (readBytes <= 0) {
								break;
							}
							currentBytes += readBytes;
							output.write(buffer, 0, readBytes);
							if (messageDigest != null) {
								messageDigest.update(buffer, 0, readBytes);
							}
							publishProgress(PROGRESS_DOWNLOADING, currentBytes, totalBytes, (int) (now() - beginTime));
						}
					} finally {
						Misc.close(input);
					}
				} finally {
					Misc.close(output);
				}
			} finally {
				conn.disconnect();
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			switch (values[0]) {
			case PROGRESS_WAIT_FOR_DOWNLOAD:
				textSpeed.setText("等待下载开始...");
				break;
			case PROGRESS_DOWNLOADING:
				int currentBytes = values[1];
				int totalBytes = values[2];
				if (totalBytes <= 0) {
					totalBytes = item.size;
				}
				int time = values[3];
				setProgressCtrls(currentBytes, totalBytes);
				float speed = (currentBytes <= 0 || time <= 0) ? 0f : (currentBytes / KB) / (time * 0.001f);
				setTextSpeed(speed);
				break;
			case PROGRESS_VERIFY:
				textSpeed.setText("正在校验下载到的更新包...");
				break;
			}
		}

		private void doSucceed() {
			try {
				Intent intent = new Intent();
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(this.fileLocal), "application/vnd.android.package-archive");
				context.startActivity(intent);
				AppMain.exit(false);
			} catch (RuntimeException e) {
				doFail("更新包安装", "无法启动更新包安装");
			}
		}

		private void doFail(String title, String message) {
			buttonAbortDownload.performClick();
			showErrorBox(title, message);
		}

	}
}
