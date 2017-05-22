package com.subao.common.model;

import com.subao.common.data.AccelGame;

import java.util.List;

/**
 * 设置
 * Created by hujd on 17-3-30.
 */
public class AccelGameListManager {
	private static volatile AccelGameListManager instance = null;
	private List<AccelGame> accelGameList;

	public static AccelGameListManager getInstance() {
		if (instance == null) {
			synchronized (AccelGameListManager.class) {
				if (instance == null) {
					instance = new AccelGameListManager();
				}
			}
		}

		return instance;
	}

	public void setAccelGameList(List<AccelGame> list) {
		this.accelGameList = list;
	}

	public List<AccelGame> getAccelGameList() {
		return accelGameList;
	}
}
