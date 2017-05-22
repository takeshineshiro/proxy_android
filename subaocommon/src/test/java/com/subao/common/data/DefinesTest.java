package com.subao.common.data;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefinesTest {

    @Test
    public void testModuleType() {
        for (Defines.ModuleType mt : Defines.ModuleType.values()) {
            assertNotNull(mt.name);
        }
    }

    @Test
    public void testConstructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RoboBase.testPrivateConstructor(Defines.class);
        RoboBase.testPrivateConstructor(Defines.VPNJniStrKey.class);
    }

    @Test
    public void testJniStrKey() {
        assertEquals("key_isp", Defines.VPNJniStrKey.KEY_ISP);
        assertEquals("key_front_game_uid", Defines.VPNJniStrKey.KEY_FRONT_GAME_UID);
        assertEquals("key_free_flow_type", Defines.VPNJniStrKey.KEY_FREE_FLOW_TYPE);
        assertEquals("key_game_server_id", Defines.VPNJniStrKey.KEY_GAME_SERVER_ID);
        assertEquals("key_net_state", Defines.VPNJniStrKey.KEY_NET_STATE);
        assertEquals("key_enable_qpp", Defines.VPNJniStrKey.KEY_ENABLE_QPP);
        assertEquals("key_enable_qos", Defines.VPNJniStrKey.KEY_ENABLE_QOS);
        assertEquals("key_subao_id", Defines.VPNJniStrKey.KEY_SUBAO_ID);
        assertEquals("key_version", Defines.VPNJniStrKey.KEY_VERSION);
        assertEquals("key_channel", Defines.VPNJniStrKey.KEY_CHANNEL);
        assertEquals("key_os_version", Defines.VPNJniStrKey.KEY_OS_VERSION);
        assertEquals("key_android_version", Defines.VPNJniStrKey.KEY_ANDROID_VERSION);
        assertEquals("key_phone_model", Defines.VPNJniStrKey.KEY_PHONE_MODEL);
        assertEquals("key_rom", Defines.VPNJniStrKey.KEY_ROM);
        assertEquals("key_cpu_speed", Defines.VPNJniStrKey.KEY_CPU_SPEED);
        assertEquals("key_cpu_core", Defines.VPNJniStrKey.KEY_CPU_CORE);
        assertEquals("key_memory", Defines.VPNJniStrKey.KEY_MEMORY);
        assertEquals("key_inject", Defines.VPNJniStrKey.KEY_INJECT);
        assertEquals("key_sdk_guid", Defines.VPNJniStrKey.KEY_SDK_GUID);
        assertEquals("key_hook_module", Defines.VPNJniStrKey.KEY_HOOK_MODULE);
        assertEquals("key_convergence_node", Defines.VPNJniStrKey.KEY_CONVERGENCE_NODE);
        assertEquals("key_pay_type_white_list", Defines.VPNJniStrKey.KEY_PAY_TYPE_WHITE_LIST);
        assertEquals("key_cellular_state_change", Defines.VPNJniStrKey.KEY_CELLULAR_STATE_CHANGE);
        assertEquals("key_mobile_switch_state", Defines.VPNJniStrKey.KEY_MOBILE_SWITCH_STATE);
        assertEquals("key_user_wifi_accel", Defines.VPNJniStrKey.KEY_USER_WIFI_ACCEL);
        assertEquals("key_add_game", Defines.VPNJniStrKey.KEY_ADD_GAME);
    }

}