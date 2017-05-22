package cn.wsds.gamemaster.ui;

import java.util.Observable;
import java.util.Observer;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.message.MessageManager;

public class MenuIconMarker_Message extends MenuIconMarker {

	/** 观察{@link MessageManager} */
	private Observer observer;

	/** 是否有未读消息？ */
	private boolean hasUnreadMessages;

	private static boolean hasUnreadMessagesNow() {
		return MessageManager.getInstance().getRecordList().hasUnread();
	}

	@Override
	protected void onAttachActivity() {
		if (observer == null) {
			observer = new Observer() {
				@Override
				public void update(Observable observable, Object data) {
					if (hasUnreadMessages != hasUnreadMessagesNow()) {
						refreshMenu();
					}
				}
			};
			MessageManager.getInstance().addObserver(observer);
		}
	}

	@Override
	protected void onDetachActivity() {
		if (observer != null) {
			MessageManager.getInstance().deleteObserver(observer);
			observer = null;
		}
	}

	@Override
	protected void recheckState() {
		this.hasUnreadMessages = hasUnreadMessagesNow();
	}

	@Override
	protected boolean isStateStriking() {
		return this.hasUnreadMessages;
	}

	@Override
	protected int getResIdNormal() {
		return R.drawable.title_message;
	}

	@Override
	protected int getResIdStriking() {
		return R.drawable.title_message_new;
	}

}
