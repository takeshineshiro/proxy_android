package com.subao.common;

import java.util.ArrayList;
import java.util.List;

public abstract class SuBaoObservable<T> {

    private final List<T> observers = new ArrayList<T>();

    public boolean registerObserver(T observer) {
        if (observer != null) {
            synchronized (observers) {
                if (!observers.contains(observer)) {
                    return observers.add(observer);
                }
            }
        }
        return false;
    }

    public boolean unregisterObserver(T observer) {
        if (observer != null) {
            boolean result;
            synchronized (observers) {
                result = observers.remove(observer);
            }
            return result;
        }
        return false;
    }

    public void unregisterAllObservers() {
        synchronized (observers) {
            observers.clear();
        }
    }

    public boolean isEmpty() {
        return observers.isEmpty();
    }

    public int getObserverCount() {
        return observers.size();
    }

    /**
     * 建立并返回一个Observer列表的副本
     *
     * @return Observer列表的副本，或null（当列表为空时）
     */
    protected final List<T> cloneAllObservers() {
        synchronized (observers) {
            if (!observers.isEmpty()) {
                return new ArrayList<T>(observers);
            }
        }
        return null;
    }
}
