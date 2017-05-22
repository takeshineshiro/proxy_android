package com.subao.common.data;

/**
 * 一些全局的常量定义
 */
public class Defines {

    /**
     * 各网络请求参数里所需的ClientType，如果是SDK，填写游戏的GUID，如果是APP，则使用本常量
     *
     * @see PortalDataDownloader
     * @see com.subao.common.data.PortalDataDownloader.Arguments
     * @see com.subao.common.auth.AuthService
     */
    public static final String REQUEST_CLIENT_TYPE_FOR_APP = "android";
    /**
     * 全局变量，由上层逻辑模块在初始化的时候设置。
     */
    public static ModuleType moduleType;

    private Defines() {
    }

    /**
     * 模块类型
     */
    public enum ModuleType {
        /**
         * SDK
         */
        SDK("SDK"),

        /**
         * APP的UI进程
         */
        UI("UI"),

        /**
         * APP的Service进程
         */
        SERVICE("SERVICE"),

        /**
         * ROM
         */
        ROM("ROM");

        public final String name;

        ModuleType(String name) {
            this.name = name;
        }
    }

    public static class VPNJniStrKey {

        /**
         * 通知JNI：当前的地区和ISP信息
         */
        public static final String KEY_ISP = "key_isp";
        /**
         * 设置前台游戏uid
         */
        public static final String KEY_FRONT_GAME_UID = "key_front_game_uid";
        /**
         * 设置免流用户类型（012为免流用户，其它值为“非免流用户”）
         */
        public static final String KEY_FREE_FLOW_TYPE = "key_free_flow_type";
        /**
         * 设置区服Id
         */
        public static final String KEY_GAME_SERVER_ID = "key_game_server_id";
        /**
         * 设置用户网络状态
         */
        public static final String KEY_NET_STATE = "key_net_state";
        /**
         * 本机型是否支持WiFi加速
         */
        public static final String KEY_ENABLE_QPP = "key_enable_qpp";
        /**
         * 当前4G环境是否支持Qos
         */
        public static final String KEY_ENABLE_QOS = "key_enable_qos";
        /**
         * 设置SubaoId
         */
        public static final String KEY_SUBAO_ID = "key_subao_id";

        /**
         * VersionInfo.number
         */
        public static final String KEY_VERSION = "key_version";
        /**
         * VersionInfo.channel
         */
        public static final String KEY_CHANNEL = "key_channel";
        /**
         * VersionInfo.osVersion
         */
        public static final String KEY_OS_VERSION = "key_os_version";
        /**
         * VersionInfo.androidVersion
         */
        public static final String KEY_ANDROID_VERSION = "key_android_version";
        /**
         * 手机型号，String
         */
        public static final String KEY_PHONE_MODEL = "key_phone_model";
        /**
         * 手机ROM，String
         */
        public static final String KEY_ROM = "key_rom";
        /**
         * CPU速度，Int
         */
        public static final String KEY_CPU_SPEED = "key_cpu_speed";
        /**
         * CPU内核数，Int
         */
        public static final String KEY_CPU_CORE = "key_cpu_core";
        /**
         * 内存大小，Int
         */
        public static final String KEY_MEMORY = "key_memory";
        /**
         * 脚本注入（注意，用此方式只能注入Lua源代码）
         */
        public static final String KEY_INJECT = "key_inject";
        /**
         * 设置SDK的GUID
         */
        public static final String KEY_SDK_GUID = "key_sdk_guid";
        /**
         * 设置SDK要HOOK的SO
         */
        public static final String KEY_HOOK_MODULE = "key_hook_module";
        /**
         * Java通知JNI：汇聚节点列表
         */
        public static final String KEY_CONVERGENCE_NODE = "key_convergence_node";
        /**
         * Java通知JNI：Beacon计数器请求结果
         *
         * @see com.subao.vpn.JniCallback#requestBeaconCounter(int, String)
         * @see com.subao.common.jni.JniWrapper#setInt(int, String, int)
         * @see com.subao.vpn.VPNJni#setInt(int, byte[], int)
         */
        public static final String KEY_BEACON_COUNTER_RESULT = "key_beacon_counter_result";

        /**
         * Java通知JNI：支付方式白名单
         *
         * @see com.subao.common.accel.EngineWrapper#setPayTypeWhiteList(String)
         */
        public static final String KEY_PAY_TYPE_WHITE_LIST = "key_pay_type_white_list";

        /**
         * Java通知JNI：系统通知蜂窝网络Available / Lost
         */
        public static final String KEY_CELLULAR_STATE_CHANGE = "key_cellular_state_change";

        /**
         * Java通知JNI：当前移动网络开关状态
         */
        public static final String KEY_MOBILE_SWITCH_STATE = "key_mobile_switch_state";

        /**
         * Java通知JNI：允许/禁止WiFi加速功能
         */
        public static final String KEY_USER_WIFI_ACCEL = "key_user_wifi_accel";

        /**
         * Java通知JNI：添加支持的游戏
         */
        public static final String KEY_ADD_GAME = "key_add_game";

        private VPNJniStrKey() {
        }
    }

}
