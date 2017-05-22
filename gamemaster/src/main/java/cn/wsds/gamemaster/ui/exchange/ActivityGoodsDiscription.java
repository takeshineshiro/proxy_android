package cn.wsds.gamemaster.ui.exchange;

import hr.client.appuser.CouponCenter;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.UserSession;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.net.http.Response;
import cn.wsds.gamemaster.net.http.ResponseHandler;
import cn.wsds.gamemaster.service.HttpApiService;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.UIUtils;

public class ActivityGoodsDiscription extends ActivityBase {

    private View textChange;
    private TextView textChangeCode;
    private View layoutExchange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_discription);
        String couponId = getIntent().getStringExtra("couponId");
        CouponCenter.AppCoupon couponData = ExchangeGoodsBuffer.getInstance().getCouponData(couponId);
        String title = couponData == null ? getTitle().toString() : couponData.getGameName();
        setDisplayHomeArrow(title == null ? getTitle() : title);
        initView(couponData);
        textChange = findViewById(R.id.text_exchange);
        layoutExchange = findViewById(R.id.layout_exchange);
        textChangeCode = (TextView) findViewById(R.id.text_exchange_code);
        findViewById(R.id.text_copy).setOnClickListener(new MyOnClickListener(this, couponId));
        findViewById(R.id.text_exchange).setOnClickListener(new MyOnClickListener(this, couponId));
    }


    private static class MyOnClickListener extends WeakReference<ActivityGoodsDiscription> implements View.OnClickListener {
        private final String couponId;

        public MyOnClickListener(ActivityGoodsDiscription activity, String couponId) {
            super(activity);
            this.couponId = couponId;
        }

        @Override
        public void onClick(View v) {
            ActivityGoodsDiscription activity = get();
            if (activity == null) {
                return;
            }
            switch (v.getId()) {
                case R.id.text_copy:
                    TextView textView = (TextView) activity.findViewById(R.id.text_exchange_code);
                    String str = textView.getText().toString().trim();
                    copyToClip(activity, str);
                    break;
                case R.id.text_exchange:
                    if (UserSession.isLogined()) {
                        Statistic.addEvent(activity, Statistic.Event.USER_EXCHANGE_CENTRE_CLICK_CHANGE, "礼包码");
                        HttpApiService.requestExchangeCoupon(couponId, new DefaultResponseHandler(activity));
                    } else {
                        UIUtils.showReloginDialog(activity, "兑换需要登录~", null);
                    }
                    break;
            }

        }
    }

    /**
     * 复制textview的内容到剪切板
     *
     * @param context
     */
    public static void copyToClip(Context context, String str) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copy", str);
        clipboard.setPrimaryClip(clip);
        UIUtils.showToast("已成功复制，快去游戏中粘贴吧");
    }

    private static class DefaultResponseHandler extends ResponseHandler {
        private static final String EXCHANGE_ERR_MESSAGE_NO_GOODS = "手慢一步，今日已兑完。明天再来试试吧~";
        private static final String EXCHANGE_ERR_MESSAGE_EXPIRE = "手慢一步，已过期/(ㄒoㄒ)/~";
        private static final String EXCHANGE_ERR_MESSAGE_POINTS = "积分不够/(ㄒoㄒ)/~";
        private static final String EXCHANGE_ERR_MESSAGE_EXCHANGED = "您已兑换过此物品";
        private static final String EXCHANGE_ERR_MESSAGE_SERVICE = "服务器错误，请稍候再试";
        private final WeakReference<ActivityGoodsDiscription> refActivity;
        private int resultCode = -1;
        private String couponContent;
        private int remainPoints;

        public DefaultResponseHandler(ActivityGoodsDiscription activity) {
            super(activity, new ReLoginOnHttpUnauthorizedCallBack(activity));
            this.refActivity = new WeakReference<ActivityGoodsDiscription>(activity);
        }

        @Override
        protected void onSuccess(Response response) {
            Activity activity = refActivity.get();
            if (activity == null) {
                return;
            }
            doParse(response.body);
            String resultStr = "失败";
            if (resultCode == 0) {
                resultStr = "成功";
                setExchangeCodeView(couponContent);
                UserSession.getInstance().updateSorce(remainPoints);
                activity.setResult(1);

            } else if (resultCode == 138 || resultCode == 137) {
                //礼包已被申请完
                resultStr += EXCHANGE_ERR_MESSAGE_NO_GOODS;
                errorHandler(EXCHANGE_ERR_MESSAGE_NO_GOODS, activity);
            } else if (resultCode == 141) {
                //礼包已过期
                resultStr += EXCHANGE_ERR_MESSAGE_EXPIRE;
                errorHandler(EXCHANGE_ERR_MESSAGE_EXPIRE, activity);
            } else if (resultCode == 140) {
                //积分不足兑换礼包
                resultStr += EXCHANGE_ERR_MESSAGE_POINTS;
                showErrorDialog(EXCHANGE_ERR_MESSAGE_POINTS, activity);
            } else if (resultCode == 139) {
                resultStr += EXCHANGE_ERR_MESSAGE_EXCHANGED;
                UIUtils.showToast(EXCHANGE_ERR_MESSAGE_EXCHANGED);
            } else {
                resultStr += EXCHANGE_ERR_MESSAGE_SERVICE;
                UIUtils.showToast(EXCHANGE_ERR_MESSAGE_SERVICE);
            }
            Statistic.addEvent(activity, Statistic.Event.USER_EXCHANGE_GAME_RESULT, resultStr);
        }

        private void errorHandler(String msg, Activity activity) {
            showErrorDialog(msg, activity);
            //通知刷新礼包列表
            activity.setResult(1);
        }

        private void showErrorDialog(CharSequence message, final Activity activity) {
            CommonAlertDialog builder = new CommonAlertDialog(activity);
            builder.setTitle("提示");
            builder.setMessage(message);
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.finish();
                }
            });
            builder.show();
        }

        private String doParse(byte[] body) {
            String jsonStr = new String(body);
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);
                if (jsonObject.has("resultCode")) {
                    resultCode = jsonObject.getInt("resultCode");
                }
                if (jsonObject.has("couponContent")) {
                    couponContent = jsonObject.getString("couponContent");
                }
                if (jsonObject.has("remainPoints")) {
                    remainPoints = jsonObject.getInt("remainPoints");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void setExchangeCodeView(String couponContent) {
            if (couponContent == null) {
                couponContent = "";
            }

            ActivityGoodsDiscription activity = refActivity.get();
            if (activity != null) {
                activity.setExchangeResultView(couponContent);
            }
        }


    }

    private void setExchangeResultView(String couponContent) {

        textChange.setVisibility(View.INVISIBLE);
        textChangeCode.setText(couponContent);
        layoutExchange.setVisibility(View.VISIBLE);
    }

    private void initView(CouponCenter.AppCoupon couponData) {
        if (couponData == null) {
            return;
        }

        String couponIconUrl = couponData.getCouponIconUrl();
        if (couponIconUrl != null && !"".equals(couponIconUrl)) {
            ImageView imageView = (ImageView) findViewById(R.id.image_game_icon);
            DownloadImage.displayImage(imageView, R.drawable.icon_present, couponIconUrl);
        }
        String couponName = couponData.getCouponName();
        if (couponName != null && !"".equals(couponName)) {
            setTextView(null, couponName, R.id.text_game_name);
        }
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        int remainderPercent = couponData.getRemainderPercent();
        progressBar.setProgress(remainderPercent);
        String str = String.format("剩余%d%s", remainderPercent, "%");
        setTextView(null, str, R.id.text_remainder);

        str = String.format("%d积分", couponData.getNeedPoints());
        SpannableStringBuilder textBuilder = SpannableStringUtils.getTextBuilder("", str, "积分".length(), getResources().getColor(R.color.color_game_11),
                getResources().getDimensionPixelSize(R.dimen.text_size_16));
        TextView textView = (TextView) findViewById(R.id.text_points);
        textView.setText(textBuilder);

        setGoodsContent(couponData);

        setGoodsPeriod(couponData);

        setGoodsMethods(couponData);
    }

    private void setGoodsMethods(CouponCenter.AppCoupon couponData) {
        String howToUse = couponData.getHowToUse();
        if (howToUse != null && !"".equals(howToUse)) {
            setIncludeView(R.id.include_goods_methods, "使用方法", howToUse);
//            String html="<p>@天狗 详细我们会做最好的农业网站，http://www.tngou.net 们会做最好的农业网站 <br>请关注@天狗网 网站http://www.tngou.net/news/show/1?id=2&cd=chenle 做做好的农业网站，加油。&nbsp;&nbsp;</p><p>坚信会做好的@tngou<br/></p>";
//            String url = parseUrlInHowToUse(html);
//            if(url) {
//            View view = findViewById(R.id.include_goods_methods);
//            TextView contentView = (TextView) view.findViewById(R.id.text_content);
//            contentView.setClickable(true);
//            contentView.setMovementMethod(LinkMovementMethod.getInstance());
//            String text = "Jaons blog http://hujiandong.com baidu:<a href='http://www.baidu.com'> www.baidu.com </a>";
//            contentView.setText(Html.fromHtml(howToUse));
//                int start = html.indexOf(url);
//                SpannableStringBuilder builder = new SpannableStringBuilder(html);
//                builder.setSpan(
//                        new ForegroundColorSpan(getResources().getColor(R.color.color_game_11)), start, start + url.length(),
//                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
//                builder.setSpan(new UnderlineSpan(), start, start + url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                contentView.setText(builder);
        }
    }


