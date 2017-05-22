package cn.wsds.gamemaster.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.Misc;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.AccelGameList;
import cn.wsds.gamemaster.data.InstalledAppManager;
import cn.wsds.gamemaster.data.UserFeedback;
import cn.wsds.gamemaster.tools.ProcessLauncher;
import cn.wsds.gamemaster.tools.ReportFeedback;
import cn.wsds.gamemaster.ui.view.LoadingRing;

import com.subao.net.NetManager;

public class NewGameSubmit {

	private final HashSet<String> appNames;

	private final Context context;
	private final View root;
	private final ViewGroup flipper;
	private final EditText editGameName;
	private final TextView textSubmitProgress;
	private final LoadingRing loadingRing;
	private final Button btnClose, btnDownload;

	private AlertDialog dialog;
	private Page currentPage;

	private void changePage(Page newPage) {
		if (currentPage != newPage) {
			if (currentPage != null) {
				currentPage.cleanup();
			}
			currentPage = newPage;
			if (currentPage != null) {
				currentPage.init();
				int idx = currentPage.getPageIndex();
				flipper.getChildAt(idx).setVisibility(View.VISIBLE);
				for (int i = flipper.getChildCount() - 1; i > idx; --i) {
					flipper.getChildAt(i).setVisibility(View.GONE);
				}
				for (int i = 0; i < idx; ++i) {
					flipper.getChildAt(i).setVisibility(View.GONE);
				}
			}
		}
	}

	private abstract class Page {
		public abstract int getPageIndex();

		public abstract void init();

		public abstract void cleanup();

		public abstract void onSubmitClick();

	}

	private class PageInputGame extends Page {

		@Override
		public int getPageIndex() {
			return 0;
		}

		@Override
		public void init() {
			editGameName.setText(null);
		}

		@Override
		public void cleanup() {}

		@Override
		public void onSubmitClick() {
			String gameName = editGameName.getText().toString().trim();
			editGameName.setText(gameName);
			if (gameName.length() == 0) {
				UIUtils.showToast("您还没有输入游戏名字");
				return;
			}
			if (gameName.length() < 2) {
				UIUtils.showToast("游戏名字有误");
				return;
			}
			if (!isGameNameValid(gameName)) {
				return;
			}
			if (NetManager.getInstance().isDisconnected()) {
				UIUtils.showToast("当前网络不可用，请检查您的网络后再尝试");
				return;
			}
			changePage(new PageSubmit());
		}

		private boolean isGameNameValid(String gameName) {
			gameName = gameName.toLowerCase(Locale.US);
			// 含用“助手”“攻略”之类的关键词吗？
			AccelGameList aglManager = AccelGameList.getInstance();
			if (aglManager.doesAppLabelIncludeBlackWord(gameName)) {
				changePage(new PageIsNotGame());
				return false;
			}
			// 是已知的“非游戏”吗？
			if (appNames.contains(gameName)) {
				changePage(new PageIsNotGame());
				return false;
			}
			// 是已支持的游戏吗？
			if (null != aglManager.findAccelGame(null, gameName)) {
				changePage(new PageGameAlreadySupported());
				return false;
			}
			return true;
		}
	}

	private abstract class PageStatic extends Page {
		@Override
		public void init() {}

		@Override
		public void cleanup() {}

		@Override
		public void onSubmitClick() {}
	}

	private class PageIsNotGame extends PageStatic {

		@Override
		public int getPageIndex() {
			return 1;
		}

		@Override
		public void init() {
			if (InstalledAppManager.getInstance().getUidOfWSDS() > 0) {
				UIUtils.setViewText(btnDownload, "立即打开");
			} else {
				UIUtils.setViewText(btnDownload, "立即下载");
			}
		}

	}

	private class PageGameAlreadySupported extends PageStatic {
		@Override
		public int getPageIndex() {
			return 2;
		}
	}

	private class PageSubmit extends Page {

		private boolean succeeded;

		@Override
		public int getPageIndex() {
			return 3;
		}

