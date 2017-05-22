package cn.wsds.gamemaster.debugger;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;

import com.subao.common.Misc;
import com.subao.common.data.SubaoIdManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import cn.wsds.gamemaster.GlobalDefines;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.AppNotificationManager;
import cn.wsds.gamemaster.dialog.CommonDesktopDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.net.RecentNetState;
import cn.wsds.gamemaster.tools.RootUtil;
import cn.wsds.gamemaster.ui.UIUtils;

public class FragmentDebug03 extends FragmentDebug {

	private EditText editDelayCloseVPN, editDelayReopenVPN;
	
	private Button buttonCopyAccelDataFile;

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_03;
	}
	
	private static class AccelDataFileCopycat extends AsyncTask<Void, Void, AccelDataFileCopycat.Result> {
		
		public static class Result {
			public final boolean okNodes;
			public final boolean okGames;
			public final boolean okParallelConfig;
			public final long timeCost;
			public Result(boolean okNodes, boolean okGames, boolean okParallelConfig, long timeCost) {
				this.okNodes = okNodes;
				this.okGames = okGames;
				this.okParallelConfig = okParallelConfig;
				this.timeCost = timeCost;
			}
		}
		
		private final WeakReference<FragmentDebug03> owner;
		
		public AccelDataFileCopycat(FragmentDebug03 owner) {
			this.owner = new WeakReference<FragmentDebug03>(owner);
		}
		
		private static boolean copy(String srcFilename, File destDir) {
			File srcFile = com.subao.utils.FileUtils.getDataFile(srcFilename);
			if (!srcFile.exists() || !srcFile.isFile()) {
				return false;
			}
			boolean result;
			InputStream input = null;
			OutputStream output = null;
			try {
				input = new FileInputStream(srcFile);
				output = new FileOutputStream(new File(destDir, srcFilename));
				byte[] buffer = new byte[4096];
				while (true) {
					int size = input.read(buffer);
					if (size <= 0) {
						break;
					}
					output.write(buffer, 0, size);
				}
				result = true;
			} catch (IOException e) {
				result = false;
			} finally {
				Misc.close(output);
				Misc.close(input);
			}
			return result;
		}
		
		@Override
		protected Result doInBackground(Void... params) {
			FragmentDebug03 owner = this.owner.get();
			if (owner == null) {
				return null;
			}
			long time = SystemClock.currentThreadTimeMillis();
			// FIXME: 17-3-29 hujd
//			boolean okNodes = AccelNodeListManagerImpl.getInstance().copyDataToPublicFile();
//			boolean okGames = AccelGameList.getInstance().copyDataToPublicFile();
//			boolean okParallelConfig = copy(ParallelConfig.FILE_NAME, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
			long timeCost = SystemClock.currentThreadTimeMillis() - time;
//			return new Result(okNodes, okGames, okParallelConfig, timeCost);
			return null;
		}
		
		@Override
		protected void onPostExecute(Result result) {
			if (result != null) {
				UIUtils.showToast(String.format("节点列表复制结果：%b\r\n游戏列表复制结果：%b\r\nWiFi加速配置复制结果：%b\r\n耗时：%.1fs",
					result.okNodes, result.okGames, result.okParallelConfig, result.timeCost * 0.001f));
			}
			FragmentDebug03 owner = this.owner.get();
			if (owner != null) {
				owner.buttonCopyAccelDataFile.setEnabled(true);
			}
		}
	}

	@Override
	protected void initView(View root) {
		View.OnClickListener clickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.debug_button_clear_root:
					RootUtil.clearRoot(new RootUtil.OnClearRootListener() {
						@Override
						public void onClearRoot(boolean result) {
							UIUtils.showToast(result ? "debug:清除Root权限成功" : "debug:清除Root权限失败");
						}
					});
					break;
				case R.id.debug_button_clear_subaoid:
					SubaoIdManager.getInstance().clear();
					UIUtils.showToast("清理完成");
					break;
				case R.id.debug_button_gameaccel_result:// 打开游戏加速推送通知
					turnSecondDebugPage(ActivityDebuggerSecondPage.VALUE_DEBUG_NOTICE_ACCEL_REUSLT);
					break;
				case R.id.debug_button_netdesc:
					debugReadNetDesc();
					break;
				case R.id.debug_button_notice_open_game_inside:
					AppNotificationManager.sendOpenGameInside();
					break;

				case R.id.debug_button_delay_value:
					turnSecondDebugPage(ActivityDebuggerSecondPage.VALUE_DEBUG_DELAY);
					break;
				case R.id.button_memory_clean:
					turnSecondDebugPage(ActivityDebuggerSecondPage.VALUE_DEBUG_MEMORY_CLEAN);
					break;
				case R.id.debug_button_ping:
					turnSecondDebugPage(ActivityDebuggerSecondPage.VALUE_DEBUG_PING);
					break;
				case R.id.debug_button_gameachieve:// 使用成就通知
					turnSecondDebugPage(ActivityDebuggerSecondPage.VALUE_DEBUG_NOTICE_GAME_ACHIEVE);
					break;
				case R.id.debug_button_copy_base_data:
					buttonCopyAccelDataFile.setEnabled(false);
					new AccelDataFileCopycat(FragmentDebug03.this).executeOnExecutor(com.subao.common.thread.ThreadPool.getExecutor());
					break;
				}
			}

			private void debugReadNetDesc() {
				CommonDialog dialog = new CommonDesktopDialog();
				String message = RecentNetState.instance.getRecentState().getDesc();
				dialog.setMessage(message);
				dialog.setPositiveButton("我知道了", null);
				dialog.show();
			}
		};
		root.findViewById(R.id.debug_button_clear_root).setOnClickListener(clickListener);
		root.findViewById(R.id.debug_button_clear_subaoid).setOnClickListener(clickListener);
		root.findViewById(R.id.debug_button_gameaccel_result).setOnClickListener(clickListener);
		root.findViewById(R.id.debug_button_netdesc).setOnClickListener(clickListener);
		root.findViewById(R.id.debug_button_gameachieve).setOnClickListener(clickListener);
		root.findViewById(R.id.debug_button_notice_open_game_inside).setOnClickListener(clickListener);
		root.findViewById(R.id.debug_button_delay_value).setOnClickListener(clickListener);
		root.findViewById(R.id.button_memory_clean).setOnClickListener(clickListener);
		root.findViewById(R.id.debug_button_ping).setOnClickListener(clickListener);
		buttonCopyAccelDataFile = (Button) root.findViewById(R.id.debug_button_copy_base_data);
		buttonCopyAccelDataFile.setOnClickListener(clickListener);
		//
		initDelayCloseVPN(root);
		//
		Switch switchClientPreCheck = (Switch) root.findViewById(R.id.debug_switch_client_pre_check);
		switchClientPreCheck.setChecked(GlobalDefines.CLIENT_PRE_CHECK);
		switchClientPreCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				GlobalDefines.CLIENT_PRE_CHECK = isChecked;
			}
		});
	}

	private void initDelayCloseVPN(View root) {
		editDelayCloseVPN = (EditText) root.findViewById(R.id.debug_edit_delay_close_vpn);
		editDelayReopenVPN = (EditText) root.findViewById(R.id.debug_edit_delay_reopen_vpn);
		Button buttonDelayCloseVPN = (Button) root.findViewById(R.id.debug_button_delay_close_vpn);
		buttonDelayCloseVPN.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				int delayClose;
				try {
					delayClose = Integer.parseInt(editDelayCloseVPN.getText().toString());
				} catch (NumberFormatException ex) {
					delayClose = 0;
				}
				if (delayClose <= 0) {
					UIUtils.showToast("请指定有效的“延时关闭秒”值（必须为正）");
					return;
				}
				//
				int delayReopen;
				try {
					delayReopen = Integer.parseInt(editDelayReopenVPN.getText().toString());
				} catch (NumberFormatException ex) {
					UIUtils.showToast("请指定“延时重连”值");
					return;
				}
				//
				MainHandler.getInstance().closeAccelDelayed(delayClose * 1000, delayReopen * 1000);
				UIUtils.showToast(delayReopen >= 0
					? String.format("%d秒后将关闭加速，并于关闭后%d秒重连", delayClose, delayReopen)
					: String.format("%d秒后将断开VPN", delayClose));
			}
		});
	}

}
