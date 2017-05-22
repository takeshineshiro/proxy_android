package com.subao.common.collection;

public class Ref<T> {

	private T t;
	
	public void set(T t) {
		this.t = t;
	}
	
	public T get() {
		return t;
	}
	
	public Ref() {
		
	}
	
	public Ref(T t) {
		this.t = t;
	}
}
