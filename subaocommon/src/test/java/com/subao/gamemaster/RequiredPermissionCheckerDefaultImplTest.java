package com.subao.gamemaster;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.subao.common.RoboBase;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * RequiredPermissionCheckerDefaultImplTest
 * <p>Created by YinHaiBo on 2017/3/17.</p>
 */
public class RequiredPermissionCheckerDefaultImplTest extends RoboBase {

    private GameMaster.RequiredPermissionCheckerDefaultImpl target;
    private Context mockContext;
    private PackageManager packageManager;
    private String packageName;
    private PackageInfo packageInfo;


    @Before
    public void setUp() {
        target = new GameMaster.RequiredPermissionCheckerDefaultImpl();
        mockContext = mock(Context.class);
        packageManager = mock(PackageManager.class);
        packageName = getContext().getPackageName();
        packageInfo = new PackageInfo();
    }

    @Test
    public void testGetPackageManagerFail() {
        doReturn(null).when(mockContext).getPackageManager();
        assertFalse(target.hasRequiredPermission(mockContext));
    }

    @Test
    public void testGetPackageNameFail() {
        PackageManager packageManager = mock(PackageManager.class);
        doReturn(packageManager).when(mockContext).getPackageManager();
        doReturn(null).when(mockContext).getPackageName();
        assertFalse(target.hasRequiredPermission(mockContext));
    }

    @Test
    public void testGetPackageInfoFail() throws PackageManager.NameNotFoundException {
        doReturn(packageManager).when(mockContext).getPackageManager();
        doReturn(packageName).when(mockContext).getPackageName();
        doReturn(null).when(packageManager).getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        assertFalse(target.hasRequiredPermission(mockContext));
    }

    @Test
    public void testRequestedPermissionsIsNull() throws PackageManager.NameNotFoundException {
        doReturn(packageManager).when(mockContext).getPackageManager();
        doReturn(packageName).when(mockContext).getPackageName();
        doReturn(packageInfo).when(packageManager).getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        assertFalse(target.hasRequiredPermission(mockContext));
    }

    @Test
    public void testHasNotPermission() throws PackageManager.NameNotFoundException {
        packageInfo.requestedPermissions = new String[]{
            "HELLO", "WORLD",
        };
        doReturn(packageManager).when(mockContext).getPackageManager();
        doReturn(packageName).when(mockContext).getPackageName();
        doReturn(packageInfo).when(packageManager).getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        assertFalse(target.hasRequiredPermission(mockContext));
    }

    @Test
    public void testHasPermission() throws PackageManager.NameNotFoundException {
        packageInfo.requestedPermissions = new String[] {
            "HELLO", "com.subao.permission.USE_SDK.test", "WORLD",
        };
        doReturn(packageManager).when(mockContext).getPackageManager();
        doReturn(packageName).when(mockContext).getPackageName();
        doReturn(packageInfo).when(packageManager).getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        assertTrue(target.hasRequiredPermission(mockContext));
    }

    @Test
    public void testException() throws PackageManager.NameNotFoundException {
        packageInfo.requestedPermissions = new String[] {
            "HELLO", "com.subao.permission.USE_SDK.test", "WORLD",
        };
        doReturn(packageManager).when(mockContext).getPackageManager();
        doReturn(packageName).when(mockContext).getPackageName();
        doThrow(PackageManager.NameNotFoundException.class).when(packageManager).getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        assertFalse(target.hasRequiredPermission(mockContext));
    }
}