package com.subao.common.data;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SubaoIdManagerTest extends RoboBase {
	
	@Test
	public void testSubaoIdManager(){
		assertEquals(36, SubaoIdManager.EMPTY_SUBAO_ID.length());
		SubaoIdManager instance = SubaoIdManager.getInstance();
		assertNotNull(instance);
		
		instance.init(getContext());
		
		String id = UUID.randomUUID().toString();
		instance.setSubaoId(id);
		assertTrue(instance.isSubaoIdValid());
		
		String subaoId = instance.getSubaoId();
		assertNotNull(subaoId);
		assertTrue(SubaoIdManager.isSubaoIdValid(subaoId));
		
		instance.clear();
		
	}
}