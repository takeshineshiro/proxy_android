package cn.wsds.gamemaster.ui.doublelink;

import java.util.ArrayList;
import java.util.List;

import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import cn.wsds.gamemaster.AppMain;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.app.GameInfo;
import cn.wsds.gamemaster.app.GameManager;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.DoubleAccelTimeRecords;
import cn.wsds.gamemaster.data.DoubleLinkUseRecords;
import cn.wsds.gamemaster.data.IntentExtraName;
import cn.wsds.gamemaster.message.MessageManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.tools.VPNUtils;
import cn.wsds.gamemaster.ui.ActivityBase;
import cn.wsds.gamemaster.ui.exchange.SpannableStringUtils;
import cn.wsds.gamemaster.ui.view.Switch;

public class ActivityDoubleLink extends ActivityBase {

    private ListView listGames;
    private View closeView;
    private Switch checkDoubelLink;
    
    private static final String TAG = "ActivityDoubleLink" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDisplayHomeArrow(R.string.double_accel);
        initLayout();
    }

    private void initLayout() {

        int recordId = getIntent().getIntExtra(IntentExtraName.NOTICE_INTENT_EXTRANAME_ID, 0);
        if(ConfigManager.getInstance().isDoubleAccelFirst() || recordId == MessageManager.ID_GOTO_DOUBLE_LINK) {
            initLayoutFirst(recordId);
        } else {
            initView();
        }
    }

    private void initLayoutFirst(int recordId) {
        setContentView(R.layout.activity_double_link_first);
        if(recordId == MessageManager.ID_GOTO_DOUBLE_LINK) {
            findViewById(R.id.click_text).setVisibility(View.INVISIBLE);
            return;
        }
        findViewById(R.id.click_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initView();
            }
        });

        MessageManager.getInstance().markMessageDoubleReaded();

        ConfigManager.getInstance().setDoubleAccelEnter(true);
    }

    private void initView() {
        setContentView(R.layout.activity_double_link);
        listGames = (ListView) findViewById(R.id.list_game);
        List<DoubleAccelTimeRecords.Record> records = getData();

        listGames.setEmptyView(findViewById(R.id.double_link_text_empty));
        if(records != null && records.size() != 0) {
            DoubleLinkDescAdapter adapter = new DoubleLinkDescAdapter(this, records);
            listGames.setAdapter(adapter);
        }

        initSwithView();

        setAccelTimeView();

        setUsedFlowView();

        //displayDoubleAccelPormpt();

        setUserFeedBack();
    }

    private void setUserFeedBack() {

        if (!ConfigManager.getInstance().isUserFeedBackDoubleAccel() &&
                DoubleLinkUseRecords.getInstance().getUsedFlowTotal() > 2 * DoubleLinkUseRecords.MB) {
            UserFeedbackUtil.doForUserFeedback(this);
            ConfigManager.getInstance().setUserFeedBackDoubleAccel();
        }
    }

