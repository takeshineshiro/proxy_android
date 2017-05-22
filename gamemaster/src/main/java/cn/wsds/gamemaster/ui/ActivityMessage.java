package cn.wsds.gamemaster.ui;

import java.util.Observable;
import java.util.Observer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.event.JPushMessageReceiver;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.message.MessageManager.Record;
import cn.wsds.gamemaster.message.MessageManager.RecordList;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.adapter.MessageAdapter;
import cn.wsds.gamemaster.ui.doublelink.ActivityDoubleLink;

/**
 * 消息中心页面
 */
public class ActivityMessage extends ActivityRecords {

	private MessageAdapter adapter;

	private final Observer messageManagerObserver = new Observer() {

		@Override
		public void update(Observable observable, Object data) {
			adapter.notifyDataSetChanged();
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setDisplayHomeArrow(this.getTitle());
		this.registerForContextMenu(this.listRecords);
		RecordList data_list = MessageManager.getInstance().getRecordList();
		adapter = new MessageAdapter(this, data_list);
		this.listRecords.setAdapter(adapter);
		this.listRecords.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
				Record record = MessageManager.getInstance().getRecordList().get(position);
				ActivityMessage.this.onItemClick(record);
			}
		});
		MessageManager.getInstance().addObserver(messageManagerObserver);
	}
	
	@Override
	protected void reportEvent(Bundle bundle) {
		if(JPushMessageReceiver.jpushTrunMessage(bundle)){
			Statistic.addEvent(this, Statistic.Event.INTERACTIVE_MESSAGE_IN,true); 			
			return ; 
		} 
			
		Statistic.addEvent(this, Statistic.Event.INTERACTIVE_MESSAGE_IN,false);	 		
	}

	@Override
	protected int getContentViewId() {
		return R.layout.activity_message;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		MessageManager.getInstance().deleteObserver(messageManagerObserver);
	}

	private void onItemClick(Record record) {
		if(record==null){
			return ;
		}
		
		Statistic.addEvent(this, Statistic.Event.INTERACTIVE_MESSAGE_CLICK,record.title);
		
		MessageManager.getInstance().getRecordList().markToRead(record.id);
		switch (record.type) {
            case MessageManager.Record.TYPE_PREVENT_CLEAN:
			ActivityMessageDetail.show(this, ActivityMessageDetail.TYPE_PREVENT_CLEAN);
			break;
		case MessageManager.Record.TYPE_HELP:
			UIUtils.turnActivity(this, ActivityInstructions.class);
			break;
		case MessageManager.Record.TYPE_GOTO_QA:
			UIUtils.turnActivity(this, ActivityQA.class);
			break;
		case MessageManager.Record.TYPE_GOTO_FEEDBACK_REPLY:
			Intent intent = new Intent(this, ActivityFeedbackReply.class);
			intent.putExtra(IntentExtraName.NOTICE_INTENT_EXTRANAME_EXTRA_DATA, record.extra);
			startActivity(intent);
			break;
		case MessageManager.Record.TYPE_GRAPHICS_TEXT_MIXED:
			Intent intentMixed = new Intent(this, ActivityMessageGraphicsTextMixed.class);
			intentMixed.putExtra(IntentExtraName.NOTICE_INTENT_EXTRANAME_ID, record.id);
			startActivity(intentMixed);
			break;
		case MessageManager.Record.TYPE_URL:
		case MessageManager.Record.TYPE_HTML:
		case MessageManager.Record.TYPE_JPUSH_NOTIFY_URL:
			ActivityMessageView.show(this, record,false);
			break;
        case Record.TYPE_GOTO_DOUBLE_LINK:
            Intent intentId = new Intent(this, ActivityDoubleLink.class);
            intentId.putExtra(IntentExtraName.NOTICE_INTENT_EXTRANAME_ID, record.id);
            startActivity(intentId);
            break;
        case MessageManager.Record.TYPE_JPUSH_NOTIFY_TEXT:
        	ActivityJpushMessage.show(this, record.title, record.extra);
        	break;
//		case MessageManager.Record.TYPE_GOTO_QUESTION_SUVERY:
//			UIUtils.turnActivity(this, ActivityQuestionSurvey.class);
//			break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		Record record = getRecordFromMenuInfo(menuInfo);
		if (record != null) {
			menu.setHeaderTitle(record.title);
			menu.add("删除本条消息");
		}
	}

	private Record getRecordFromMenuInfo(ContextMenu.ContextMenuInfo menuInfo) {
		int position = ((AdapterContextMenuInfo) menuInfo).position;
		Record record = (Record) adapter.getItem(position);
		return record;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Record record = getRecordFromMenuInfo(item.getMenuInfo());
		if (record != null) {				
			MessageManager.getInstance().getRecordList().deleteWithId(record.id);
			Statistic.addEvent(this, Statistic.Event.INTERACTIVE_MESSAGE_DELETE,record.title);			
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_recovery:
			tryDropAll();
			break;
		case android.R.id.home:
			this.finish();
			break;
		default:
			onContextItemSelected(item);
			return false;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.information_actionbar_menu, menu);
		return true;
	}

	private void tryDropAll() {
		if (MessageManager.getInstance().getRecordList().isEmpty()) {
			UIUtils.showToast("没有消息可删除");
		} else {
			new DialogDropAll().show();
		}
	}

	/**
	 * 清除所有消息
	 */
	private class DialogDropAll implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
		public void show() {
			CommonDialog dlg = new CommonAlertDialog(ActivityMessage.this);
			dlg.setMessage("是否确认删除所有消息？");
			dlg.setPositiveButton(R.string.cancel, this);
			dlg.setNegativeButton("删除所有", this);
			dlg.setOnCancelListener(this);
			dlg.show();
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			addUserAction(false);
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			boolean drop = (which == DialogInterface.BUTTON_NEGATIVE);
			addUserAction(drop);
			if (drop) {		
				MessageManager.getInstance().getRecordList().clear();
				Statistic.addEvent(ActivityMessage.this, Statistic.Event.INTERACTIVE_MESSAGE_DELETE,"all");				
			}
		}

		private void addUserAction(boolean confirmDrop) {}
	}
	
	@Override
	protected ActivityType getPreActivityType() {
		return ActivityType.USER_CENTER;
	}
}
