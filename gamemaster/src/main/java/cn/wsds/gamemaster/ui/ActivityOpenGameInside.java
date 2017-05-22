package cn.wsds.gamemaster.ui;

import android.content.Intent;
import android.os.Bundle;
import cn.wsds.gamemaster.data.IntentExtraName;

public class ActivityOpenGameInside extends ActivityBase {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this.getApplicationContext(), ActivityMain.class);
		intent.putExtra(IntentExtraName.START_FROM_OPEN_GAME_INSIDE, true);
		this.startActivity(intent);
		this.finish();
	}

}
