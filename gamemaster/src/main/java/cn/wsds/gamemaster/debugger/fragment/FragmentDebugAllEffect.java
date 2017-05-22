package cn.wsds.gamemaster.debugger.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;

public class FragmentDebugAllEffect extends Fragment {

	private EditText editTimeConnectionRepair, editTimeAccelSucceed, editTimeFlowException;
	
	private static class Param {
		public final EditText editText;
		public final int messageId;
		public Param(EditText editText, int messageId) {
			this.editText = editText;
			this.messageId = messageId;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_debugger_all_effect, container, false);
		this.editTimeConnectionRepair = (EditText)view.findViewById(R.id.edit_time_connection_repair);
		this.editTimeAccelSucceed = (EditText)view.findViewById(R.id.edit_time_accel_succeed);
		this.editTimeFlowException = (EditText)view.findViewById(R.id.edit_time_flow_exception);
		view.findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				execute();
			}
		});
		return view;
	}
	
	private void execute() {
		Param[] params = new Param[] {
			new Param(editTimeConnectionRepair, MainHandler.MSG_SHOW_EFFECT_CONNECTION_REPAIR),
			new Param(editTimeAccelSucceed, MainHandler.MSG_SHOW_EFFECT_ACCEL_SUCCEED),
			new Param(editTimeFlowException, MainHandler.MSG_SHOW_EFFECT_FLOW_EXCEPTION),
		};
		MainHandler mainHandler = MainHandler.getInstance();
		for (Param p : params) {
			mainHandler.removeMessages(p.messageId);
			long time = getValueFromEditText(p.editText);
			if (time > 0) {
				mainHandler.sendEmptyMessageDelayed(p.messageId, time * 1000);
			}
		}
		this.getActivity().finish();
	}
	
	private static long getValueFromEditText(EditText et) {
		String s = et.getText().toString().trim();
		if (s.length() == 0) {
			return 0;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