		@Override
		public void init() {
			UIUtils.setViewVisibility(btnClose, View.INVISIBLE);
			UIUtils.setViewVisibility(loadingRing, View.VISIBLE);
			loadingRing.start(new LoadingRing.OnCompleteListener() {
				@Override
				public void onComplete() {
					UIUtils.setViewVisibility(loadingRing, View.INVISIBLE);
					UIUtils.setViewVisibility(btnClose, View.VISIBLE);
					UIUtils.setViewText(textSubmitProgress, succeeded ? "提交成功！\n请等待工作人员审核。" : "提交失败，请稍后再试");
				}
			});
			//
			UserFeedback userFeedback = UserFeedback.createFeedback(
				String.format("游戏添加：%s", editGameName.getText().toString()), null);
			ReportFeedback reportFeedback = new ReportFeedback(ReportFeedback.buildProtoDefault(userFeedback),
				new ReportFeedback.Callback() {
					@Override
					public void onEnd(boolean result) {
						succeeded = result;
						loadingRing.requestStop();
					}
				});
			reportFeedback.execute(com.subao.common.thread.ThreadPool.getExecutor());
		}

		@Override
		public void cleanup() {
			loadingRing.requestStop();
		}

		@Override
		public void onSubmitClick() {}

	}

	private final View.OnClickListener onButtonClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			if (currentPage == null) {
				return;
			}
			switch (v.getId()) {
			case R.id.button_submit:
				currentPage.onSubmitClick();
				break;
			case R.id.button_download:
				openOrDownload();
				//$FALL-THROUGH$
			case R.id.button_cancel:
			case R.id.button_ok:
			case R.id.button_close:
				dismissDialog();
				break;
			}
		}

		private void openOrDownload() {
			int uid = InstalledAppManager.getInstance().getUidOfWSDS();
			if (uid > 0) {
				if (ProcessLauncher.execute(AppMain.getContext(), "com.subao.husubao")) {
					return;
				}
			}
			//
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.wsds.cn"));
			NewGameSubmit.this.context.startActivity(intent);
		}
	};

	public NewGameSubmit(Context context) {
		this.context = context;
		appNames = loadAppNames(context);
		//
		root = View.inflate(context, R.layout.dialog_add_game, null);
		flipper = (ViewGroup) root.findViewById(R.id.flipper);
		editGameName = (EditText) root.findViewById(R.id.edit_game_name);
		loadingRing = (LoadingRing) root.findViewById(R.id.loading_ring);
		textSubmitProgress = (TextView) root.findViewById(R.id.text_submit_progress);
		btnDownload = (Button) root.findViewById(R.id.button_download);
		btnClose = (Button) root.findViewById(R.id.button_close);
		//
		Resources res = context.getResources();
		int color1 = res.getColor(R.color.color_game_7);
		int color2 = res.getColor(R.color.color_game_11);
		SpannableStringBuilder ssb = Misc.buildSpannableStringFromArrays(null,
			res.getStringArray(R.array.new_game_submit_text_1), color1, color2);
		((TextView) root.findViewById(R.id.text_input_game_name)).setText(ssb);
		ssb.clear();
		ssb = Misc.buildSpannableStringFromArrays(ssb, res.getStringArray(R.array.new_game_submit_text_2), color1,
			color2);
		((TextView) root.findViewById(R.id.text_is_not_game)).setText(ssb);
		//
		root.findViewById(R.id.button_submit).setOnClickListener(onButtonClickListener);
		root.findViewById(R.id.button_cancel).setOnClickListener(onButtonClickListener);
		root.findViewById(R.id.button_ok).setOnClickListener(onButtonClickListener);
		btnClose.setOnClickListener(onButtonClickListener);
		btnDownload.setOnClickListener(onButtonClickListener);
	}

	private static HashSet<String> loadAppNames(Context context) {
		HashSet<String> names = new HashSet<String>(1000);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(context.getAssets().open("app_name.txt")));
			while (true) {
				String name = reader.readLine();
				if (name == null) {
					break;
				}
				if (name.length() > 0) {
					name = name.trim();
					if (name.length() > 0) {
						names.add(name.toLowerCase(Locale.US));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {}
			}
		}
		return names;
	}

	public void execute() {
		if (dialog != null) {
			return;
		}
		dialog = new AlertDialog.Builder(context).setView(root).create();
		dialog.setCanceledOnTouchOutside(true);
		changePage(new PageInputGame());
		dialog.show();
	}

	private void dismissDialog() {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
	}

}
