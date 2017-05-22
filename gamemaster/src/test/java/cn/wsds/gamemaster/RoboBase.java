package cn.wsds.gamemaster;

import android.app.Application;
import android.content.Context;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * RoboBase
 * <p>Created by YinHaiBo on 2017/3/3.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class)
public abstract class RoboBase {

    protected Context getContext() {
        return RuntimeEnvironment.application;
    }

    public static <T> void testPrivateConstructor(Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        assertTrue(0 != (constructor.getModifiers() & Modifier.PRIVATE));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
