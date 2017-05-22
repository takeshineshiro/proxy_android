package com.subao.common.data;

import com.subao.common.net.Protocol;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SupportGameTest {

    @Test
    public void test() {
        int uid = 100164;
        String packageName = "com.example.game.test";
        String appLabel = "测试";
        Protocol protocol = Protocol.UDP;
        boolean isForeign = false;
        List<AccelGame.PortRange> whitePorts = new ArrayList<AccelGame.PortRange>(1);
        whitePorts.add(new AccelGame.PortRange(1006, 1007));
        List<AccelGame.PortRange> blackPorts = new ArrayList<AccelGame.PortRange>(1);
        blackPorts.add(new AccelGame.PortRange(8888, 8889));
        List<String> whiteIps = new ArrayList<String>(1);
        whiteIps.add("127.0.0.1");
        List<String> blackIps = new ArrayList<String>(1);
        blackIps.add("199.199.1.1");
        //
        SupportGame game = new SupportGame(uid, packageName, appLabel, protocol, isForeign, whitePorts, blackPorts, whiteIps, blackIps);
        assertNotNull(game.toString());
        assertTrue(game.equals(game));
        assertFalse(game.equals(null));
        assertFalse(game.equals(this));
        //
        SupportGame game1 = new SupportGame(uid, packageName, appLabel, protocol, isForeign, whitePorts, blackPorts, whiteIps, blackIps);
        assertTrue(game.equals(game1));
        assertFalse(game.equals(new SupportGame(uid + 1, packageName, appLabel, protocol, isForeign, whitePorts, blackPorts, whiteIps, blackIps)));
        assertFalse(game.equals(new SupportGame(uid, packageName + "other", appLabel, protocol, isForeign, whitePorts, blackPorts, whiteIps, blackIps)));
        assertFalse(game.equals(new SupportGame(uid, packageName, appLabel, null, isForeign, whitePorts, blackPorts, whiteIps, blackIps)));
        assertFalse(game.equals(new SupportGame(uid, packageName, appLabel, protocol, !isForeign, whitePorts, blackPorts, whiteIps, blackIps)));
        assertFalse(game.equals(new SupportGame(uid, packageName, appLabel, protocol, isForeign, null, blackPorts, whiteIps, blackIps)));
        assertFalse(game.equals(new SupportGame(uid, packageName, appLabel, protocol, isForeign, whitePorts, null, whiteIps, blackIps)));
        assertFalse(game.equals(new SupportGame(uid, packageName, appLabel, protocol, isForeign, whitePorts, blackPorts, null, blackIps)));
        assertFalse(game.equals(new SupportGame(uid, packageName, appLabel, protocol, isForeign, whitePorts, blackPorts, whiteIps, null)));
        assertFalse(game.equals(new SupportGame(uid, packageName, appLabel + "other", protocol, isForeign, whitePorts, blackPorts, whiteIps, blackIps)));

    }


}