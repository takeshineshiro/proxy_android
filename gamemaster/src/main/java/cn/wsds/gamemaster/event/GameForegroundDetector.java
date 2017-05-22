package cn.wsds.gamemaster.event;

import cn.wsds.gamemaster.app.GameInfo;

public interface GameForegroundDetector {
	
	/**
	 * 取当前顶层游戏
	 * 
	 * @return null表示当前顶层应用不是我们支持的游戏
	 */
	public GameInfo getCurrentForegroundGame();

}
