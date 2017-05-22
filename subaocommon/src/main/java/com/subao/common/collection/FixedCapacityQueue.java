package com.subao.common.collection;

import java.lang.reflect.Array;
import java.util.NoSuchElementException;

/**
 * 固定容量的队列
 * <p>
 * 一个队列容器，容量固定。当容量已满时，新增加一个元素，将导致最早加入（队首）的元素弹出队列。<br />
 * <b>注意，本容器的实现使用固定长度的数组，所以不太适用于容量过大的场合，使用时请考虑内存占用问题。</b>
 * </p>
 */
public class FixedCapacityQueue<E> {

	private final Object[] array;
	private final int capacity;
	private int head, tail;
	private int count;

	public FixedCapacityQueue(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity <= 0: " + capacity);
		}
		this.capacity = capacity;
		this.array = new Object[capacity];
	}

	/**
	 * 返回本队列的容量
	 */
	public int getCapacity() {
		return this.capacity;
	}

	/**
	 * 移除队列里所有元素
	 */
	public void clear() {
		count = head = tail = 0;
	}

	/**
	 * 判断队列是否为空
	 * 
	 * @return true表示队列为空，false表示不为空
	 */
	public boolean isEmpty() {
		return count == 0;
	}

	/**
	 * 判断队列是否已满
	 */
	public boolean isFull() {
		return count >= capacity;
	}

	/**
	 * 返回队列里元素的个数
	 */
	public int size() {
		return count;
	}

	/**
	 * 返回指定下标（从Header算起）的元素
	 */
	@SuppressWarnings("unchecked")
	public E get(int index) {
		if (index < 0 || index >= count) {
			throw new IndexOutOfBoundsException();
		}
		int i = head + index;
		if (i >= capacity) {
			i -= capacity;
		}
		return (E) array[i];
	}

	private void copyTo(Object[] result) {
		if (count > 0) {
			if (head < tail) {
				System.arraycopy(array, head, result, 0, count);
			} else {
				int n = capacity - head;
				System.arraycopy(array, head, result, 0, n);
				System.arraycopy(array, 0, result, n, tail);
			}
		}
	}

	/**
	 * 将队列里所有元素按顺序（较早进队的在前）置入数组
	 */
	public Object[] toArray() {
		Object[] result = new Object[count];
		copyTo(result);
		return result;
	}

	/**
	 * 将队列里所有元素按顺序（较早进队的在前）置入泛型数组
	 * 
	 * @param contents
	 *            预先申请好的泛型数组。如果容量不足以容纳所有元素
	 * @return
	 */
	public <T> T[] toArray(T[] contents) {
		if (contents.length < count) {
			@SuppressWarnings("unchecked")
			T[] newArray = (T[]) Array.newInstance(contents.getClass().getComponentType(), count);
			contents = newArray;
		}
		copyTo(contents);
		if (contents.length > count) {
			contents[count] = null;
		}
		return contents;
	}

	/**
	 * 将指定的元素推入队列。如果队列已满，将最早入队的元素移出队列
	 * 
	 * @param e
	 *            元素，不能为null
	 * @throws NullPointerException
	 *             when {@code e == null}
	 */
	public void offer(E e) {
		if (e == null) {
			throw new NullPointerException("Cannot offer a null object into queue.");
		}
		array[tail] = e;
		++tail;
		if (tail >= capacity) {
			tail = 0;
		}
		if (count < capacity) {
			++count;
		} else {
			head = tail;
		}
	}

	/**
	 * 从队首开始，移除指定个数的元素
	 * <p>
	 * 如果指定的个数小于0、或大于{@link #size()}，抛出IllegalArgumentException异常
	 * </p>
	 * 
	 * @param count
	 *            要删除多少个？
	 */
	public void remove(int count) {
		if (count < 0 || count > this.count) {
			throw new IllegalArgumentException("Invalid count want remove");
		}
		if (count == 0) {
			return;
		}
		if (count == this.count) {
			clear();
            return;
		}
		this.count -= count;
		head += count;
		if (head >= capacity) {
			head -= capacity;
		}
	}

	/**
	 * 返回队列里最早入队的一个元素，并将其从队列中移除。<br />
	 * 与{@link #poll}不同，本函数在队列为空时将抛出{@link NoSuchElementException}异常
	 * 
	 * @return 队首元素
	 * @throws NoSuchElementException
	 *             队列为空。
	 */
	public E remove() {
		E e = poll();
		if (null == e) {
			throw new NoSuchElementException();
		}
		return e;
	}

	/**
	 * 返回队列里最早入队的一个元素，并将其从队列中移除。<br />
	 * 与{@link #remove}不同，本函数在队列为空时不抛异常，只是返回null
	 * 
	 * @return 队首元素。如果队列为空将返回null
	 */
	public E poll() {
		if (isEmpty()) {
			return null;
		}
		@SuppressWarnings("unchecked")
		E e = (E) array[head];
		--count;
		++head;
		if (head >= capacity) {
			head = 0;
		}
		return e;
	}

/**
	 * 返回队列中最早入队那个元素，但并不将其移除。如果队列为空，抛出异常 <br />
	 * 参见{@link #peek
	 * 
	 * @return 队列中最早入队的元素。
	 * @throws NoSuchElementException
	 *             如果队列为空
	 */
	public E element() {
		E e = peek();
		if (null == e) {
			throw new NoSuchElementException();
		}
		return e;
	}

	/**
	 * 返回队列中最早入队那个元素，但并不将其移除。 <br />
	 * 参见{@link #element()}
	 * 
	 * @return 队列中最早入队的元素。如果队列为空，返回null;
	 */
	public E peek() {
		if (isEmpty()) {
			return null;
		} else {
			@SuppressWarnings("unchecked")
			E e = (E) array[head];
			return e;
		}
	}

}
