package com.subao.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 用于对数据进行持久化操作的接口
 */
public interface Persistent {

    /**
     * 持久化存储是否已存在
     */
    boolean exists();

    /**
     * 打开一个输入流以供读取已持久化的数据
     */
    InputStream openInput() throws IOException;

    /**
     * 打开一个输出流以便将数据写入到持久化存储里
     */
    OutputStream openOutput() throws IOException;

    /**
     * 删除持久化存储
     */
    boolean delete();

    /**
     * 如果本对象是一个目录，创建一个该目录下的子文件对象
     *
     * @param name 文件对象名
     * @return {@link Persistent}
     */
    Persistent createChild(String name);

    /**
     * 读取内容并返回
     * @return
     */
    byte[] read() throws IOException;

}
