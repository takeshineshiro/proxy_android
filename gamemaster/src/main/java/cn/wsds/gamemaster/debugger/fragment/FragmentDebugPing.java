package cn.wsds.gamemaster.debugger.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.accel.AccelOpenManager;
import cn.wsds.gamemaster.ui.adapter.AdapterConsole;

public class FragmentDebugPing extends Fragment {

	private static final int MAX_LINES = 64;
	private EditText editAddress;
	private Button buttonPing;
	private ListView listView;
	private AdapterConsole adapter;

	private boolean running;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_debugger_ping, container, false);
		editAddress = (EditText) root.findViewById(R.id.edit_address);
		buttonPing = (Button) root.findViewById(R.id.button_ping);
		buttonPing.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String address = editAddress.getText().toString().trim();
				if (address.length() == 0) {
					UIUtils.showToast("请提供要PING的目标地址");
					return;
				}
				if (AccelOpenManager.isStarted() && !AccelOpenManager.isRootModel()) {
					UIUtils.showToast("VPN加速开启状态下无法PING");
					return;
				}
				if (running) {
					adapter.cleanup();
				} else {
					adapter.execute("ping", address);
				}
				changeState(!running);
			}
		});
		//
		listView = (ListView) root.findViewById(R.id.listview_console);
		adapter = new AdapterConsole(getActivity(), listView, MAX_LINES, true, new AdapterConsole.Listener() {
			@Override
			public void onStop(boolean isCancelled) {
				changeState(false);
			}
		});
		listView.setAdapter(adapter);
		//
		return root;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		adapter.cleanup();
	}

	private void changeState(boolean running) {
		if (running != this.running) {
			this.running = running;
			if (running) {
				buttonPing.setText("停止");
			} else {
				buttonPing.setText("PING");
			}
			editAddress.setEnabled(!running);
		}
	}
}
