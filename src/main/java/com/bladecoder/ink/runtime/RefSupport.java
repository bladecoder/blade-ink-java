package com.bladecoder.ink.runtime;

public class RefSupport<T> {
	T value;
	
	public void setValue(T value) {
		this.value = value;
	}
	
	public T getValue() {
		return value;
	}
}
