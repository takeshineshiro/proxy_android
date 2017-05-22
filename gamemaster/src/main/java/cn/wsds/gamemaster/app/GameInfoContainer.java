package cn.wsds.gamemaster.app;

public interface GameInfoContainer {

	/**
	 * 判断给定UID的游戏是否为海外游戏
	 */
	public GameInfo getGameInfoByUID(int uid);
	
}
