package com.subao.common;

import android.util.Pair;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * MockProxyEngineCommunicator
 * <p>Created by YinHaiBo on 2017/2/3.</p>
 */
public class MockProxyEngineCommunicator implements ProxyEngineCommunicator, Iterable<Pair<String, Object>> {

    private Deque<Pair<String, Object>> keyValues = new LinkedList<Pair<String, Object>>();

    final Map<String, String> definedConst = new HashMap<String, String>(16);

    public void reset() {
        keyValues.clear();
    }

    @Override
    public void setInt(int cid, String key, int value) {
        keyValues.offer(new Pair<String, Object>(key, value));
    }

    @Override
    public void setString(int cid, String key, String value) {
        keyValues.offer(new Pair<String, Object>(key, value));
    }

    @Override
    public void defineConst(String key, String value) {
        definedConst.put(key, value);
    }

    public int countOfSet() {
        return keyValues.size();
    }

    public String getLastKey() {
        return keyValues.getLast().first;
    }

    public int getLastIntValue() {
        return (Integer) keyValues.getLast().second;
    }

    public String getLastStringValue() {
        return (String) keyValues.getLast().second;
    }

    @Override
    public Iterator<Pair<String, Object>> iterator() {
        return keyValues.iterator();
    }
}
