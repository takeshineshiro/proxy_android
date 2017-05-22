package com.subao.common.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RegionAndISPTest {

	@Test
	public void testResult() {
		RegionAndISP r = new RegionAndISP(12, 34);
		assertEquals(12, r.region);
		assertEquals(34, r.isp);
		assertNotNull(r.toString());
		RegionAndISP r2 = new RegionAndISP(12, 34);
		assertEquals(r2, r);
		assertEquals(r, r);
		assertTrue(r.equals(r2));
		assertFalse(r.equals(null));
		assertFalse(r.equals(this));
		assertFalse(r.equals(new RegionAndISP(12, 0)));
		assertFalse(r.equals(new RegionAndISP(0, 34)));
	}

	@Test
	public void testHashCode() {
		RegionAndISP r1 = new RegionAndISP(12, 34);
		RegionAndISP r2 = new RegionAndISP(12, 34);
		assertEquals(r1.hashCode(), r2.hashCode());
	}

    private static String transRegion(int region) {
        for (ChinaRegion cr : ChinaRegion.values()) {
            if (region == cr.num) {
                return cr.pinyin;
            }
        }
        return Integer.toString(region);
    }

    private static String transISP(int isp) {
        for (ChinaISP ci : ChinaISP.values()) {
            if (isp == ci.num) {
                return ci.code;
            }
        }
        return Integer.toString(isp);
    }

    @Test
    public void toText() {
        assertNotNull(new RegionAndISP(51, 10).toString());
        assertNull(RegionAndISP.toText(null));
        for (int region = 11; region <= 81; ++region) {
            for (int isp = 10; isp <= 13; ++isp) {
                String expected = transRegion(region) + '.' + transISP(isp);
                assertEquals(expected, RegionAndISP.toText(new RegionAndISP(region, isp)));
            }
        }
    }

    @Test
    public void toSimpleText() {
        assertEquals("32.10", new RegionAndISP(32, 10).toSimpleText());
    }
}
