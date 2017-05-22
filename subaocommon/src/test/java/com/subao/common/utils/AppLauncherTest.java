package com.subao.common.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * AppLauncherTest
 * <p>Created by YinHaiBo on 2017/4/4.</p>
 */
public class AppLauncherTest extends RoboBase {

    private static final String PACKAGE_NAME = "com.example.game";
    private static final String CLASS_NAME = "className";

    private static final String LINE_OK = "starting: intent";
    private static final String LINE_ERROR = "error";

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(AppLauncher.class);
    }

    @Test
    public void execute() {
        assertFalse(AppLauncher.execute(getContext(), null));
        // Null PackageManager
        Context context = mock(Context.class);
        doReturn(null).when(context).getPackageManager();
        assertFalse(AppLauncher.execute(context, PACKAGE_NAME));
        // 测试getLaunchIntentForPackage()返回null
        context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        doReturn(null).when(packageManager).getLaunchIntentForPackage(anyString());
        assertFalse(AppLauncher.execute(context, PACKAGE_NAME));
        // 测试getLaunchIntentForPackage()抛出异常
        context = mock(Context.class);
        packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        doThrow(SecurityException.class).when(packageManager).getLaunchIntentForPackage(anyString());
        assertFalse(AppLauncher.execute(context, PACKAGE_NAME));
        // 测试getLaunchIntentForPackage()返回Intent
        context = mock(Context.class);
        packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        Intent intent = mock(Intent.class);
        ComponentName componentName = new ComponentName(PACKAGE_NAME, CLASS_NAME);
        doReturn(componentName).when(intent).getComponent();
        doReturn(intent).when(packageManager).getLaunchIntentForPackage(anyString());
        assertTrue(AppLauncher.execute(context, PACKAGE_NAME));
        // startActivity()抛出异常
        context = mock(Context.class);
        packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        intent = mock(Intent.class);
        componentName = new ComponentName(PACKAGE_NAME, CLASS_NAME);
        doReturn(componentName).when(intent).getComponent();
        doReturn(intent).when(packageManager).getLaunchIntentForPackage(anyString());
        doThrow(SecurityException.class).when(context).startActivity(any(Intent.class));
        assertFalse(AppLauncher.execute(context, PACKAGE_NAME));
    }

    @Test
    public void executeWithAMOk() {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        Intent intent = mock(Intent.class);
        ComponentName componentName = new ComponentName(PACKAGE_NAME, CLASS_NAME);
        doReturn(componentName).when(intent).getComponent();
        doReturn(intent).when(packageManager).getLaunchIntentForPackage(anyString());
        assertTrue(AppLauncher.execute(context, PACKAGE_NAME, new AppLauncher.AMExecutor() {
            @Override
            public boolean execute(Intent intent, AppLauncher.ProcessBuilderWrapper processBuilder) {
                return true;
            }
        }));
    }

    @Test
    public void getComponentName() {
        assertNull(AppLauncher.getComponentName(null));
        Intent intent = mock(Intent.class);
        ComponentName componentName = new ComponentName(PACKAGE_NAME, CLASS_NAME);
        doReturn(componentName).when(intent).getComponent();
        assertEquals(String.format("%s/%s", PACKAGE_NAME, CLASS_NAME), AppLauncher.getComponentName(intent));
    }

    @Test
    public void executeWithAM() throws IOException {
        AppLauncher.AMExecutor amExecutor = new AppLauncher.AMExecutor();
        //
        Intent intent = mock(Intent.class);
        doReturn(null).when(intent).getComponent();
        assertFalse(amExecutor.execute(intent, null));
        //
        testExecuteWithAM(LINE_OK, true, false);
        testExecuteWithAM(LINE_ERROR, false, false);
        testExecuteWithAM(LINE_OK, false, true);
    }

    private void testExecuteWithAM(String processResultLine, boolean result, boolean runtimeException) {
        AppLauncher.AMExecutor amExecutor = new AppLauncher.AMExecutor();
        //
        Intent intent;
        intent = mock(Intent.class);
        doReturn(new ComponentName(PACKAGE_NAME, CLASS_NAME)).when(intent).getComponent();
        Process process;
        if (runtimeException) {
            process = null;
        } else {
            process = mock(Process.class);
            doReturn(new ByteArrayInputStream(processResultLine.getBytes())).when(process).getInputStream();
        }
        AppLauncher.ProcessBuilderWrapper processBuilder = new MockProcessBuilderWrapper(process);
        assertEquals(result, amExecutor.execute(intent, processBuilder));
    }

    private static class MockProcessBuilderWrapper extends AppLauncher.ProcessBuilderWrapper {

        private final Process process;

        MockProcessBuilderWrapper(Process process) {
            this.process = process;
        }

        @Override
        public Process start() throws IOException {
            return this.process;
        }
    }
}