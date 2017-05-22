package com.subao.common.collection;


import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FixedCapacityQueueTest {

    @Test(expected = RuntimeException.class)
    public void construct() {
        new FixedCapacityQueue<Integer>(0);
    }

    @Test
    public void getCapacity() {
        FixedCapacityQueue<Integer> target = new FixedCapacityQueue<Integer>(8);
        assertEquals(8, target.getCapacity());
    }

    @Test(expected = NullPointerException.class)
    public void offerNull() {
        FixedCapacityQueue<Integer> queue = new FixedCapacityQueue<Integer>(3);
        queue.offer(null);
    }

    @Test
    public void offer() {
        for (int capacity = 1; capacity <= 11; ++capacity) {
            FixedCapacityQueue<Integer> queue = new FixedCapacityQueue<Integer>(capacity);
            for (int i = 1; i <= 60; ++i) {
                queue.offer(i * 10);
                int expectedSize = (i > capacity) ? capacity : i;
                assertEquals(expectedSize, queue.size());
                assertFalse(queue.isEmpty());
                assertEquals(i >= capacity, queue.isFull());
                //
                Object[] objects = queue.toArray();
                assertEquals(queue.size(), objects.length);
                //
                int firstElement = 10;
                if (i > capacity) {
                    firstElement += (i - capacity) * 10;
                }
                for (int j = -1; j <= queue.size(); ++j) {
                    int value;
                    try {
                        value = queue.get(j);
                        if (j < 0 || j >= queue.size()) {
                            fail();
                        }
                    } catch (IndexOutOfBoundsException e) {
                        if (j >= 0 && j < queue.size()) {
                            fail();
                            throw e;
                        } else {
                            continue;
                        }
                    }
                    int expectedValue = firstElement + j * 10;
                    assertEquals(expectedValue, value);
                    assertEquals(expectedValue, objects[j]);
                }
            }
            //
            queue.clear();
            assertTrue(queue.isEmpty());
            assertFalse(queue.isFull());
            assertEquals(0, queue.size());
        }
    }

    @Test
    public void toArray() {
        for (int capacity = 1; capacity <= 11; ++capacity) {
            FixedCapacityQueue<Integer> queue = new FixedCapacityQueue<Integer>(capacity);
            for (int i = 1; i <= 60; ++i) {
                queue.offer(i * 10);
                for (int m = 0; m < capacity + 3; ++m) {
                    Integer[] integers = new Integer[m];
                    Integer[] array = queue.toArray(integers);
                    assertTrue(array.length >= queue.size());
                    for (int j = 0; j < array.length; ++j) {
                        if (j >= queue.size()) {
                            assertNull(array[j]);
                        } else {
                            assertEquals(array[j], queue.get(j));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void remove() {
        FixedCapacityQueue<Integer> queue = new FixedCapacityQueue<Integer>(7);
        try {
            queue.remove();
            fail();
        } catch (NoSuchElementException e) { }
        //
        for (int capacity = 1; capacity <= 11; ++capacity) {
            for (int i = 0; i <= 30; ++i) {
                queue = new FixedCapacityQueue<Integer>(capacity);
                int firstElement = 0;
                for (int j = 0; j < i; ++j) {
                    queue.offer(j);
                    if (j >= capacity) {
                        ++firstElement;
                    }
                }
                int count = Math.min(i, capacity);
                assertEquals(count, queue.size());
                if (count == 0) {
                    try {
                        queue.element();
                        fail();
                    } catch (NoSuchElementException e) {}
                } else {
                    assertEquals(firstElement, (int)queue.element());
                }
                while (!queue.isEmpty()) {
                    Integer value = queue.remove();
                    assertEquals(firstElement, (int)value);
                    ++firstElement;
                    --count;
                }
                assertEquals(0, count);
            }
        }
    }

    @Test
    public void removeCount() {
        for (int capacity = 1; capacity <= 11; ++capacity) {
            for (int i = 0; i <= 30; ++i) {
                for (int n = -1; n <= capacity + 1; ++n) {
                    FixedCapacityQueue<Integer> queue = new FixedCapacityQueue<Integer>(capacity);
                    for (int j = 0; j < i; ++j) {
                        queue.offer(j);
                    }
                    if (n < 0 || n > queue.size()) {
                        try {
                            queue.remove(n);
                            fail();
                        } catch (IllegalArgumentException e) {
                        }
                    } else {
                        int oldSize = queue.size();
                        queue.remove(n);
                        assertEquals(oldSize - n, queue.size());
                    }
                }
            }
        }
    }

}