package com.subao.common.data;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AddressTest{
	
	@Test
	public void testEndPoint(){
		
		Address address = new Address();
		assertNotNull(address);
		
		Address.HostName host = new Address.HostName();
		assertNotNull(host);
		
		Address.EndPoint endPoint1 = new Address.EndPoint( Address.HostName.TEST_SERVER, 80);
		Address.EndPoint endPoint2 = new Address.EndPoint( Address.HostName.TEST_SERVER, 80);
		Address.EndPoint endPoint3 = new Address.EndPoint( Address.HostName.ISP_MAP, 2400);
		
		assertNotNull(endPoint1);
		assertNotNull(endPoint2);
		assertNotNull(endPoint3);
		
		String ss = endPoint1.toString();
		assertNotNull(ss);
		
		String test = "test" ;
		
		assertFalse(endPoint1.equals(null));
		assertFalse(endPoint1.equals(test));
		assertFalse(test.equals(endPoint1));
		
		assertTrue(endPoint1.equals(endPoint1));
		assertTrue(endPoint1.equals(endPoint2));
		
		assertFalse(endPoint1.equals(endPoint3));

		new Address.EndPoint(null, 0).toString();
	}
	
	
	@Test
	public void testServer(){
		assertNotNull(Address.EndPoint.MESSAGE);
		assertNotNull(Address.EndPoint.PORTAL);
		assertTrue(Address.EndPoint.MESSAGE.equals(Address.EndPoint.MESSAGE));
		assertFalse(Address.EndPoint.MESSAGE.equals(Address.EndPoint.PORTAL));
		String test = Address.HostName.TEST_SERVER;
		
		assertFalse(Address.EndPoint.MESSAGE.equals(null));
		assertFalse(Address.EndPoint.MESSAGE.equals(test));
		assertFalse(test.equals(Address.EndPoint.MESSAGE));
	}
}