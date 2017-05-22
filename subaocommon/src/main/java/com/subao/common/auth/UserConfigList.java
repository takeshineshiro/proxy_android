package com.subao.common.auth;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * UserConfigList
 * <p>Created by YinHaiBo on 2016/12/12.</p>
 */
class UserConfigList {
    private final Map<String, UserConfig> map = new HashMap<String, UserConfig>(2);

    public String getConfigString(String userId) {
        UserConfig uc = get(userId);
        return (uc != null) ? uc.value : null;
    }

    public void put(String userId, UserConfig uc) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        synchronized (map) {
            if (uc == null) {
                map.remove(userId);
            } else {
                map.put(userId, uc);
            }
        }
    }

    public UserConfig get(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        UserConfig uc;
        synchronized (map) {
            uc = map.get(userId);
        }
        return uc;
    }
}
