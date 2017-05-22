package cn.wsds.gamemaster.share.ui;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.share.GameMasterShareManager;
import cn.wsds.gamemaster.share.GameMasterShareManager.ShareType;
import cn.wsds.gamemaster.share.ShareCallBackObservable;
import cn.wsds.gamemaster.share.ShareObserver;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.user.UserTaskManager;
import cn.wsds.gamemaster.ui.user.UserTaskManager.ReportShareResponseHandler;
import cn.wsds.gamemaster.ui.user.UserTaskManager.ResponseHandlerCreater;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskIdentifier;
import cn.wsds.gamemaster.ui.user.UserTaskManager.TaskRecord;

public class ShareLayout {

	public static interface OnShareClickListener {
		public void onClick(ShareType type);
	}

	private static enum PointsIcon {
		NONE,
		ICON_10_POINTS,
//		ICON_30_POINTS,
	}

	private static class ShareItem {

		private final View view;
		private ImageView iconPoints;

		public ShareItem(View view) {
			this.view = view;
		}

		public void setOnClickListener(View.OnClickListener l) {
			this.view.setOnClickListener(l);
		}

		public void setPointsIcon(PointsIcon icon) {
			if (icon == PointsIcon.NONE) {
				if (iconPoints != null) {
					iconPoints.setVisibility(View.GONE);
				}
				return;
			}
			if (iconPoints == null) {
				ViewStub stub = (ViewStub) view.findViewById(R.id.stub_icon_points);
				iconPoints = (ImageView) stub.inflate();
			}
		}
	}

	private final View rootView;
	private final OnShareClickListener onShareClickListener;
	private final List<ShareItem> shareItems = new ArrayList<ShareItem>(8);

	public static View createView(Context context, OnShareClickListener onShareClickListener) {
		ShareLayout inst = new ShareLayout(context, onShareClickListener);
		return inst.rootView;
	}

	@SuppressLint("InflateParams") private ShareLayout(Context context, OnShareClickListener onShareClickListener) {
		this.rootView = LayoutInflater.from(context).inflate(R.layout.layout_share, null, false);
		this.onShareClickListener = onShareClickListener;
		initShareItems();
	}

	private void initShareItems() {
		ShareItem shareItem;
		shareItem = new ShareItem(rootView.findViewById(R.id.share_penyouquan));
		shareItems.add(shareItem);
		shareItem = new ShareItem(rootView.findViewById(R.id.share_qq));
		shareItems.add(shareItem);
		shareItem = new ShareItem(rootView.findViewById(R.id.share_qqzone));
		shareItems.add(shareItem);
		shareItem = new ShareItem(rootView.findViewById(R.id.share_sina));
		shareItems.add(shareItem);
		shareItem = new ShareItem(rootView.findViewById(R.id.share_weixin));
		shareItems.add(shareItem);
		//
		View.OnClickListener shareOnClick = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (onShareClickListener == null) {
					return;
				}
				GameMasterShareManager.ShareType shareType = getShareType(view.getId());
				if (shareType != null) {
					onShareClickListener.onClick(shareType);
				}
			}
		};
		for (ShareItem si : shareItems) {
			si.setOnClickListener(shareOnClick);
		}
		setTaskPointsVisibility(isShowTaskPointsVisibility());
	}

	/**
	 * 设置分享的分数是否可见
	 * 
	 * @param visibility
	 *            是否可见
	 */
	private void setTaskPointsVisibility(boolean visibility) {
		PointsIcon icon;
		if (visibility) {
			icon = PointsIcon.ICON_10_POINTS;
		} else {
			icon = PointsIcon.NONE;
		}
		for (ShareItem item : shareItems) {
			item.setPointsIcon(icon);
		}
	}

	/**
	 * 叠加在分享目标上面的积分图标是否要显示？
	 * 
	 * @return true=要显示积分图标
	 */
	private boolean isShowTaskPointsVisibility() {
		if (!UserSession.isLogined()) {
			return true;
		}
		List<TaskRecord> taskRecords = UserTaskManager.getInstance().getTaskRecord(TaskIdentifier.share);
		for (TaskRecord taskRecord : taskRecords) {
			if (!taskRecord.isTaskProgressElemsEmpty()) {
				return false;
			}
		}
		return true;
	}

	private GameMasterShareManager.ShareType getShareType(int id) {
		switch (id) {
		case R.id.share_weixin:
			return ShareType.ShareToWeixin;
		case R.id.share_penyouquan:
			return ShareType.ShareToFriends;
		case R.id.share_qq:
			return ShareType.ShareToQQ;
		case R.id.share_qqzone:
			return ShareType.ShareToZone;
		case R.id.share_sina:
			return ShareType.ShareToSina;
		default: //注意，不要添加其他点击事件
			return null;
		}
	}

	public static abstract class DefaultShareObserver implements ShareObserver {

		@Override
		public void callbackResult(ShareType shareType, int resultCode) {
			ShareCallBackObservable.getInstance().removeShareObserver(this);
			//反馈结果处理
			switch (resultCode) {
			case ShareObserver.CALLBACK_CODE_SUCCESS:
				onShareSuccess(shareType);
				break;
			case ShareObserver.CALLBACK_CODE_CANCEL:
				UIUtils.showToast(R.string.toast_share_cancel);
				break;
			case ShareObserver.CALLBACK_CODE_DENY:
			case ShareObserver.CALLBACK_CODE_UNKNOWN:
				UIUtils.showToast(R.string.toast_share_fail);
				break;
			}
		}

		/**
		 * @param shareType
		 */
		private final void onShareSuccess(ShareType shareType) {
			UIUtils.showToast(R.string.toast_share_succeed);
			if (UserSession.isLogined()) {
				String checkPointKey = getCheckPointKey(shareType);
				UserTaskManager.doReuqestActionTaskFinish(TaskIdentifier.share, checkPointKey, new ResponseHandlerCreater() {

					@Override
					public ReportShareResponseHandler create(TaskRecord taskRecord) {
						return new ReportShareResponseHandler(getActivity(), taskRecord);
					}
				});
			} else {
				UIUtils.showToast(R.string.thank_your_share);
			}
		}

		public abstract Activity getActivity();

		private static String getCheckPointKey(ShareType shareType) {
			switch (shareType) {
			case ShareToFriends:
				return "moments";
			case ShareToQQ:
			case ShareToSina:
			case ShareToWeixin:
			case ShareToZone:
			default:
				return "defaultPoints";
			}
		}

	}
}
