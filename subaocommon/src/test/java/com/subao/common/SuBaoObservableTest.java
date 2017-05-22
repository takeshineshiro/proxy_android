package com.subao.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class SuBaoObservableTest {

	private static class Observer {

	}

	private static class ObservableImpl extends SuBaoObservable<Observer> {

	}

	@Test
	public void testRegisterObserver() {
		ObservableImpl impl = new ObservableImpl();
		Observer o = new Observer();
		assertFalse(impl.registerObserver(null));
		assertTrue(impl.registerObserver(o));
		assertFalse(impl.registerObserver(o));
	}

	@Test
	public void testUnregisterObserver() {
		ObservableImpl impl = new ObservableImpl();
		Observer o = new Observer();
		assertTrue(impl.registerObserver(o));
		assertTrue(impl.unregisterObserver(o));
		assertFalse(impl.unregisterObserver(o));
		assertFalse(impl.unregisterObserver(null));
	}

	@Test
	public void testUnregisterAll() {
		ObservableImpl impl = new ObservableImpl();
		Observer o = new Observer();
		assertTrue(impl.registerObserver(o));
		assertFalse(impl.isEmpty());
		assertEquals(1, impl.getObserverCount());
		impl.unregisterAllObservers();
		assertTrue(impl.isEmpty());
		assertFalse(impl.unregisterObserver(o));
	}

	@Test
	public void testCloneAllObservers() {
		ObservableImpl impl = new ObservableImpl();
		Observer o1 = new Observer();
		Observer o2 = new Observer();
		assertTrue(impl.registerObserver(o1));
		assertTrue(impl.registerObserver(o2));
		assertEquals(2, impl.getObserverCount());
		assertFalse(impl.isEmpty());
		//
		List<Observer> list = impl.cloneAllObservers();
		assertEquals(2, list.size());
		assertTrue(list.contains(o1));
		assertTrue(list.contains(o2));
		//
		impl.unregisterAllObservers();
		list = impl.cloneAllObservers();
		assertNull(list);
	}
}
