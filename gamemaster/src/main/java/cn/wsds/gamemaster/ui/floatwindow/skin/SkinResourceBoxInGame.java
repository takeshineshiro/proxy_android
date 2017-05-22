package cn.wsds.gamemaster.ui.floatwindow.skin;

/**
 * 大悬浮窗资源ID
 */
public abstract class SkinResourceBoxInGame{
	public abstract int getLayoutResource();
	
	public abstract int getBackgroundResource();
	
	public abstract int getButtonBackgroundResourceOnSelected();
	public abstract int getNetNameTextColorId();
	
	public abstract int getButtonEnableBackgroundRecource();
	public abstract int getButtonUnEnableBackgroundRecource();

	/** 按钮“折线图”的图片资源（高亮） */
	public abstract int getButtonCurveResourceChecked();
	/** 按钮“折线图”的图片资源（普通） */
	public abstract int getButtonCurveResourceUnChecked();
	

	/** 按钮“详情”的图片资源（高亮） */
	public abstract int getButtonDetailsResourceChecked();
	/** 按钮“详情”的图片资源（普通） */
	public abstract int getButtonDetailsResourceUnChecked();
	

	/** 按钮“设置”的图片资源（高亮） */
	public abstract int getButtonSettingResourceChecked();
	/** 按钮“设置”的图片资源（普通） */
	public abstract int getButtonSettingResourceUnChecked();
	
	
	/** 按钮“日志上传”的图片资源（高亮） */
	public abstract int getButtonLogUploadResourceChecked();
	/** 按钮“日志上传”的图片资源（普通） */
	public abstract int getButtonLogUploadResourceUnChecked();
	
	
	/** 按钮“截屏”的图片资源（高亮） */
	public abstract int getButtonScreenShotResourceChecked();
	/** 按钮“截屏”的图片资源（普通） */
	public abstract int getButtonScreenShotResourceUnChecked();
	

}
