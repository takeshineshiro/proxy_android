package cn.wsds.gamemaster.debugger.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ResUsageChecker;
import cn.wsds.gamemaster.ResUsageChecker.ResUsage;
import cn.wsds.gamemaster.ui.UIUtils;

public class FragmentDebugMemoryClean extends Fragment {

	private EditText editMemoryUsage,editCpuUsage/*,editAppUsage*/;
	private final View.OnClickListener viewClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_confirm:
				if(setMemoryUsage()){
					UIUtils.showToast("设置成功");
				}
				break;
			case R.id.button_cancel:
				editMemoryUsage.setText("");
				editCpuUsage.setText("");
				ResUsageChecker.getInstance().setDebugData(null);
				break;
			}
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_debugger_memory_clean, container, false);
		editMemoryUsage = (EditText) view.findViewById(R.id.edit_memory_usage);
		editCpuUsage = (EditText) view.findViewById(R.id.edit_cpu_usage);
//		editAppUsage = (EditText) view.findViewById(R.id.edit_app_usage);
		
		ResUsage debugUsage = ResUsageChecker.getInstance().getDebugUsage();
		if(debugUsage!=null){
			editMemoryUsage.setText(String.valueOf(debugUsage.memoryUsage));
			editCpuUsage.setText(String.valueOf(debugUsage.cpuUsage));
		}
		view.findViewById(R.id.button_confirm).setOnClickListener(viewClickListener);
		view.findViewById(R.id.button_cancel).setOnClickListener(viewClickListener);
		return view;
	}

	protected boolean setMemoryUsage() {
		String memoryUsage = editMemoryUsage.getText().toString();
		if(TextUtils.isEmpty(memoryUsage)){
			editMemoryUsage.setError("必填项");
			return false;
		}
		String cpuUsage = editCpuUsage.getText().toString();
		if(TextUtils.isEmpty(cpuUsage)){
			editCpuUsage.setError("必填项");
			return false;
		}
		ResUsage resUsage = new ResUsage(Integer.valueOf(cpuUsage), Integer.valueOf(memoryUsage), null);
		ResUsageChecker.getInstance().setDebugData(resUsage);
		return true;
	}
}
