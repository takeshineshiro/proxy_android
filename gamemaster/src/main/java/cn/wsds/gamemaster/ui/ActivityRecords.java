package cn.wsds.gamemaster.ui;

import android.os.Bundle;
import android.widget.ListView;
import cn.wsds.gamemaster.R;

public abstract class ActivityRecords extends ActivityBase {
	
	protected ListView listRecords;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getContentViewId());
		listRecords = (ListView) findViewById(R.id.list_records);
		listRecords.setEmptyView(findViewById(R.id.records_text_empty));
	}

	protected abstract int getContentViewId();
	
}
