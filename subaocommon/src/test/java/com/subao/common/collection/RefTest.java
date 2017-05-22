package com.subao.common.collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RefTest {

    @Test
    public void test() throws Exception {
        Ref<String> ref = new Ref<String>();
        assertNull(ref.get());
        String testString_1 = "hello";
        ref.set(testString_1);
        assertEquals(testString_1, ref.get());
        String testString_2 = "world";
        ref = new Ref<String>(testString_2);
        assertEquals(testString_2, ref.get());
    }

}