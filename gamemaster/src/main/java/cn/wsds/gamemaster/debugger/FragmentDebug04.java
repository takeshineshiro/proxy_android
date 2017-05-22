package cn.wsds.gamemaster.debugger;

import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;

import com.subao.common.net.IPv4;
import com.subao.common.net.NetUtils;
import com.subao.common.net.Protocol;

import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.qos.QosTools;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.vpn.JniCallbackProcesser;

public class FragmentDebug04 extends FragmentDebug {

	private EditText editId, editSrcIp, editSrcPort, editDstIp, editDstPort;
	private EditText editAccelTime;

	private RadioGroup radioGroupProtocol;

	private class OnButtonClickListener implements View.OnClickListener {

		private int parseSessionId() {
			try {
				return Integer.parseInt(editId.getText().toString().trim());
			} catch (NumberFormatException e) {
				throw new NumberFormatException("请填写正确的会话ID");
			}
		}

		private int parseAccelTime() {
			try {
				return Integer.parseInt(editAccelTime.getText().toString());
			} catch (NumberFormatException e) {
				throw new NumberFormatException("请输入正确的加速时长");
			}
		}

		private void doOpen() {
			int id = parseSessionId();
			//
			updatePrivateIp();
			//
			int srcPort;
			try {
				srcPort = Integer.parseInt(editSrcPort.getText().toString());
			} catch (NumberFormatException e) {
				srcPort = randomPrivatePort();
			}
			//
			String destIp = editDstIp.getText().toString().trim();
			if (TextUtils.isEmpty(destIp)) {
				throw new RuntimeException("请填写正确的目标IP");
			}
			//
			int destPort;
			try {
				destPort = Integer.parseInt(editDstPort.getText().toString().trim());
				if (destPort <= 0 || destPort > 65535) {
					throw new RuntimeException();
				}
			} catch (RuntimeException e) {
				throw new RuntimeException("请填写正确的目标端口");
			}
		}

		private void doModify() {
			int id = parseSessionId();
			int timeSeconds = parseAccelTime();
			
			//TODO : need node and accessToken value ;
			JniCallbackProcesser.doModifyQosAccel(id, null ,null, timeSeconds);
		}

		private void doClose() {
		}

		@Override
		public void onClick(View v) {
			try {
				switch (v.getId()) {
				case R.id.button_qos_request_open:
					doOpen();
					break;
				case R.id.button_qos_request_close:
					doClose();
					break;
				case R.id.button_qos_request_modify:
					doModify();
					break;
				}
			} catch (RuntimeException e) {
				UIUtils.showToast(e.getMessage());
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updatePrivateIp();
	}

	@Override
	protected int getRootLayoutResId() {
		return R.layout.fragment_debug_04;
	}

	private static String getPrivateIp() {
		return IPv4.ipToString(NetUtils.getLocalIp(new QosTools.DefaultLocalIpFilter()));
	}

	@Override
	protected void initView(View root) {
		Switch alwaysSucceedSwitcher = (Switch) root.findViewById(R.id.debug_switch_qos_always_succeed);
		// FIXME: 17-3-29 hujd
		//alwaysSucceedSwitcher.setChecked(QosHelper.getDebugAlwaysSucceedSwitch());
		alwaysSucceedSwitcher.setChecked(true);
		alwaysSucceedSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				try {
					//QosHelper.setDebugAlwaysSucceedSwitch(isChecked);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		//
		editId = (EditText) root.findViewById(R.id.edit_session_id);
		editSrcIp = (EditText) root.findViewById(R.id.edit_src_ip);
		editSrcPort = (EditText) root.findViewById(R.id.edit_src_port);
		editDstIp = (EditText) root.findViewById(R.id.edit_dest_ip);
		editDstPort = (EditText) root.findViewById(R.id.edit_dest_port);
		radioGroupProtocol = (RadioGroup) root.findViewById(R.id.radio_group_protocol);
		editAccelTime = (EditText) root.findViewById(R.id.edit_accel_time);
		//
		View.OnClickListener l = new OnButtonClickListener();
		root.findViewById(R.id.button_qos_request_open).setOnClickListener(l);
		root.findViewById(R.id.button_qos_request_modify).setOnClickListener(l);
		root.findViewById(R.id.button_qos_request_close).setOnClickListener(l);
		//
		randomPrivatePort();
	}

	private int randomPrivatePort() {
		int port = 1024 + (int) (System.currentTimeMillis() % 50000);
		editSrcPort.setText(Integer.toString(port));
		return port;
	}

	private void updatePrivateIp() {
		try {
			editSrcIp.setText(getPrivateIp());
		} catch (Exception e) {
			editSrcIp.setText(null);
		}
	}

}
