package com.subao.common.collection;

import java.io.IOException;
import java.io.InputStream;

/**
 * Buffer
 * <p>Created by YinHaiBo on 2016/11/26.</p>
 */

public class Buffer {

    private byte[] array;
    private int position;

    public Buffer(int capacity) {
        array = new byte[capacity];
    }

    /**
     * 从给定的{@link InputStream}里读入指定字节数的数据到本Buffer
     *
     * @param input {@link InputStream}
     * @param bytes 要读取的字节数
     * @return 实际读取的字节数，即{@link InputStream#read(byte[], int, int)}的返回值
     * @see InputStream#read(byte[], int, int)
     */
    public int readFromInputStream(InputStream input, int bytes) throws IOException {
        int bufAvali = array.length - position; // 缓冲区还剩多少可用？
        int needExpand = bytes - bufAvali;  // 是否需要扩大缓冲区？
        if (needExpand > 0) {
            needExpand = Math.max(array.length / 2, needExpand);
            byte[] newArray = new byte[array.length + needExpand];
            System.arraycopy(array, 0, newArray, 0, position);
            this.array = newArray;
        }
        int result = input.read(this.array, position, bytes);
        if (result > 0) {
            position += result;
        }
        return result;
    }

    /**
     * 将当前缓冲区里的有效内容克隆一份
     *
     * @return 当前缓冲区里有效内容的副本
     */
    public byte[] cloneArray() {
        byte[] result = new byte[position];
        if (position > 0) {
            System.arraycopy(this.array, 0, result, 0, position);
        }
        return result;
    }
}
