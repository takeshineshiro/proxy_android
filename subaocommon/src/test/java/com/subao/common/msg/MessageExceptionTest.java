package com.subao.common.msg;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class MessageExceptionTest{
	@Test
	public void tesMessageException(){
		MessageException exception = new MessageException();
		assertNotNull(exception);
		
		MessageException exceptionMsg = new  MessageException("test message");
		assertNotNull(exceptionMsg);
	}
	
}