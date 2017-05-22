/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/wong/Android_APP/gamemaster/src/main/aidl/cn/wsds/gamemaster/service/aidl/IGameVpnService.aidl
 */
package cn.wsds.gamemaster.service.aidl;
public interface IGameVpnService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements cn.wsds.gamemaster.service.aidl.IGameVpnService
{
private static final java.lang.String DESCRIPTOR = "cn.wsds.gamemaster.service.aidl.IGameVpnService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an cn.wsds.gamemaster.service.aidl.IGameVpnService interface,
 * generating a proxy if needed.
 */
public static cn.wsds.gamemaster.service.aidl.IGameVpnService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof cn.wsds.gamemaster.service.aidl.IGameVpnService))) {
return ((cn.wsds.gamemaster.service.aidl.IGameVpnService)iin);
}
return new cn.wsds.gamemaster.service.aidl.IGameVpnService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_isVPNStarted:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isVPNStarted();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setToForeground:
{
data.enforceInterface(DESCRIPTOR);
this.setToForeground();
reply.writeNoException();
return true;
}
case TRANSACTION_setToBackground:
{
data.enforceInterface(DESCRIPTOR);
this.setToBackground();
reply.writeNoException();
return true;
}
case TRANSACTION_closeVPN:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.closeVPN(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_startVPN:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<java.lang.String> _arg0;
_arg0 = data.createStringArrayList();
int _result = this.startVPN(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_isIpEqualInterfaceAddress:
{
data.enforceInterface(DESCRIPTOR);
byte[] _arg0;
_arg0 = data.createByteArray();
boolean _result = this.isIpEqualInterfaceAddress(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_protect:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.protect(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getVpnAccelState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getVpnAccelState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_isInitFailException:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isInitFailException();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_vpnInit:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.vpnInit();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_vpnSendPutSupportGame:
{
data.enforceInterface(DESCRIPTOR);
cn.wsds.gamemaster.service.aidl.VpnSupportGame _arg0;
if ((0!=data.readInt())) {
_arg0 = cn.wsds.gamemaster.service.aidl.VpnSupportGame.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.vpnSendPutSupportGame(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendPutSupportGames:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<cn.wsds.gamemaster.service.aidl.VpnSupportGame> _arg0;
_arg0 = data.createTypedArrayList(cn.wsds.gamemaster.service.aidl.VpnSupportGame.CREATOR);
this.vpnSendPutSupportGames(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendSetLogLevel:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.vpnSendSetLogLevel(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnCheckSocketState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.vpnCheckSocketState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_vpnGetAccelStatus:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.vpnGetAccelStatus();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_vpnNetworkCheck:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.vpnNetworkCheck();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_vpnSetRootMode:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.vpnSetRootMode();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_vpnStartProxy:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
boolean _result = this.vpnStartProxy(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_vpnStopProxy:
{
data.enforceInterface(DESCRIPTOR);
this.vpnStopProxy();
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendUnionAccelSwitch:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.vpnSendUnionAccelSwitch(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnIsNodeAlreadyDetected:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.vpnIsNodeAlreadyDetected(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_vpnStartNodeDetect:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _arg1;
_arg1 = (0!=data.readInt());
this.vpnStartNodeDetect(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnGetAppLogCache:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.vpnGetAppLogCache();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_vpnOpenQosAccelResult:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
int _arg2;
_arg2 = data.readInt();
this.vpnOpenQosAccelResult(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnModifyQosAccelResult:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
this.vpnModifyQosAccelResult(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnCloseQosAccelResult:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.vpnCloseQosAccelResult(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendSetNetworkState:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.vpnSendSetNetworkState(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnOnNewMobileNetworkFD:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.vpnOnNewMobileNetworkFD(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendStartGameDelayDetect:
{
data.enforceInterface(DESCRIPTOR);
this.vpnSendStartGameDelayDetect();
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendStopGameDelayDetect:
{
data.enforceInterface(DESCRIPTOR);
this.vpnSendStopGameDelayDetect();
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendSetFrontGameUid:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.vpnSendSetFrontGameUid(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendSetJNIBooleanValue:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _arg1;
_arg1 = (0!=data.readInt());
this.vpnSendSetJNIBooleanValue(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendSetJNIIntValue:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.vpnSendSetJNIIntValue(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_vpnSendSetJNIStringValue:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
this.vpnSendSetJNIStringValue(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getLocalPort:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getLocalPort();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setUserToken:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
this.setUserToken(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_registCellularWatcher:
{
data.enforceInterface(DESCRIPTOR);
this.registCellularWatcher();
reply.writeNoException();
return true;
}
case TRANSACTION_onSubaoIdUpdate:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onSubaoIdUpdate(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_sendKeepalive:
{
data.enforceInterface(DESCRIPTOR);
this.sendKeepalive();
reply.writeNoException();
return true;
}
case TRANSACTION_getVIPValidTime:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getVIPValidTime();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getAccelerationStatus:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getAccelerationStatus();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_configChange:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
this.configChange(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements cn.wsds.gamemaster.service.aidl.IGameVpnService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public boolean isVPNStarted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isVPNStarted, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setToForeground() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_setToForeground, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setToBackground() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_setToBackground, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void closeVPN(int reason) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(reason);
mRemote.transact(Stub.TRANSACTION_closeVPN, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int startVPN(java.util.List<java.lang.String> supportPackageNames) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStringList(supportPackageNames);
mRemote.transact(Stub.TRANSACTION_startVPN, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isIpEqualInterfaceAddress(byte[] ip) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeByteArray(ip);
mRemote.transact(Stub.TRANSACTION_isIpEqualInterfaceAddress, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean protect(int fd) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(fd);
mRemote.transact(Stub.TRANSACTION_protect, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getVpnAccelState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getVpnAccelState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isInitFailException() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isInitFailException, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int vpnInit() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnInit, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void vpnSendPutSupportGame(cn.wsds.gamemaster.service.aidl.VpnSupportGame supportGame) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((supportGame!=null)) {
_data.writeInt(1);
supportGame.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_vpnSendPutSupportGame, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendPutSupportGames(java.util.List<cn.wsds.gamemaster.service.aidl.VpnSupportGame> supportGames) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedList(supportGames);
mRemote.transact(Stub.TRANSACTION_vpnSendPutSupportGames, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendSetLogLevel(int level) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(level);
mRemote.transact(Stub.TRANSACTION_vpnSendSetLogLevel, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int vpnCheckSocketState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnCheckSocketState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int vpnGetAccelStatus() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnGetAccelStatus, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean vpnNetworkCheck() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnNetworkCheck, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean vpnSetRootMode() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnSetRootMode, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean vpnStartProxy(int mode, int vpnfd) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(mode);
_data.writeInt(vpnfd);
mRemote.transact(Stub.TRANSACTION_vpnStartProxy, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void vpnStopProxy() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnStopProxy, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendUnionAccelSwitch(boolean checked) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((checked)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_vpnSendUnionAccelSwitch, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean vpnIsNodeAlreadyDetected(int uid) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(uid);
mRemote.transact(Stub.TRANSACTION_vpnIsNodeAlreadyDetected, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void vpnStartNodeDetect(int gameUID, boolean force) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(gameUID);
_data.writeInt(((force)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_vpnStartNodeDetect, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String vpnGetAppLogCache() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnGetAppLogCache, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void vpnOpenQosAccelResult(int id, java.lang.String speedId, int error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(id);
_data.writeString(speedId);
_data.writeInt(error);
mRemote.transact(Stub.TRANSACTION_vpnOpenQosAccelResult, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnModifyQosAccelResult(int id, int timeSeconds, int error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(id);
_data.writeInt(timeSeconds);
_data.writeInt(error);
mRemote.transact(Stub.TRANSACTION_vpnModifyQosAccelResult, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnCloseQosAccelResult(int id, int error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(id);
_data.writeInt(error);
mRemote.transact(Stub.TRANSACTION_vpnCloseQosAccelResult, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendSetNetworkState(int type) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(type);
mRemote.transact(Stub.TRANSACTION_vpnSendSetNetworkState, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnOnNewMobileNetworkFD(int fd) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(fd);
mRemote.transact(Stub.TRANSACTION_vpnOnNewMobileNetworkFD, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendStartGameDelayDetect() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnSendStartGameDelayDetect, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendStopGameDelayDetect() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_vpnSendStopGameDelayDetect, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendSetFrontGameUid(int uid) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(uid);
mRemote.transact(Stub.TRANSACTION_vpnSendSetFrontGameUid, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendSetJNIBooleanValue(int key, boolean value) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(key);
_data.writeInt(((value)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_vpnSendSetJNIBooleanValue, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendSetJNIIntValue(int key, int value) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(key);
_data.writeInt(value);
mRemote.transact(Stub.TRANSACTION_vpnSendSetJNIIntValue, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void vpnSendSetJNIStringValue(int key, java.lang.String value) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(key);
_data.writeString(value);
mRemote.transact(Stub.TRANSACTION_vpnSendSetJNIStringValue, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getLocalPort() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLocalPort, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setUserToken(java.lang.String openId, java.lang.String token, java.lang.String appId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(openId);
_data.writeString(token);
_data.writeString(appId);
mRemote.transact(Stub.TRANSACTION_setUserToken, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void registCellularWatcher() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_registCellularWatcher, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onSubaoIdUpdate(java.lang.String subaoId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(subaoId);
mRemote.transact(Stub.TRANSACTION_onSubaoIdUpdate, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sendKeepalive() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_sendKeepalive, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String getVIPValidTime() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getVIPValidTime, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getAccelerationStatus() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAccelerationStatus, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void configChange(int name, java.lang.String value) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(name);
_data.writeString(value);
mRemote.transact(Stub.TRANSACTION_configChange, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_isVPNStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_setToForeground = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_setToBackground = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_closeVPN = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_startVPN = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_isIpEqualInterfaceAddress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_protect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getVpnAccelState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_isInitFailException = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_vpnInit = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_vpnSendPutSupportGame = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_vpnSendPutSupportGames = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_vpnSendSetLogLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_vpnCheckSocketState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_vpnGetAccelStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_vpnNetworkCheck = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_vpnSetRootMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_vpnStartProxy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_vpnStopProxy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_vpnSendUnionAccelSwitch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_vpnIsNodeAlreadyDetected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_vpnStartNodeDetect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
static final int TRANSACTION_vpnGetAppLogCache = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
static final int TRANSACTION_vpnOpenQosAccelResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
static final int TRANSACTION_vpnModifyQosAccelResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
static final int TRANSACTION_vpnCloseQosAccelResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
static final int TRANSACTION_vpnSendSetNetworkState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
static final int TRANSACTION_vpnOnNewMobileNetworkFD = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
static final int TRANSACTION_vpnSendStartGameDelayDetect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
static final int TRANSACTION_vpnSendStopGameDelayDetect = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
static final int TRANSACTION_vpnSendSetFrontGameUid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
static final int TRANSACTION_vpnSendSetJNIBooleanValue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);
static final int TRANSACTION_vpnSendSetJNIIntValue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
static final int TRANSACTION_vpnSendSetJNIStringValue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
static final int TRANSACTION_getLocalPort = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
static final int TRANSACTION_setUserToken = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
static final int TRANSACTION_registCellularWatcher = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
static final int TRANSACTION_onSubaoIdUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
static final int TRANSACTION_sendKeepalive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
static final int TRANSACTION_getVIPValidTime = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
static final int TRANSACTION_getAccelerationStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
static final int TRANSACTION_configChange = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
}
public boolean isVPNStarted() throws android.os.RemoteException;
public void setToForeground() throws android.os.RemoteException;
public void setToBackground() throws android.os.RemoteException;
public void closeVPN(int reason) throws android.os.RemoteException;
public int startVPN(java.util.List<java.lang.String> supportPackageNames) throws android.os.RemoteException;
public boolean isIpEqualInterfaceAddress(byte[] ip) throws android.os.RemoteException;
public boolean protect(int fd) throws android.os.RemoteException;
public int getVpnAccelState() throws android.os.RemoteException;
public boolean isInitFailException() throws android.os.RemoteException;
public int vpnInit() throws android.os.RemoteException;
public void vpnSendPutSupportGame(cn.wsds.gamemaster.service.aidl.VpnSupportGame supportGame) throws android.os.RemoteException;
public void vpnSendPutSupportGames(java.util.List<cn.wsds.gamemaster.service.aidl.VpnSupportGame> supportGames) throws android.os.RemoteException;
public void vpnSendSetLogLevel(int level) throws android.os.RemoteException;
public int vpnCheckSocketState() throws android.os.RemoteException;
public int vpnGetAccelStatus() throws android.os.RemoteException;
public boolean vpnNetworkCheck() throws android.os.RemoteException;
public boolean vpnSetRootMode() throws android.os.RemoteException;
public boolean vpnStartProxy(int mode, int vpnfd) throws android.os.RemoteException;
public void vpnStopProxy() throws android.os.RemoteException;
public void vpnSendUnionAccelSwitch(boolean checked) throws android.os.RemoteException;
public boolean vpnIsNodeAlreadyDetected(int uid) throws android.os.RemoteException;
public void vpnStartNodeDetect(int gameUID, boolean force) throws android.os.RemoteException;
public java.lang.String vpnGetAppLogCache() throws android.os.RemoteException;
public void vpnOpenQosAccelResult(int id, java.lang.String speedId, int error) throws android.os.RemoteException;
public void vpnModifyQosAccelResult(int id, int timeSeconds, int error) throws android.os.RemoteException;
public void vpnCloseQosAccelResult(int id, int error) throws android.os.RemoteException;
public void vpnSendSetNetworkState(int type) throws android.os.RemoteException;
public void vpnOnNewMobileNetworkFD(int fd) throws android.os.RemoteException;
public void vpnSendStartGameDelayDetect() throws android.os.RemoteException;
public void vpnSendStopGameDelayDetect() throws android.os.RemoteException;
public void vpnSendSetFrontGameUid(int uid) throws android.os.RemoteException;
public void vpnSendSetJNIBooleanValue(int key, boolean value) throws android.os.RemoteException;
public void vpnSendSetJNIIntValue(int key, int value) throws android.os.RemoteException;
public void vpnSendSetJNIStringValue(int key, java.lang.String value) throws android.os.RemoteException;
public int getLocalPort() throws android.os.RemoteException;
public void setUserToken(java.lang.String openId, java.lang.String token, java.lang.String appId) throws android.os.RemoteException;
public void registCellularWatcher() throws android.os.RemoteException;
public void onSubaoIdUpdate(java.lang.String subaoId) throws android.os.RemoteException;
public void sendKeepalive() throws android.os.RemoteException;
public java.lang.String getVIPValidTime() throws android.os.RemoteException;
public int getAccelerationStatus() throws android.os.RemoteException;
public void configChange(int name, java.lang.String value) throws android.os.RemoteException;
}