//    private String parseUrlInHowToUse(String howToUse) {
//        String reg = "(http[s]?://([\\w-]+\\.)+[\\w-]+([\\w-./?%&*=]*))";
//        Pattern p = Pattern.compile(reg);
//        Matcher m = p.matcher(howToUse);
//
//        while (m.find()) {
//            String url = m.group();
//            Log.d("hujd", "url" + url);
//            return url;
//        }
//
//        return null;
//    }

    private void setGoodsPeriod(CouponCenter.AppCoupon couponData) {
        CouponCenter.TimeRange timeRange = couponData.getUseTime();
        if (timeRange == null) {
            return;
        }

        String from = TimeRangeFormatter.format(timeRange.getFrom());
        String to = TimeRangeFormatter.format(timeRange.getTo());

        setIncludeView(R.id.include_goods_period, "礼包使用期限", from + "-" + to);
    }

    private void setGoodsContent(CouponCenter.AppCoupon couponData) {
        String couponDescription = couponData.getCouponDescription();
        if (couponDescription != null && !"".equals(couponDescription)) {
            setIncludeView(R.id.include_goods_content, "礼包内容", couponDescription);
        }
    }

    private void setIncludeView(int id, String title, String content) {
        View view = findViewById(id);

        setTextView(view, content, R.id.text_content);
        setTextView(view, title, R.id.text_goods_title);
    }

    private void setTextView(View view, String str, int id) {
        TextView textView;
        if (view != null) {
            textView = (TextView) view.findViewById(id);
        } else {
            textView = (TextView) findViewById(id);
        }
        if(id == R.id.text_content) {
            textView.setClickable(true);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(Html.fromHtml(str));
        } else {
            textView.setText(str);
        }
    }

}
