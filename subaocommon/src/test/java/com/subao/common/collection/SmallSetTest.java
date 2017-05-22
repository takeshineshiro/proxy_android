package com.subao.common.collection;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SmallSetTest
 * <p>Created by YinHaiBo on 2016/11/4.</p>
 */
public class SmallSetTest {

    private Set<Integer> target;

    @Before
    public void setUp() {
        target = new SmallSet<Integer>(10);
        target.add(1);
        target.add(3);
        target.add(5);
    }

    @Test
    public void constructor() {
        Set<Integer> set = new SmallSet<Integer>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void add() throws Exception {
        assertTrue(target.add(2));
        assertFalse(target.add(2));
        assertTrue(target.add(4));
        assertFalse(target.add(1));
    }

    @Test
    public void addAll() throws Exception {
        List<Integer> list = new ArrayList<Integer>(4);
        list.add(2);
        list.add(4);
        list.add(5);
        list.add(6);
        target.addAll(list);
        assertEquals(6, target.size());
        for (int i = 1; i <= 6; ++i) {
            assertTrue(target.contains(i));
        }
    }

    @Test
    public void clear() throws Exception {
        assertFalse(target.isEmpty());
        assertEquals(3, target.size());
        target.clear();
        assertTrue(target.isEmpty());
        assertEquals(0, target.size());
    }

    @Test
    public void iterator() throws Exception {
        int expected = 1;
        for (Integer i : target) {
            assertEquals(expected, (int)i);
            expected += 2;
        }
    }

    @Test
    public void remove() throws Exception {
        assertFalse(target.remove(2));
        assertEquals(3, target.size());
        int size = 3;
        for (int i = 1; i <= 5; i += 2) {
            assertTrue(target.remove(i));
            --size;
            assertEquals(size, target.size());
        }
        assertEquals(0, target.size());
        assertEquals(0, size);
        assertTrue(target.isEmpty());
    }

    @Test
    public void removeAll() throws Exception {
        List<Integer> list = new ArrayList<Integer>(2);
        list.add(1);
        list.add(5);
        assertTrue(target.removeAll(list));
        assertEquals(1, target.size());
        assertTrue(target.contains(3));
        assertFalse(target.removeAll(list));
        assertEquals(1, target.size());
        assertTrue(target.contains(3));
    }

    @Test
    public void retainAll() throws Exception {
        List<Integer> list = new ArrayList<Integer>(2);
        list.add(1);
        list.add(5);
        assertTrue(target.retainAll(list));
        assertEquals(2, list.size());
        assertTrue(target.contains(1));
        assertTrue(target.contains(5));
    }

    @Test
    public void toArray() throws Exception {
        Object[] array = target.toArray();
        assertEquals(3, array.length);
        int expected = 1;
        for (Object obj : array) {
            assertEquals((int)(Integer)obj, expected);
            expected += 2;
        }
    }

    @Test
    public void toArray1() throws Exception {
        Integer[] array = target.toArray(new Integer[0]);
        assertEquals(3, array.length);
        int expected = 1;
        for (Integer obj : array) {
            assertEquals(obj.intValue(), expected);
            expected += 2;
        }
    }

    @Test
    public void containsAll() throws Exception {
        List<Integer> list = new ArrayList<Integer>(4);
        assertTrue(target.containsAll(list));
        for (int i = 1; i <= 5; i += 2) {
            list.add(i);
            assertTrue(target.containsAll(list));
        }
        list.add(1);
        assertTrue(target.containsAll(list));
        list.add(2);
        assertFalse(target.containsAll(list));
    }

}