package cn.wsds.gamemaster.ui.doublelink;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.UIUtils;


@SuppressLint("InflateParams")
public class UserFeedbackUtil{

	public static void doForUserFeedback(final Context context){
		 if(context == null ){
			 return ;
		 }		
		 
		 final View layout = LayoutInflater.from(context)
				 .inflate(R.layout.dialog_user_feedback, null);
		 
		 final AlertDialog dialog = new AlertDialog.Builder(context) 
	              .setView(layout).create();
		 
		 layout.findViewById(R.id.image_close).setOnClickListener(new OnClickListener(){	 
			public void onClick(View v) {			 
				dialog.dismiss();
			}		 
		 });
		 
		 layout.findViewById(R.id.button_ok).setOnClickListener(new OnClickListener(){	 
				public void onClick(View v) {	
					String params = "";
					
					if(((RadioButton)layout.findViewById(R.id.radio_excellent)).isChecked()){
				        params = "效果好" ;
					}else if(((RadioButton)layout.findViewById(R.id.radio_just_ok)).isChecked()){
						params = "有一些效果" ;
					}else if(((RadioButton)layout.findViewById(R.id.radio_useless)).isChecked()){
						params = "没有效果" ;
					}else{
						UIUtils.showToast("您还没有选择意见");
						   return ;
					}
					 
					Statistic.addEvent(context, 
							Statistic.Event.INTERACTIVE_DUAL_NETWORK_INVEST, params);
					UIUtils.showToast("感谢您的意见");
					dialog.dismiss();
				}		 
		 });
				 
		 dialog.setCanceledOnTouchOutside(false);
		 dialog.show();	 
	 }
}
