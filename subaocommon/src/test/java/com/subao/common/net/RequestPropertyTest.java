package com.subao.common.net;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hujd on 16-7-21.
 */
public class RequestPropertyTest {

    @Test
    public void testToString() throws Exception {
        RequestProperty property = new RequestProperty("j", "3");
        assertNotNull(property.toString());
    }
}