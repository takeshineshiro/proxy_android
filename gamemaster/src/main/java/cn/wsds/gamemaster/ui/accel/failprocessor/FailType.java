package cn.wsds.gamemaster.ui.accel.failprocessor;

/**
 * 加速开启错误类型
 */
public enum FailType {
	/** wifi 热点*/
	WifiAP,
	/** 网络访问权限被禁止 */
	NetworkCheck,
	/** 当前接入点为wap */
	WAP,

	/** 授权发生错误 */
	ImpowerError,
	/** 授权被拒绝 */
	ImpowerReject,
	/** 开启错误 */
	StartError,
	/** 缺失这个模块 */
	DefectModel,
	/** 取消 */
	ImpowerCancel
} 