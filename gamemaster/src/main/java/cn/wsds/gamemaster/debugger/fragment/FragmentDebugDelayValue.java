package cn.wsds.gamemaster.debugger.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.ui.UIUtils;

public class FragmentDebugDelayValue extends Fragment {

	private EditText editFirstDelay, editUIDForSecondDelay, editSecondDelay, editUIDForDirectTrans, editPort,
		editDirectTransDelay;

	private final View.OnClickListener onClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.test_button_first_delay:
				doSetFirstDelay();
				break;
			case R.id.test_button_second_delay:
				doSetSecondDelay();
				break;
			case R.id.test_button_direct_trans:
				doSetDirectTrans();
				break;
			}
		}

		private void doSetFirstDelay() {
			Integer delay = getIntValueFromEdit(editFirstDelay);
			GameManager.getInstance().setTestValue_FirstSegmentDelay(delay);
			UIUtils.showToast(delay == null ? "已清除对第一段延迟值的锁定" : "锁定第一段延迟值: " + delay);
		}

		private void doSetSecondDelay() {
			Integer uid = getIntValueFromEdit(editUIDForSecondDelay);
			if (uid == null) {
				UIUtils.showToast("请指定UID");
				return;
			}
			Integer delay = getIntValueFromEdit(editSecondDelay);
			if (delay == null) {
				UIUtils.showToast("请指定第二段时延值");
				return;
			}
			GameManager.getInstance().onSecondSegmentNetDelayChange(uid, delay, true);
			UIUtils.showToast(String.format("UID：%d\n第二段时延值：%d", uid, delay));
		}

		private void doSetDirectTrans() {
			Integer uid = getIntValueFromEdit(editUIDForDirectTrans);
			if (uid == null) {
				UIUtils.showToast("请指定UID");
				return;
			}
			Integer port = getIntValueFromEdit(editPort);
			if (port == null) {
				UIUtils.showToast("请指定端口号");
				return;
			}
			Integer delay = getIntValueFromEdit(editDirectTransDelay);
			if (delay == null) {
				UIUtils.showToast("请指定透传值");
				return;
			}
			GameManager.getInstance().onDirectTrans(uid, port, delay);
			UIUtils.showToast(String.format("透传：UID=%d, 端口=%d, 延迟=%d", uid, port, delay));
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_debugger_delay_value, container, false);
		editFirstDelay = (EditText) view.findViewById(R.id.test_edit_first_delay);
		editUIDForSecondDelay = (EditText) view.findViewById(R.id.test_edit_uid_for_second_delay);
		editSecondDelay = (EditText) view.findViewById(R.id.test_edit_second_delay);
		editUIDForDirectTrans = (EditText) view.findViewById(R.id.test_edit_uid_for_direct_trans);
		editPort = (EditText) view.findViewById(R.id.test_edit_port);
		editDirectTransDelay = (EditText) view.findViewById(R.id.test_edit_direct_trans_delay);
		//
		view.findViewById(R.id.test_button_first_delay).setOnClickListener(onClickListener);
		view.findViewById(R.id.test_button_second_delay).setOnClickListener(onClickListener);
		view.findViewById(R.id.test_button_direct_trans).setOnClickListener(onClickListener);
		//
		initCtrlsValue();
		return view;
	}

	private void initCtrlsValue() {
		Integer n = GameManager.getInstance().getTestValue_FirstSegmentDelay();
		if (n != null) {
			editFirstDelay.setText(n.toString());
		}
	}

	private static Integer getIntValueFromEdit(EditText editText) {
		String s = editText.getText().toString().trim();
		if (s.length() != 0) {
			try {
				return Integer.parseInt(s);
			} catch (NumberFormatException nfe) {

			}
		}
		editText.setText(null);
		return null;
	}

}
