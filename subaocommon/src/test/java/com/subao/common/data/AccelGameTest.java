package com.subao.common.data;

import android.util.Pair;

import com.subao.common.RoboBase;
import com.subao.common.net.Protocol;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AccelGameTest
 * <p>Created by YinHaiBo on 2017/3/28.</p>
 */
public class AccelGameTest extends RoboBase {

    /**
     * 创建测试数据，用于测试{@link AccelGame}的whitePorts和blackPorts
     *
     * @return 一对数据，first为List，用于创建AccelGame对象时指定whitePorts或blackPorts，
     * second为array，用于测试时的期望值
     */
    private static Pair<List<AccelGame.PortRange>, AccelGame.PortRange[]> createPortRanges() {
        int count = 1 + (int) (Math.random() * 5);
        AccelGame.PortRange[] expected = new AccelGame.PortRange[count];
        List<AccelGame.PortRange> actual = new ArrayList<AccelGame.PortRange>(count);
        for (int i = 0; i < count; ++i) {
            int start = (int) (Math.random() * 10);
            int end = start + 1 + (int) (Math.random() * 40);
            AccelGame.PortRange portRange = new AccelGame.PortRange(start, end);
            expected[i] = portRange;
            actual.add(portRange);
        }
        return new Pair<List<AccelGame.PortRange>, AccelGame.PortRange[]>(
            actual, expected
        );
    }

    /**
     * 创建测试数据，用于测试IP列表是否符合预期
     *
     * @return 一个Pair. First为创建{@link AccelGame}时指定的IP列表，second为测试时的预期值
     */
    private static Pair<List<String>, String[]> createIPList() {
        int count = 1 + (int) (Math.random() * 5);
        String[] expected = new String[count];
        List<String> actual = new ArrayList<String>(count);
        for (int i = 0; i < count; ++i) {
            String s = Double.toString(Math.random());
            expected[i] = s;
            actual.add(s);
        }
        return new Pair<List<String>, String[]>(actual, expected);
    }

    /**
     * 测试给定的{@link com.subao.common.data.AccelGame.PortRange}列表是否符合期望值
     *
     * @param excepted 期望值
     * @param actual   要测试的端口范围列表
     */
    public static void testPortRanges(AccelGame.PortRange[] excepted, Iterable<AccelGame.PortRange> actual) {
        int i = 0;
        for (AccelGame.PortRange pr : actual) {
            assertEquals(excepted[i++], pr);
        }
        assertEquals(i, excepted.length);
    }

    /**
     * 测试给定的IP列表是否符合期望
     *
     * @param expectedIPList 预期的IP列表
     * @param actualIPList   实际的IP列表
     */
    public static void testIPList(String[] expectedIPList, Iterable<String> actualIPList) {
        int i = 0;
        for (String actual : actualIPList) {
            String expected = expectedIPList[i++];
            assertEquals(expected, actual);
        }
        assertEquals(i, expectedIPList.length);
    }

    @Test
    public void testConstDefines() {
        assertEquals(1, AccelGame.ACCEL_MODE_ALLOW);
        assertEquals(2, AccelGame.ACCEL_MODE_FAKE);
        assertEquals(3, AccelGame.ACCEL_MODE_DENY);
        assertEquals(1, AccelGame.FLAG_FOREIGN);
        assertEquals(2, AccelGame.FLAG_EXACT_MATCH);
//        assertEquals(4, AccelGame.FLAG_RECOMMEND_VPN);
//        assertEquals(8, AccelGame.FLAG_RECOMMEND_ROOT);
        assertEquals(16, AccelGame.FLAG_UDP);
        assertEquals(32, AccelGame.FLAG_TCP);
    }

    @Test
    public void testConstructor() {
        assertNull(new AccelGame.Builder().build(null));
        String appLabel = "王者荣耀";
        int accelMode = AccelGame.ACCEL_MODE_ALLOW;
        int flags = 33;
        Pair<List<AccelGame.PortRange>, AccelGame.PortRange[]> whitePorts = createPortRanges();
        Pair<List<AccelGame.PortRange>, AccelGame.PortRange[]> blackPorts = createPortRanges();
        Pair<List<String>, String[]> whiteIps = createIPList();
        Pair<List<String>, String[]> blackIps = createIPList();
        //
        AccelGame.Builder builder = new AccelGame.Builder();
        builder.setAccelMode(accelMode);
        builder.setFlags(flags);
        builder.setWhitePorts(whitePorts.first);
        builder.setBlackPorts(blackPorts.first);
        builder.setWhiteIps(whiteIps.first);
        builder.setBlackIps(blackIps.first);
        AccelGame accelGame = builder.build(appLabel);
        //
        assertEquals(appLabel, accelGame.appLabel);
        assertEquals(accelMode, accelGame.accelMode);
        assertEquals(flags, accelGame.flags);
        testPortRanges(whitePorts.second, accelGame.getWhitePorts());
        testPortRanges(blackPorts.second, accelGame.getBlackPorts());
        testIPList(whiteIps.second, accelGame.getWhiteIps());
        testIPList(blackIps.second, accelGame.getBlackIps());
    }

    @Test
    public void testPortRange() {
        AccelGame.PortRange portRange = new AccelGame.PortRange(123, 456);
        assertEquals(123, portRange.start);
        assertEquals(456, portRange.end);
        assertTrue(portRange.equals(portRange));
        assertFalse(portRange.equals(null));
        assertFalse(portRange.equals(this));
        assertTrue(portRange.equals(new AccelGame.PortRange(123, 456)));
        assertFalse(portRange.equals(new AccelGame.PortRange(123, 6)));
        assertFalse(portRange.equals(new AccelGame.PortRange(1, 456)));
        assertNotNull(portRange.toString());
    }

    @Test
    public void isLabelThreeAsciiChar() {
        AccelGame.Builder builder = new AccelGame.Builder();
        assertTrue(builder.build("ABC").isLabelThreeAsciiChar);
        assertFalse(builder.build("A王c").isLabelThreeAsciiChar);
    }

    @Test
    public void testFlags() {
        AccelGame.Builder builder = new AccelGame.Builder();
        builder.setFlags(AccelGame.FLAG_TCP);
        AccelGame accelGame = builder.build("test");
        assertFalse(accelGame.isForeign());
        assertFalse(accelGame.needExactMatch());
        assertEquals(Protocol.TCP, accelGame.getProtocol());
        assertFalse(accelGame.isAccelFake());
        //
        builder.setFlags(0xffffff & ~AccelGame.FLAG_TCP);
        accelGame = builder.build("test");
        assertTrue(accelGame.isForeign());
        assertTrue(accelGame.needExactMatch());
        assertEquals(Protocol.UDP, accelGame.getProtocol());
        assertFalse(accelGame.isAccelFake());
        //
        builder.setFlags(AccelGame.FLAG_TCP | AccelGame.FLAG_UDP);
        accelGame = builder.build("test");
        assertEquals(Protocol.BOTH, accelGame.getProtocol());
    }

    @Test
    public void testAccelFake() {
        AccelGame.Builder builder = new AccelGame.Builder();
        builder.setAccelMode(AccelGame.ACCEL_MODE_FAKE);
        AccelGame accelGame = builder.build("test");
        assertTrue(accelGame.isAccelFake());
    }

}