package com.subao.common.collection;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SmallSet<E> extends AbstractSet<E> {
	
	private final List<E> list;
	
	public SmallSet() {
		list = new ArrayList<E>();
	}
	
	public SmallSet(int capacity) {
		list = new ArrayList<E>(capacity);
	}

	@Override
	public boolean add(E object) {
		if (list.contains(object)) {
			return false;
		} else {
			list.add(object);
			return true;
		}
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		boolean r = false;
		for (E e : collection) {
			if (add(e)) {
				r = true;
			}
		}
		return r;
	}

	@Override
	public void clear() {
		list.clear();
	}

	@Override
	public boolean contains(Object object) {
		return list.contains(object);
	}
	
	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return list.iterator();
	}

	@Override
	public boolean remove(Object object) {
		return list.remove(object);
	}


	@Override
	public boolean retainAll(Collection<?> collection) {
		return list.retainAll(collection);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return list.toArray(array);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return list.containsAll(collection);
	}

}