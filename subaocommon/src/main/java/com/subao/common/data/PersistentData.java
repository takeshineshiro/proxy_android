package com.subao.common.data;

import com.subao.common.Misc;
import com.subao.common.io.Persistent;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 可持久化的数据
 * <p>Created by YinHaiBo on 2017/2/27.</p>
 */
public class PersistentData {

    private final Persistent dir;

    /**
     * 以给定的目录对象进行构造
     *
     * @param dir 目录
     */
    public PersistentData(Persistent dir) {
        this.dir = dir;
    }

    /**
     * 将给定的数据，以指定的名移保存
     *
     * @param name  数据名称，保证可用作文件名
     * @param value 数据内容。如果数据为null则删除文件
     * @throws IOException
     */
    public void save(String name, byte[] value) throws IOException {
        Persistent file = dir.createChild(name);
        if (value == null) {
            file.delete();
            return;
        }
        OutputStream outputStream = file.openOutput();
        try {
            outputStream.write(value);
        } finally {
            Misc.close(outputStream);
        }
    }

    /**
     * 加载指定名称的数据
     *
     * @param name 数据名称
     * @return 数据
     * @see #save(String, byte[])
     */
    public byte[] load(String name) throws IOException {
        return dir.createChild(name).read();
    }

}
