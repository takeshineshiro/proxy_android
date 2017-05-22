package cn.wsds.gamemaster.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.subao.common.Logger;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.MainHandler;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.pullrefresh.SubaoHeader;

public class PayResultWattiingMode {

    private static final String TAG = "PayWatting" ;

    private static class WaittingDialog {

        final int NOCLOSESHOWTIME = 10*1000;
        final int DIALOGSHOWTIME = 5*60*1000;
        Dialog dialog = null;
        SubaoHeader animation =null ;
        ImageView closeIcon = null;
        long startTime =  -1 ;

        final Runnable timer = new Runnable() {
            @Override
            public void run() {
                if (!dialogValid()){
                    return;
                }

                long showTime = SystemClock.elapsedRealtime() - startTime ;
                if (showTime <=DIALOGSHOWTIME) {
                    if(showTime>NOCLOSESHOWTIME){
                        closeEnable();
                    }

                    MainHandler.getInstance().postDelayed(this,1000);
                } else {
                    dismiss();
                }
            }
        };

        void show(Context context,boolean paying){
            refresh(context,paying);
            runnigStart();
        }

        void dismiss(){
            clearCurrentSatus();
        }

        void refresh(Context context,boolean paying){
            clearCurrentSatus();
            initView(context,paying);
        }

        void clearCurrentSatus(){
            if((dialog!=null)&&(dialog.isShowing())){

                if (animation!=null){
                    animation.onUIRefreshComplete(null);
                }

                dialog.dismiss();
            }

            dialog = null ;
            animation=null;
            closeIcon = null ;
            startTime =  -1;
        }

        void initView(Context context,boolean paying){
            if ((context==null)){
                Logger.d(TAG,"activity is invalid, can not show watting dialog!");
                return;
            }

            View layout = LayoutInflater.from(context)
                    .inflate(R.layout.layout_pay_result_watting,null);

            ((TextView) layout.findViewById(R.id.text_watting))
                    .setText(paying?"支付中…":"请稍候…");

            animation = (SubaoHeader)layout.findViewById(R.id.animation_layout);

            closeIcon = (ImageView)layout.findViewById(R.id.icon_close);
            closeIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_PAYMENTS_CLOSE_CLICK);
                }
            });

            initDialog(context,layout);
        }

        void initDialog(Context context ,View view){
            if(view==null){
                return;
            }

            dialog = new Dialog(context,R.style.AppDialogTheme);
            dialog.setContentView(view);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }

        void closeEnable(){
            if ((closeIcon == null)||(closeIcon.isShown())){
                return;
            }

            closeIcon.setVisibility(View.VISIBLE);
        }

        void runnigStart(){
            dialog.show();
            animation.onUIRefreshPrepare(null);
            animation.onUIRefreshBegin(null);
            postTimer();
        }

        void postTimer(){
            if(startTime<0){
                startTime = SystemClock.elapsedRealtime();
            }

            MainHandler.getInstance().postDelayed(timer,1000);
        }

        boolean dialogValid(){
            return ((dialog!=null)&&(dialog.isShowing())) ;
        }
    }

    private static final WaittingDialog wattiingDialog = new WaittingDialog();

    public static void start(Context context , boolean paying){
        wattiingDialog.show(context,paying);
    }

    public static void stop(){
        wattiingDialog.dismiss();
    }
}
