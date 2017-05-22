package com.subao.common.msg;

import com.subao.common.Misc;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责将Link消息持久化，以及从持久化介质读取出来的类
 * <p>Created by YinHaiBo on 2017/1/17.</p>
 */

public class MessagePersistent {

    final static String FILENAME_PREFIX = "link_";

    private final Operator operator;

    public MessagePersistent(Operator operator) {
        this.operator = operator;
    }

    private static String buildFilenameFromMessageId(String msgId) {
        return FILENAME_PREFIX + msgId;
    }

//    private static String extractMessageIdFromFilename(String filename) {
//        if (filename == null) {
//            return null;
//        }
//        if (!filename.startsWith(FILENAME_PREFIX)) {
//            return null;
//        }
//        return filename.substring(FILENAME_PREFIX.length());
//    }

    /**
     * 存储一个消息
     *
     * @param messageId  消息ID
     * @param msgContent 消息内容
     * @throws IOException
     */
    public void save(String messageId, byte[] msgContent) throws IOException {
        RandomAccessFile randomAccessFile = operator.openFile(buildFilenameFromMessageId(messageId), false);
        if (randomAccessFile != null) {
            try {
                randomAccessFile.write(msgContent);
            } finally {
                Misc.close(randomAccessFile);
            }
        } else {
            throw new IOException("Open file error");
        }
    }

    /**
     * 加载所有已存储的消息到内存列表中（但可指定加载上限）
     * @param maxCount 加载上限
     * @return Message消息的列表，或null
     */
    public List<Message> loadList(int maxCount) {
        String[] filenames = operator.enumFilenames();
        if (filenames != null && filenames.length > 0) {
            List<Message> messages = new ArrayList<Message>(filenames.length);
            for (String filename : filenames) {
                if (filename.length() > FILENAME_PREFIX.length() && filename.startsWith(FILENAME_PREFIX)) {
                    try {
                        byte[] data = load(filename);
                        String messageId = filename.substring(FILENAME_PREFIX.length());
                        messages.add(new Message(messageId, data));
                        if (maxCount > 0 && messages.size() == maxCount) {
                            break;
                        }
                    } catch (IOException e) {
                        operator.delete(filename);
                    }
                }
            }
            return messages;
        }
        return null;
    }

    byte[] load(String filename) throws IOException {
        RandomAccessFile randomAccessFile = operator.openFile(filename, true);
        try {
            long len = randomAccessFile.length();
            if (len > 1024 * 1024) {
                throw new IOException("File too large");
            }
            byte[] data = new byte[(int) len];
            int read = randomAccessFile.read(data);
            if (read != (int) len) {
                throw new IOException("Read file error");
            }
            return data;
        } finally {
            Misc.close(randomAccessFile);
        }
    }

    /**
     * 删除指定Message ID的消息存储
     *
     * @param messageId 消息ID
     */
    public void delete(String messageId) {
        operator.delete(buildFilenameFromMessageId(messageId));
    }

    /**
     * 具体文件操作者
     */
    public interface Operator {
        RandomAccessFile openFile(String filename, boolean readonly) throws IOException;

        String[] enumFilenames();

        void delete(String filename);
    }

    /**
     * 消息
     */
    public static class Message {
        public final String messageId;
        public final byte[] messageBody;

        public Message(String messageId, byte[] messageBody) {
            this.messageId = messageId;
            this.messageBody = messageBody;
        }
    }

}
