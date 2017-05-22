package com.subao.common.parallel;

import com.subao.common.Disposable;

/**
 * NetworkWatcherImpl
 * <p>Created by YinHaiBo on 2017/3/7.</p>
 */
interface NetworkWatcherImpl extends Disposable {

    /**
     * 注册一个回调
     *
     * @param transportType {@link NetworkWatcher.TransportType}
     * @param callback      {@link NetworkWatcher.Callback} 观察者
     * @return 成功返回一个注册对象，将来可用{@link #unregister(Object)}来反注册
     * @throws NetworkWatcher.OperationException
     * @see #unregister(Object)
     */
    Object register(NetworkWatcher.TransportType transportType, NetworkWatcher.Callback callback) throws NetworkWatcher.OperationException;

    /**
     * 反注册一个回调
     *
     * @param registerObj {@link #register(NetworkWatcher.TransportType, NetworkWatcher.Callback)}函数返回的非负的注册ID
     */
    void unregister(Object registerObj);

}
