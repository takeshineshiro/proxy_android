package com.subao.common.msg;

import com.subao.common.RoboBase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MessagePersistentTest
 * <p>Created by YinHaiBo on 2017/1/18.</p>
 */
public class MessagePersistentTest extends RoboBase {

    private static byte[] DATA = new byte[]{5, 6, 7, 8, 9, 10, 11};

    private RandomAccessFile mockFile;
    private MockOperator mockOperator;
    private MessagePersistent messagePersistent;

    @Before
    public void setUp() {
        mockFile = mock(RandomAccessFile.class);
        mockOperator = new MockOperator(mockFile);
        messagePersistent = new MessagePersistent(mockOperator);
    }

    @Test
    public void testMessage() {
        String msgId = "msg_id";
        MessagePersistent.Message msg = new MessagePersistent.Message(msgId, DATA);
        assertEquals(msgId, msg.messageId);
        assertTrue(Arrays.equals(DATA, msg.messageBody));
    }

    @Test
    public void save() throws IOException {
        Answer<Void> answer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                byte[] buf = invocation.getArgument(0);
                assertTrue(Arrays.equals(DATA, buf));
                return null;
            }
        };
        doAnswer(answer).when(mockFile).write(any(byte[].class));
        String msgId = "1234";
        messagePersistent.save(msgId, DATA);
        assertEquals(MessagePersistent.FILENAME_PREFIX + msgId, mockOperator.filename);
    }

    @Test(expected = IOException.class)
    public void loadLargeFile() throws IOException {
        when(mockFile.length()).thenReturn(1024L * 1024 + 1);
        messagePersistent.load("1234");
    }

    @Test(expected = IOException.class)
    public void loadReadFail() throws IOException {
        when(mockFile.length()).thenReturn(1024L);
        when(mockFile.read(any(byte[].class))).thenReturn(128);
        messagePersistent.load("1234");
    }

    @Test
    public void loadList() throws Exception {
        assertNull(messagePersistent.loadList(-1));
        mockOperator.setEnumFilenamesResult(new String[]{
            "a", "b", "c",
            "link_a", "link_error", "link_b"
        });
        List<MessagePersistent.Message> messageList = messagePersistent.loadList(-1);
        assertEquals(2, messageList.size());
        messageList = messagePersistent.loadList(1);
        assertEquals(1, messageList.size());
        messageList = messagePersistent.loadList(2);
        assertEquals(2, messageList.size());
        messageList = messagePersistent.loadList(3);
        assertEquals(2, messageList.size());
    }

    @Test
    public void delete() throws Exception {
        String msgId = "1234";
        messagePersistent.delete(msgId);
        assertEquals(MessagePersistent.FILENAME_PREFIX + msgId, mockOperator.filename);
    }

    private static class MockOperator implements MessagePersistent.Operator {

        public final RandomAccessFile randomAccessFile;
        public String filename;
        public boolean openFileReadonly;

        private String[] enumFilenamesResult;


        MockOperator(RandomAccessFile randomAccessFile) {
            this.randomAccessFile = randomAccessFile;
        }

        @Override
        public RandomAccessFile openFile(String filename, boolean readonly) throws IOException {
            if ("link_error".equals(filename)) {
                throw new IOException();
            }
            this.filename = filename;
            this.openFileReadonly = readonly;
            return randomAccessFile;
        }

        @Override
        public String[] enumFilenames() {
            return enumFilenamesResult;
        }

        void setEnumFilenamesResult(String[] value) {
            this.enumFilenamesResult = value;
        }

        @Override
        public void delete(String filename) {
            this.filename = filename;
        }

    }


}