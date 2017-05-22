package com.subao.common;

import static org.junit.Assert.assertFalse;

import java.util.NoSuchElementException;

import org.junit.Test;

public class EmptyIteratorTest {
	
	@Test
	public void testHasNext() {
		EmptyIterator<Integer> it = new EmptyIterator<Integer>();
		assertFalse(it.hasNext());
	}
	
	@Test(expected=NoSuchElementException.class)
	public void testNext() {
		EmptyIterator<Integer> it = new EmptyIterator<Integer>();
		it.next();
	}

	@Test(expected=IllegalStateException.class)
	public void testRemove() {
		EmptyIterator<Integer> it = new EmptyIterator<Integer>();
		it.remove();
	}

}
