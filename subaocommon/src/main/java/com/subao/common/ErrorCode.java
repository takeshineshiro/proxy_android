package com.subao.common;

/**
 * 公用的ERROR Code
 */
public class ErrorCode {

    private ErrorCode() {}

    public static final int OK = 0;

    /**
     * 尚未初始化引擎
     */
    public static final int NOT_INIT = 1000;

    /**
     * WiFi加速相关操作：Android版本号过低（5.0及以上才支持WiFi加速）
     */
    public static final int WIFI_ACCEL_ANDROID_VERSION_TOO_LOW = 2000;

    /**
     * WiFi加速相关操作：APP（游戏）未申请CHANGE_NETWORK_STATE权限
     */
    public static final int WIFI_ACCEL_NO_PERMISSION = 2001;

    /**
     * WiFi加速相关操作：注册回调失败
     */
    public static final int WIFI_ACCEL_REGISTER_FAIL = 2002;

    /**
     * WiFi加速相关操作：没有可用的蜂窝网络，且检测到移动网络开关状态：OFF
     */
    public static final int WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_OFF = 2003;

    /**
     * WiFi加速相关操作：当前不是4G/3G
     */
    public static final int WIFI_ACCEL_NOT_4G = 2004;

    /**
     * WiFi加速相关操作：创建FD失败
     */
    public static final int WIFI_ACCEL_CREATE_FD_FAIL = 2005;

    /**
     * WiFi加速相关操作：本机型不在允许列表里
     */
    public static final int WIFI_ACCEL_MODEL_NOT_ALLOW = 2006;

    /**
     * WiFi加速相关操作：没有可用的蜂窝网络，但检测到移动网络开关状态为：ON
     */
    public static final int WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON = 2007;

    /**
     * WiFi加速相关操作：没有可用的蜂窝网络，且检测到移动网络开关状态为：UNKNOWN
     */
    public static final int WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN = 2008;

    /**
     * WiFi加速相关操作：在Android 5.1环境下调用bindSocket()失败
     */
    public static final int WIFI_ACCEL_BIND_FD_FAIL_VER22 = 2009;

    /**
     * WiFi加速相关操作：在Android 5.0环境下，利用反射，取NetID失败
     */
    public static final int WIFI_ACCEL_FAIL_GET_NET_ID = 2010;

    /**
     * 在Android 5.0环境下，从{@link java.net.DatagramSocket} 里取File Descriptor失败
     */
    public static final int WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR = 2011;

    /**
     * 在Android 5.0环境下，从{@link android.os.ParcelFileDescriptor}中取FD失败
     */
    public static final int WIFI_ACCEL_FAIL_GET_FD = 2012;

    /**
     * 在Android 5.1环境下，利用反射绑定Socket到指定网络失败
     */
    public static final int WIFI_ACCEL_BIND_FD_FAIL_VER21 = 2013;

    /**
     * 取到NetworkInfo并判断类型时，类型不为MOBILE
     */
    public static final int WIFI_ACCEL_NOT_MOBILE = 2014;

    /**
     * 已经绑定成功后，取{@link android.os.ParcelFileDescriptor}失败
     */
    public static final int WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND = 2015;

    /**
     * 无可用蜂窝网络，但网络开关开着，错误码从2007优化一下：
     * 即优化为：此基值加上信号强度百分比（2100~2200）之间
     */
    public static final int WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH = 2100;


    //========================[ Qos 相关 ]===============================

    /**
     * Qos相关操作：试图发起请求的时候网络断开状态
     */
    public static final int QOS_NETWORK_DISCONNECT = 3000;

    /**
     * 试图发起请求的时候，非4G状态
     */
    public static final int QOS_NOT_4G = 3001;

    /**
     * 请求时长无效
     */
    public static final int QOS_INVALID_TIME_LENGTH = 3002;

    /**
     * 当前地区不支持
     */
    public static final int QOS_REGION_NOT_SUPPORT = 3003;

