package cn.wsds.gamemaster.ui.floatwindow;

import android.content.Context;
import android.graphics.Point;
import android.view.View;
import cn.wsds.gamemaster.R;
import cn.wsds.gamemaster.ui.floatwindow.ViewFloatInReconnect.OnFinishListener;

import com.subao.utils.MetricsUtils;

/**
 * 断线重连悬浮窗
 * @author Administrator
 *
 */
public class FloatWindowInReconnect extends FloatWindow{
	
	private static FloatWindowInReconnect instance;
	private ViewFloatInReconnect viewFloat;
	private int positionX;
	private int positionY;
	
	private FloatWindowInReconnect(Context context, String beginContent) {
		super(context);	
		viewFloat = new ViewFloatInReconnect(context, beginContent);
		viewFloat.setOnFinishListener(new OnFinishListener() {
			
			@Override
			public void onFinish(boolean successed) {
				destroyInstance();
			}
		});
		initPosition(context);
		addView(Type.DRAGGED, viewFloat, positionX, positionY);
	}
	
	
	/**
	 * 创建断线重连悬浮窗实例
	 * 
	 * @param context
	 * 				Context
	 * @param beginContent 
	 * 				开始显示的内容
	 * @return
	 */
	public static FloatWindowInReconnect createInstance(Context context, String beginContent){
		if(instance == null){
			instance = new FloatWindowInReconnect(context, beginContent);
			instance.setVisibility(View.VISIBLE);
			FloatWindowInGame.setInstanceVisibility(View.GONE);
		}
		return instance;
	}
	
	@Override
	protected void destroy() {
		super.destroy();
		FloatWindowInGame.setInstanceVisibility(View.VISIBLE);
		instance = null;
	}
	
	public static boolean exists() {
		return instance != null;
	}
	
	/**
	 * 获取InGame悬浮窗的高度
	 */
	private void initPosition(Context context){
		Point screenSize = MetricsUtils.getDevicesSizeByPixels(context);
		positionX = (screenSize.x  - context.getResources().getDimensionPixelSize(R.dimen.space_size_310)) / 2;
		positionY = screenSize.y / 3;
	}

	@Override
	protected void onViewAdded(View view) {
		
	}

	@Override
	protected boolean canDrag() {
		return false;
	}
	
	/**
	 * 销毁悬浮窗
	 */
	public static void destroyInstance() {
		if (instance != null) {
			FloatWindowInReconnect inst = instance;
			instance = null;
			inst.destroy();
		}
	}
	
	@Override
	protected void onClick(int x, int y) {
		super.onClick(x, y);
		if(instance != null && viewFloat.canCloseWindow(x, y)){
//			StatisticDefault.addEvent(instance.getContext(), 
//					StatisticDefault.Event.REPAIR_CONNECTION_EFFECT_CLOSED_BY_USER);
			instance.destroy();
		}
	}
	
	/**
	 * 改变当前数据
	 * @param count
	 * @param successed
	 */
	public static void changeCurrentData(int count, boolean successed){
		if(instance != null){
			instance.viewFloat.changeCurrentData(count, successed);
		}
	}
}