//    private void displayDoubleAccelPormpt() {
//        if(0 == (ConfigManager.getInstance().getHelpUIStatus() & ConfigManager.HELP_UI_STATUS_DOUBLE_ACCEl_PORMPT)) {
//            final View view = findViewById(R.id.group_double_accel);
//            view.post(new Runnable() {
//                @Override
//                public void run() {
//                    DoubleAccelPormpt.open(view);
//
//                }
//            });
//        }
//    }

    private void setUsedFlowView() {
        TextView text = (TextView) findViewById(R.id.flow_used_text);
        long totalUsedFlow = ConfigManager.getInstance().getLastFlowOfDoubleAccel();
        SpannableStringBuilder textBuilder = SpannableStringUtils.getTextBuilder(DoubleLinkDescAdapter.setFormatDecimal(totalUsedFlow), "M", 0,
                this.getResources().getColor(R.color.color_game_7), getResources().getDimensionPixelSize(R.dimen.text_size_12));
        text.setText(textBuilder);
    }

    private void setAccelTimeView() {
        TextView text = (TextView) findViewById(R.id.double_link_count);
        long totalTime = DoubleAccelTimeRecords.getInstance().getTotalTime();
        SpannableStringBuilder textBuilder = SpannableStringUtils.getTextBuilder(UtilFormatTime.formatDoubleAccelTime(totalTime), "", 0,
                this.getResources().getColor(R.color.color_game_7), getResources().getDimensionPixelSize(R.dimen.text_size_12));
        text.setText(textBuilder);
    }

    private TextView mTextView;

    private void initSwithView() {
        checkDoubelLink = (Switch) findViewById(R.id.check_double_link);
        closeView = findViewById(R.id.double_link_close);
        mTextView = (TextView) findViewById(R.id.text_name);
        setSwithView();
        checkDoubelLink.setOnChangedListener(new Switch.OnChangedListener() {
            @Override
            public void onCheckedChanged(Switch checkSwitch, boolean checked) {
                ConfigManager.getInstance().setDoubleAccelStatus(checked);
                setTextView(checked);
                setCloseView(checked);
                uploadFirstStartDoubleAccel(checked);
                //VPNManager.getInstance().sendUnionAccelSwitch(checked);
                VPNUtils.sendUnionAccelSwitch(checked, TAG);
                uploadDoubleAccelSwithEvent(checked);
                //StatisticDefault.addEvent(ActivityDoubleLink.this, StatisticDefault.Event.INTERACTIVE_DUAL_NETWORK_SWITCH_CLICK, status);
            }
        });
    }

    /**
     * 上报并联加速开关状态事件
     * @param checked
     */
    public static void uploadDoubleAccelSwithEvent(boolean checked) {
        if(checked) {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_DUAL_NETWORK_SWITCH_ON, "打开双链路");
        } else {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.FLOATING_WINDOW_DUAL_NETWORK_SWITCH_OFF, "关闭双链路");
        }
    }

    /**
     * 上报首次启动双链路event
     * @param checked
     */
    public static void uploadFirstStartDoubleAccel(boolean checked) {
        if(checked && ConfigManager.getInstance().isFirstStartDoubleAccel()) {
            Statistic.addEvent(AppMain.getContext(), Statistic.Event.BACKSTAGE_DUAL_NETWORK_USER_PHONE, Build.MODEL);
            ConfigManager.getInstance().setNotFirstStartDoubleAccel();
        }
    }

    private void setSwithView() {
        if(checkDoubelLink == null) {
            return;
        }
        boolean doubleAccel = ConfigManager.getInstance().isEnableDoubleAccel();
        setCloseView(doubleAccel);
        setTextView(doubleAccel);
        checkDoubelLink.setChecked(doubleAccel);
    }


    private void setCloseView(boolean doubleAccel) {
        if(doubleAccel) {
            closeView.setVisibility(View.INVISIBLE);
        } else {
            closeView.setVisibility(View.VISIBLE);
        }
    }

    private void setTextView(boolean doubleAccel) {
        if(doubleAccel) {
            mTextView.setText(getResources().getString(R.string.double_accel_enable));
        } else {
            mTextView.setText(getResources().getString(R.string.double_accel_disable));
        }
    }

	private List<DoubleAccelTimeRecords.Record> getData() {
		List<DoubleAccelTimeRecords.Record> list = new ArrayList<DoubleAccelTimeRecords.Record>(10);
		DoubleAccelTimeRecords records = DoubleAccelTimeRecords.getInstance();
		for (DoubleAccelTimeRecords.Record record : records) {
			GameInfo gameInfo = GameManager.getInstance().getGameInfo(record.getPackageName());
			if (gameInfo == null || !gameInfo.isInstalled()) {
				continue;
			}
			list.add(record);
		}
		return list;
	}


    @Override
    protected void onStart() {
        super.onStart();
        setSwithView();
    }
}
