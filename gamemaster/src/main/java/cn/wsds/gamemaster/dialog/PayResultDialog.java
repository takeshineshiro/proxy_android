package cn.wsds.gamemaster.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.pay.model.ProductDetail;
import cn.wsds.gamemaster.pay.model.Store;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.ActivityFeedback;
import cn.wsds.gamemaster.ui.ActivityMain;
import cn.wsds.gamemaster.ui.UIUtils;
import cn.wsds.gamemaster.ui.store.ActivityHistoryOrders;

public class PayResultDialog {

    private static abstract class ResultDialog{

        private final String PRODUCT_NAME_SEASON = "VIP加速套餐 — 季卡";
        private final String PRODUCT_NAME_MONTH = "VIP加速套餐 — 月卡";
        private final ProductDetail product;
        protected final int extras ;

        protected View view_success;
        protected View view_failed;
        protected TextView text_extras;
        protected TextView btn_left;
        protected TextView btn_right;
        protected Context context;

        private Dialog dialog ;

        private final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.button_left:
                        onLeftBtnClick();
                        break;
                    case R.id.button_right:
                        onRigtBtnClick();
                        break;
                    default:
                        break;
                }

                dialog.dismiss();
            }
        };

        ResultDialog(ProductDetail product , int extras){
            this.product = product;
            this.extras = extras;
            reportEvent(product, true);
        }

        void show(){
            Activity currentActivity = ActivityBase.getCurrentActivity();
            if((product == null)||(currentActivity == null)||
                    (currentActivity.isFinishing())){
                return ;
            }

            initView(currentActivity);
        }

        void initView(Context context){
            this.context = context ;

            View layout = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_pay_result, null);

            ((TextView)layout.findViewById(R.id.text_price)).setText(getPriceText());
            ((TextView)layout.findViewById(R.id.text_product_name))
                    .setText(getProductDesc(product.getFlag()));

            view_success = layout.findViewById(R.id.view_pay_success);
            view_failed =layout.findViewById(R.id.view_pay_failed);
            text_extras = (TextView)layout.findViewById(R.id.text_extras);

            btn_left = (TextView)layout.findViewById(R.id.button_left);
            btn_right = (TextView)layout.findViewById(R.id.button_right);

            btn_left.setOnClickListener(listener);
            btn_right.setOnClickListener(listener);

            showResult();
            showDialog(layout);
        }

        String getProductDesc(int flag){

            switch (flag){
                case Store.FLAG_SEASON:
                    return PRODUCT_NAME_SEASON;
                case Store.FLAG_MONTH:
                    return PRODUCT_NAME_MONTH;
                default:
                    return "";
            }
        }

        String getPriceText(){
            StringBuilder builder = new StringBuilder();
            builder.append("￥");
            builder.append(product.getPrice());

            return builder.toString();
        }

        void showDialog(View view){
            dialog = new AlertDialog.Builder(context)
                    .setView(view).create();

            dialog.show();
        }

        void dismiss(Dialog dialog){
            dialog.dismiss();
        }

        abstract void showResult();
        abstract void onLeftBtnClick();
        abstract void onRigtBtnClick();
    }

    public static void reportEvent(ProductDetail product, boolean payResult) {
        if (product == null) {
            return;
        }

        String productName = product.getProductName() ;
        if(TextUtils.isEmpty(productName)){
            return ;
        }
        switch (product.getFlag()){
            case Store.FLAG_SEASON:
                if (payResult) {
                    Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_QUARTER_SUCCESS);
                } else {
                    Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_QUARTER_CLICK);
                }
                break;
            case Store.FLAG_MONTH:
                if (payResult) {
                    Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_MONTH_SUCCESS);
                } else {
                    Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_MONTH_CLICK);
                }
                break;
            case Store.FLAG_FREE:
                if (payResult) {
                    Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_FREE_SUCCESS);
                } else {
                    Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_FREE_CLICK);
                }
                break;
            default:
                break;
        }
    }

    private static class SucessResult extends ResultDialog {

        SucessResult(ProductDetail product,int extras) {
            super(product,extras);
        }

        @Override
        void showResult() {
            view_success.setVisibility(View.VISIBLE);
            view_failed.setVisibility(View.GONE);
            btn_left.setText("查看订单");
            btn_right.setText("去加速");
            if(extras>0){
                text_extras.setVisibility(View.VISIBLE);
                text_extras.setText(getExtrasRmind());
            }
        }

        @Override
        void onLeftBtnClick() {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_PAYMENTS_SUCCESS, "查看订单");
            UIUtils.turnActivity(context, ActivityHistoryOrders.class);
        }

        @Override
        void onRigtBtnClick() {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_PAYMENTS_SUCCESS, "开始加速");
            UIUtils.turnActivity(context, ActivityMain.class);
        }

        String getExtrasRmind(){
           return String.format("另赠%d天VIP加速",extras);
        }
    }

    private static class FailedResult extends ResultDialog{
        FailedResult(ProductDetail product,int extras) {
            super(product,extras);
        }

        @Override
        void showResult() {
            view_success.setVisibility(View.GONE);
            view_failed.setVisibility(View.VISIBLE);
            btn_left.setText("遇到问题");
            btn_right.setText("重试");
        }

        @Override
        void onLeftBtnClick() {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_PAYMENTS_FAILED, "遇到问题");
            UIUtils.turnActivity(context, ActivityFeedback.class);
        }

        @Override
        void onRigtBtnClick() {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.VIPCENTER_PAYMENTS_FAILED, "重试");
        }
    }

    public static void show(ProductDetail product , boolean success ,int extras){

        ResultDialog resultDialog ;
        if(success){
            resultDialog = new SucessResult(product,extras);
        }else{
            resultDialog = new FailedResult(product,extras);
        }

        resultDialog.show();
    }
}