    /**
     * Qos管理服务器的请求相关的错误基值，包括通讯失败等
     */
    public static final int QOS_MANAGER_IO_BASE = 4000;

    /**
     * 与QosManager通讯的时候，出现RuntimeException（比如权限被禁用等）
     */
    public static final int QOS_MANAGER_IO_RUNTIME_EXCEPTION = QOS_MANAGER_IO_BASE + 1;

    /**
     * Qos管理服务器返回内容相关的错误码基值
     */
    public static final int QOS_MANAGER_RESPONSE_BASE = 5000;

    /**
     * Qos管理服务器返回的内容为空（期望不为空）
     */
    public static final int QOS_MANAGER_RESPONSE_NULL = QOS_MANAGER_RESPONSE_BASE + 1;

    /**
     * Qos管理服务器Response格式解析错
     */
    public static final int QOS_MANAGER_RESPONSE_PARSE_ERROR = QOS_MANAGER_RESPONSE_BASE + 2;

    /**
     * QosManager返回的Response缺少必要的信息
     */
    public static final int QOS_MANAGER_RESPONSE_DATA_ERROR = QOS_MANAGER_RESPONSE_BASE + 3;

    /**
     * QosManager返回的Response里的resultCode错误代码基值
     */
    public static final int QOS_MANAGER_RESPONSE_RESULT_CODE_BASE = 6000;

    /**
     * QOS与第三方厂商通讯错误的基值
     */
    public static final int QOS_THIRD_PROVIDER_BASE = 7000;

    /**
     * 与第三方厂商通讯错
     */
    public static final int QOS_THIRD_PROVIDER_IO_ERROR = QOS_THIRD_PROVIDER_BASE + 1;

    /**
     * 第三方厂商返回的Response里有错误代码
     */
    public static final int QOS_THIRD_PROVIDER_RESPONSE_ERROR_CODE = QOS_THIRD_PROVIDER_BASE + 2;

    /**
     * 第三方厂商返回的Response无法解析或缺少必要的关键字段
     */
    public static final int QOS_THIRD_PROVIDER_RESPONSE_PARSE_ERROR = QOS_THIRD_PROVIDER_BASE + 3;

    /**
     * 与第三方厂商通讯时，遇到Runtime Exception
     */
    public static final int QOS_THIRD_PROVIDER_IO_RUNTIME_EXCEPTION = QOS_THIRD_PROVIDER_BASE + 4;

    // =============================[ VPN 相关 ] ====================================

    public static final int VPN_ESTABLISH_EXCEPTION = 8000;

    public static final int VPN_SERVICE_NOT_EXISTS = 8001;

    public static final int VPN_PROTECT_SOCKET_FAIL = 8002;

    public static final int VPN_JNI_START_FAIL = 8003;

    public static final int VPN_ESTABLISH_ERROR = 8004;

    public static boolean canRetryWhenWifiAccelError(int errorCode) {
        switch (errorCode) {
        case WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_OFF:
        case WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_ON:
        case WIFI_ACCEL_NO_AVAILABLE_CELLULAR_NETWORK_SWITCH_UNKNOWN:
        case WIFI_ACCEL_NOT_4G:
        case WIFI_ACCEL_CREATE_FD_FAIL:
        case WIFI_ACCEL_BIND_FD_FAIL_VER22:
        case WIFI_ACCEL_FAIL_GET_NET_ID:
        case WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR:
        case WIFI_ACCEL_FAIL_GET_FD:
        case WIFI_ACCEL_BIND_FD_FAIL_VER21:
        case WIFI_ACCEL_NOT_MOBILE:
        case WIFI_ACCEL_FAIL_GET_FILE_DESCRIPTOR_AFTER_BIND:
            return true;
        default:
            if (errorCode >= WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH
                && errorCode <= WIFI_ACCEL_NO_AVAILABLE_CELLULAR_SIGNAL_STRENGTH + 100)
            {
                return true;
            }
            return false;
        }
    }


}
