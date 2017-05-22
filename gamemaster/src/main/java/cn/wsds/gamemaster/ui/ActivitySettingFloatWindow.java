package cn.wsds.gamemaster.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.data.ConfigManager;
import cn.wsds.gamemaster.data.FloatWindowMeasure;
import cn.wsds.gamemaster.dialog.CommonAlertDialog;
import cn.wsds.gamemaster.dialog.CommonDialog;
import cn.wsds.gamemaster.dialog.UsageStateHelpDialog;
import cn.wsds.gamemaster.event.TaskManager;
import cn.wsds.gamemaster.statistic.Statistic;
import cn.wsds.gamemaster.statistic.StatisticUtils;
import cn.wsds.gamemaster.tools.AppsWithUsageAccess;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil;
import cn.wsds.gamemaster.tools.MobileSystemTypeUtil.SystemType;
import cn.wsds.gamemaster.ui.mainfloatwindow.OpenHelpManager;
import cn.wsds.gamemaster.ui.view.OpenFloatWindowHelpDialog;
import cn.wsds.gamemaster.ui.view.Switch;
import cn.wsds.gamemaster.ui.view.Switch.OnChangedListener;

/**
 * 设置子界面 --- 悬浮窗设置
 */
public class ActivitySettingFloatWindow extends ActivityBase {

    private View measurePormpt;
    private RadioGroup measureTypeGroup;
    private View floatwindowVisiableDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDisplayHomeArrow(R.string.float_wnd_switch);
        setContentView(R.layout.activity_setting_floatwindow);

        findViewById(R.id.float_wnd_pormpt).setOnClickListener(new FloatWndGroupClickListener((RadioGroup) findViewById(R.id.floatwindow_mode)));
        //尺寸设置
        measurePormpt = findViewById(R.id.text_pormpt);
        measureTypeGroup = (RadioGroup) findViewById(R.id.floatwindow_type_group);
        int resIdOfRadioNeedCheek;

        switch (FloatWindowMeasure.getCurrentType()) {
            case MINI:
                resIdOfRadioNeedCheek = R.id.radio_mini;
                break;
            case LARGE:
                resIdOfRadioNeedCheek = R.id.radio_large;

                break;
            case NORMAL:
            default:
                resIdOfRadioNeedCheek = R.id.radio_normal;

                break;
        }

        measureTypeGroup.check(resIdOfRadioNeedCheek);

        measureTypeGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                FloatWindowMeasure.Type size;
                switch (checkedId) {
                    case R.id.radio_mini:
                        size = FloatWindowMeasure.Type.MINI;
                        break;
                    case R.id.radio_large:
                        size = FloatWindowMeasure.Type.LARGE;
                        break;
                    case R.id.radio_normal:
                    default:
                        size = FloatWindowMeasure.Type.NORMAL;
                        break;
                }
                FloatWindowMeasure.setCurrentType(size);
				StatisticUtils.statisticFloatwindowType(ActivitySettingFloatWindow.this, size);
            }
        });

        floatwindowVisiableDelay = findViewById(R.id.floatwindow_visiable_delay);
        boolean showFloatWindowInGame = ConfigManager.getInstance().getShowFloatWindowInGame();
        visiableStyleVersion(showFloatWindowInGame);

        // 悬浮窗
        Switch floatWndSwitch = (Switch) findViewById(R.id.check_float_wnd);
        floatWndSwitch.setChecked(showFloatWindowInGame);
        floatWndSwitch.setOnBeforeCheckChangeListener(new FloatSwitchBeforeCheckChangeListener(floatWndSwitch));

        Switch switchVisiableDelay = (Switch) findViewById(R.id.check_visible_delay);
        switchVisiableDelay.setChecked(ConfigManager.getInstance().getFloatwindowSwitchDelay());
        switchVisiableDelay.setOnChangedListener(new OnChangedListener() {

            @Override
            public void onCheckedChanged(Switch checkSwitch, boolean checked) {
                ConfigManager.getInstance().setFloatwindowSwitchDelay(checked);
            }
        });

        initOpenGuide();
    }

    @Override
    protected void onStop() {
        super.onStop();
        OpenFloatWindowHelpDialog.close();
    }

    /**
     * 初始化悬浮窗引导相关组件
     */
    private void initOpenGuide() {
        boolean doesPhoneHaveFloatWindowLimit = doesPhoneHasFloatWindowLimit();		// 该品牌型号的手机是否有悬浮窗管理（限制）模块
        boolean doesOsHaveAppUsageAccessModule = AppsWithUsageAccess.hasModule();	// 该OS（Android）版本是否有最近应用访问模块
        TextView textOpenFloatWindowPrompt = (TextView) findViewById(R.id.text_guide_pormpt);	// 提示开启悬浮窗的文本
        
        // 如果两个条件均不满足，整个容器内的控件都不显示了
		if (!doesPhoneHaveFloatWindowLimit && !doesOsHaveAppUsageAccessModule) {
            findViewById(R.id.openguide_group).setVisibility(View.GONE);
            textOpenFloatWindowPrompt.setVisibility(View.GONE);
            return;
        }

		View impowerFloatwindow = findViewById(R.id.button_impower_floatwindow);
        View impowerUsageAccess = findViewById(R.id.button_impower_usage_access);

        // 针对悬浮窗管理模块的
        if (doesPhoneHaveFloatWindowLimit) {
            impowerFloatwindow.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    OpenFloatWindowHelpDialog.open(ActivitySettingFloatWindow.this, MobileSystemTypeUtil.getSystemProp(), null);
                    int count = ConfigManager.getInstance().getOpenFloatWindowHelpPageCount() + 1;
                    ConfigManager.getInstance().setOpenFloatWindowHelpPageCount(count);
                }
            });
        } else {
            impowerFloatwindow.setVisibility(View.GONE);
        } 
        
        // 针对最近应用程序访问的
        if (doesOsHaveAppUsageAccessModule) {
            impowerUsageAccess.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                	UsageStateHelpDialog.open(ActivitySettingFloatWindow.this, null);
                }
            });
        } else {
            impowerUsageAccess.setVisibility(View.GONE);
        }

        if (doesPhoneHaveFloatWindowLimit && doesOsHaveAppUsageAccessModule) {
        	// 有两个按钮
            textOpenFloatWindowPrompt.setText("找不到悬浮窗？请确保以下两处都已授权");
        } else {
        	// 只有一个按钮
            findViewById(R.id.space_middle).setVisibility(View.GONE);
            findViewById(R.id.space_left).setVisibility(View.VISIBLE);
            findViewById(R.id.space_right).setVisibility(View.VISIBLE);
        }
    }

    /** 根据型号品牌判断本手机是否有悬浮窗管理（限制）功能 */
    private static boolean doesPhoneHasFloatWindowLimit() {
        SystemType systemType = MobileSystemTypeUtil.getSystemProp().type;
        
        if (SystemType.EMUI == systemType || SystemType.MIUI == systemType) {
            return true;
        }
        //魅族的新型号手机也需要
        if(SystemType.MX==systemType){ 
        	String prop = MobileSystemTypeUtil.getSystemProp().prop ;
			return OpenHelpManager.canOpenFloatWindowOnMX(prop);
		}
        
        return false;
    }

    private class FloatSwitchBeforeCheckChangeListener implements cn.wsds.gamemaster.ui.view.Switch.OnBeforeCheckChangeListener,
            DialogInterface.OnClickListener {

        private Switch floatWndSwitch;

        public FloatSwitchBeforeCheckChangeListener(Switch floatWndSwitch) {
            this.floatWndSwitch = floatWndSwitch;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int id = ((CommonDialog) dialog).getId();
            switch (id) {
                case R.id.check_float_wnd:
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        Statistic.addEvent(getApplicationContext(),
                                Statistic.Event.FLOATING_WINDOW_SETTING_SWITCH_DISPLAY, "关");
                        ConfigManager.getInstance().setShowFloatWindowInGame(false);
                        visiableStyleVersion(false);
                        floatWndSwitch.setChecked(false);
                    }
                    break;
            }
        }

        @Override
        public boolean onBeforeCheckChange(Switch checkSwitch, boolean expectation) {
            if (expectation) {
                ConfigManager.getInstance().setShowFloatWindowInGame(true);
                visiableStyleVersion(true);
                Statistic.addEvent(getApplicationContext(),
                        Statistic.Event.FLOATING_WINDOW_SETTING_SWITCH_DISPLAY, "开");
                return true;
            }
            CommonDialog dialog = new CommonAlertDialog(ActivitySettingFloatWindow.this);
            dialog.setMessage("游戏悬浮窗是您游戏中的好帮手，建议您保持开启");
            dialog.setPositiveButton("开启(推荐)", this);
            dialog.setNegativeButton("关闭", this);
            dialog.setId(R.id.check_float_wnd);
            dialog.show();
            return false;
        }
    }

    /**
     * 是否显示尺寸设置
     *
     * @param visiable 是否显示
     */
    private void visiableStyleVersion(boolean visiable) {
        int visibility = visiable ? View.VISIBLE : View.GONE;
        measurePormpt.setVisibility(visibility);
        measureTypeGroup.setVisibility(visibility);
        floatwindowVisiableDelay.setVisibility(visibility);
    }

    private static final class FloatWndGroupClickListener implements View.OnClickListener {

        private final RadioGroup group;
        private int clickCount;

        private FloatWndGroupClickListener(RadioGroup group) {
            this.group = group;
            switch (ConfigManager.getInstance().getFloatWindowMode()) {
                case 1:
                    group.check(R.id.floatwindow_mode_1);
                    break;
                case 2:
                    group.check(R.id.floatwindow_mode_2);
                    break;
                case 3:
                    group.check(R.id.floatwindow_mode_3);
                    break;
                default:
                    group.check(R.id.floatwindow_mode_auto);
                    break;
            }
            group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int mode;
                    switch (checkedId) {
                        case R.id.floatwindow_mode_1:
                            mode = 1;
                            break;
                        case R.id.floatwindow_mode_2:
                            mode = 2;
                            break;
                        case R.id.floatwindow_mode_3:
                            mode = 3;
                            break;
                        default:
                            mode = 0;
                            break;
                    }
                    if (ConfigManager.getInstance().setFloatWindowMode(mode)) {
                        TaskManager.getInstance().resetStrategy(mode);
                    }
                }
            });
        }

        @Override
        public void onClick(View v) {
            clickCount++;
            if (clickCount >= 5) {
                group.setVisibility(View.VISIBLE);
            }
        }

    }
}
