package com.subao.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProxyEngineCommunicatorTest {

    @Test
    public void test() {
        assertNull(ProxyEngineCommunicator.Instance.get());
        ProxyEngineCommunicator proxyEngineCommunicator = new Impl();
        ProxyEngineCommunicator.Instance.set(proxyEngineCommunicator);
        assertEquals(proxyEngineCommunicator, ProxyEngineCommunicator.Instance.get());
    }

    @Test
    public void beQAHappy() {
        new ProxyEngineCommunicator.Instance();
    }

    private static class Impl implements ProxyEngineCommunicator {

        @Override
        public void setInt(int cid, String key, int value) {

        }

        @Override
        public void setString(int cid, String key, String value) {

        }

        @Override
        public void defineConst(String key, String value) {

        }
    }
}