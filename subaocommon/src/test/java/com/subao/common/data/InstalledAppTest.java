package com.subao.common.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Process;

import com.subao.common.RoboBase;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * InstalledAppTest
 * <p>Created by YinHaiBo on 2017/3/28.</p>
 */
public class InstalledAppTest extends RoboBase {

    @Test
    public void constructor() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        testPrivateConstructor(InstalledApp.class);
    }

    @Test
    public void testInfo() {
        Context context = getContext();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        PackageManager packageManager = context.getPackageManager();
        String appLabel = applicationInfo.loadLabel(packageManager).toString();
        Drawable icon = applicationInfo.loadIcon(packageManager);
        //
        InstalledApp.Info info = new InstalledApp.Info(
            applicationInfo, appLabel, true
        );
        assertEquals(applicationInfo.uid, info.getUid());
        assertEquals(appLabel, info.getAppLabel());
        assertEquals(context.getPackageName(), info.getPackageName());
        assertTrue(info.isSDKEmbed());
        assertEquals(icon, info.loadIcon(context));
        //
        Context mockContext = mock(Context.class);
        doReturn(null).when(mockContext).getPackageManager();
        assertNull(info.loadIcon(mockContext));
    }

    @Test
    public void loadIcon() {
        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        doThrow(SecurityException.class).when(applicationInfo).loadIcon(any(PackageManager.class));
        InstalledApp.Info info = new InstalledApp.Info(applicationInfo, "test", false);
        assertNull(info.loadIcon(getContext()));
    }

    @Test
    public void isSDKEmbed_Exception() {
        boolean isSDKEmbed = InstalledApp.isSDKEmbed(getContext().getPackageManager(), "This is a not exist package name");
        assertFalse(isSDKEmbed);
    }

    @Test
    public void isSDKEmbed_NullPackageInfo() throws PackageManager.NameNotFoundException {
        Context context = mock(Context.class);
        PackageManager pm = mock(PackageManager.class);
        doReturn(pm).when(context).getPackageManager();
        //noinspection WrongConstant
        doReturn(null).when(pm).getPackageInfo(anyString(), anyInt());
        assertFalse(InstalledApp.isSDKEmbed(pm, getContext().getPackageName()));
    }

    @Test
    public void isSDKEmbed_NullPermissions() throws PackageManager.NameNotFoundException {
        Context context = mock(Context.class);
        PackageManager pm = mock(PackageManager.class);
        doReturn(pm).when(context).getPackageManager();
        PackageInfo pi = new PackageInfo();
        //noinspection WrongConstant
        doReturn(pi).when(pm).getPackageInfo(anyString(), anyInt());
        assertFalse(InstalledApp.isSDKEmbed(pm, getContext().getPackageName()));
    }

    @Test
    public void isSDKEmbed_NoPermissions() throws PackageManager.NameNotFoundException {
        Context context = mock(Context.class);
        PackageManager pm = mock(PackageManager.class);
        doReturn(pm).when(context).getPackageManager();
        PackageInfo pi = new PackageInfo();
        pi.requestedPermissions = new String[] {"test"};
        //noinspection WrongConstant
        doReturn(pi).when(pm).getPackageInfo(anyString(), anyInt());
        assertFalse(InstalledApp.isSDKEmbed(pm, getContext().getPackageName()));
    }

    @Test
    public void isSDKEmbed_HasPermission() throws PackageManager.NameNotFoundException {
        Context context = mock(Context.class);
        PackageManager pm = mock(PackageManager.class);
        doReturn(pm).when(context).getPackageManager();
        PackageInfo pi = new PackageInfo();
        pi.requestedPermissions = new String[] {"test", "com.subao.permission.USE_SDK.hello"};
        //noinspection WrongConstant
        doReturn(pi).when(pm).getPackageInfo(anyString(), anyInt());
        assertTrue(InstalledApp.isSDKEmbed(pm, getContext().getPackageName()));
    }

    @Test
    public void getInstalledAppList_Fail() throws Exception {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(context).getPackageManager();
        //noinspection WrongConstant
        doReturn(null).when(packageManager).getInstalledApplications(anyInt());
        List<InstalledApp.Info> list = InstalledApp.getInstalledAppList(context);
        assertNull(list);
    }

    @Test
    public void getInstalledAppList_Ok() throws Exception {
        Context mockContext = mock(Context.class);
        doReturn("me").when(mockContext).getPackageName();
        PackageManager mockPackageManager = mock(PackageManager.class);
        doReturn(mockPackageManager).when(mockContext).getPackageManager();
        List<ApplicationInfo> listApplicationInfo = new ArrayList<ApplicationInfo>(4);
        listApplicationInfo.add(createApplicationInfo(android.os.Process.FIRST_APPLICATION_UID - 1, "test", false)); // need skip
        listApplicationInfo.add(createApplicationInfo(android.os.Process.FIRST_APPLICATION_UID, "test", false)); // ok
        listApplicationInfo.add(createApplicationInfo(android.os.Process.FIRST_APPLICATION_UID + 1, "test", true)); // need skip
        listApplicationInfo.add(createApplicationInfo(android.os.Process.FIRST_APPLICATION_UID + 2, "me", false)); // need skip
        listApplicationInfo.add(createApplicationInfo(Process.LAST_APPLICATION_UID, "test2", false)); // ok
        listApplicationInfo.add(createApplicationInfo(Process.LAST_APPLICATION_UID + 1, "test2", false)); // need skip

        //noinspection WrongConstant
        doReturn(listApplicationInfo).when(mockPackageManager).getInstalledApplications(anyInt());
        List<InstalledApp.Info> list = InstalledApp.getInstalledAppList(mockContext);
        assertEquals(2, list.size());
    }

    private static ApplicationInfo createApplicationInfo(int uid, String packageName, boolean isSystemApp) {
        ApplicationInfo result = new ApplicationInfo();
        result.uid = uid;
        result.packageName = packageName;
        if (isSystemApp) {
            result.flags = ApplicationInfo.FLAG_SYSTEM;
        }
        return result;
    }
}