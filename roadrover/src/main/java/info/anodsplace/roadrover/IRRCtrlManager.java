package info.anodsplace.roadrover;

import android.os.*;
import android.util.Log;

public interface IRRCtrlManager extends IInterface
{
    int audioMute(final int p0) throws RemoteException;
    
    int deviceClose(final int p0, final IRRCtrlDeviceListener p1) throws RemoteException;
    
    int deviceControl(final int p0, final int p1, final int p2, final int p3) throws RemoteException;
    
    int deviceControlExt(final int p0, final int p1, final byte[] p2) throws RemoteException;
    
    int deviceOpen(final int p0, final int p1, final int p2, final IRRCtrlDeviceListener p3) throws RemoteException;
    
    int getDeviceIntParam(final int p0, final int p1) throws RemoteException;
    
    byte[] getDeviceParam(final int p0, final int p1) throws RemoteException;
    
    int getIntParam(final int p0) throws RemoteException;
    
    String getVersion() throws RemoteException;
    
    int mcuLogoUpdate(final String p0, final int p1, final int p2, final int p3, final int p4) throws RemoteException;
    
    int reportCmdComplete(final int p0, final int p1, final int p2, final byte[] p3) throws RemoteException;
    
    int reportDeviceCmdComplete(final int p0, final int p1, final int p2, final int p3, final int p4, final byte[] p5) throws RemoteException;
    
    int reportDeviceInfo(final int p0, final int p1, final byte[] p2) throws RemoteException;
    
    int reportDeviceParamChanged(final int p0, final int p1, final byte[] p2) throws RemoteException;
    
    int reportRRCtrlParamChange(final int p0, final int p1, final int p2, final byte[] p3) throws RemoteException;
    
    void requestRRUpdates(final int p0, final IRRCtrlListener p1) throws RemoteException;
    
    int screenMute(final int p0) throws RemoteException;
    
    void sendADValue(final int p0, final int p1) throws RemoteException;
    
    int sendDeviceMsg(final int p0, final int p1, final byte[] p2) throws RemoteException;
    
    void sendKBDData(final int p0, final int p1, final int p2) throws RemoteException;
    
    void sendKey(final int p0, final int p1, final int p2) throws RemoteException;
    
    void sendOSDButton(final int p0, final int p1) throws RemoteException;
    
    int sendPCStatus(final int p0) throws RemoteException;
    
    int sendStringCommand(final int p0, final String p1, final String p2) throws RemoteException;
    
    void sendTouchMsg(final int p0, final int p1, final int p2, final int p3) throws RemoteException;
    
    int setDeviceIntParam(final int p0, final int p1, final int p2) throws RemoteException;
    
    int setDeviceParam(final int p0, final int p1, final byte[] p2) throws RemoteException;
    
    int setIntParam(final int p0, final int p1) throws RemoteException;
    
    int switchAudioOut(final int p0, final int p1) throws RemoteException;
    
    int switchScreenOut(final int p0, final int p1) throws RemoteException;
    
    void unrequestRRUpdates(final int p0, final IRRCtrlListener p1) throws RemoteException;
    
    public abstract static class Stub extends Binder implements IRRCtrlManager
    {
        public static IRRCtrlManager get() {
            final Object o = new Object();
            try {
                return IRRCtrlManager.Stub.asInterface((IBinder)Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(o, "rrctrl"));
            }
            catch (Exception ex) {
                Log.e("Roadrover", ex.getMessage(), ex);
                return null;
            }
        }

        private static final String DESCRIPTOR = "android.rrctrl.IRRCtrlManager";
        static final int TRANSACTION_audioMute = 8;
        static final int TRANSACTION_deviceClose = 11;
        static final int TRANSACTION_deviceControl = 12;
        static final int TRANSACTION_deviceControlExt = 13;
        static final int TRANSACTION_deviceOpen = 10;
        static final int TRANSACTION_getDeviceIntParam = 14;
        static final int TRANSACTION_getDeviceParam = 15;
        static final int TRANSACTION_getIntParam = 5;
        static final int TRANSACTION_getVersion = 1;
        static final int TRANSACTION_mcuLogoUpdate = 21;
        static final int TRANSACTION_reportCmdComplete = 28;
        static final int TRANSACTION_reportDeviceCmdComplete = 31;
        static final int TRANSACTION_reportDeviceInfo = 30;
        static final int TRANSACTION_reportDeviceParamChanged = 29;
        static final int TRANSACTION_reportRRCtrlParamChange = 27;
        static final int TRANSACTION_requestRRUpdates = 2;
        static final int TRANSACTION_screenMute = 9;
        static final int TRANSACTION_sendADValue = 26;
        static final int TRANSACTION_sendDeviceMsg = 18;
        static final int TRANSACTION_sendKBDData = 24;
        static final int TRANSACTION_sendKey = 25;
        static final int TRANSACTION_sendOSDButton = 23;
        static final int TRANSACTION_sendPCStatus = 19;
        static final int TRANSACTION_sendStringCommand = 20;
        static final int TRANSACTION_sendTouchMsg = 22;
        static final int TRANSACTION_setDeviceIntParam = 16;
        static final int TRANSACTION_setDeviceParam = 17;
        static final int TRANSACTION_setIntParam = 4;
        static final int TRANSACTION_switchAudioOut = 6;
        static final int TRANSACTION_switchScreenOut = 7;
        static final int TRANSACTION_unrequestRRUpdates = 3;
        
        public Stub() {
            this.attachInterface((IInterface)this, "android.rrctrl.IRRCtrlManager");
        }
        
        public static IRRCtrlManager asInterface(final IBinder binder) {
            if (binder == null) {
                return null;
            }
            final IInterface queryLocalInterface = binder.queryLocalInterface("android.rrctrl.IRRCtrlManager");
            if (queryLocalInterface != null && queryLocalInterface instanceof IRRCtrlManager) {
                return (IRRCtrlManager)queryLocalInterface;
            }
            return new Proxy(binder);
        }
        
        public IBinder asBinder() {
            return (IBinder)this;
        }
        
        public boolean onTransact(int n, final Parcel parcel, final Parcel parcel2, final int n2) throws RemoteException {
            switch (n) {
                default: {
                    return super.onTransact(n, parcel, parcel2, n2);
                }
                case 1598968902: {
                    parcel2.writeString("android.rrctrl.IRRCtrlManager");
                    return true;
                }
                case 1: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    final String version = this.getVersion();
                    parcel2.writeNoException();
                    parcel2.writeString(version);
                    return true;
                }
                case 2: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    this.requestRRUpdates(parcel.readInt(), IRRCtrlListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                }
                case 3: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    this.unrequestRRUpdates(parcel.readInt(), IRRCtrlListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                }
                case 4: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.setIntParam(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 5: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.getIntParam(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 6: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.switchAudioOut(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 7: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.switchScreenOut(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 8: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.audioMute(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 9: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.screenMute(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 10: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.deviceOpen(parcel.readInt(), parcel.readInt(), parcel.readInt(), IRRCtrlDeviceListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 11: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.deviceClose(parcel.readInt(), IRRCtrlDeviceListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 12: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.deviceControl(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 13: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.deviceControlExt(parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 14: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.getDeviceIntParam(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 15: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    final byte[] deviceParam = this.getDeviceParam(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(deviceParam);
                    return true;
                }
                case 16: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.setDeviceIntParam(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 17: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.setDeviceParam(parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 18: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.sendDeviceMsg(parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 19: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.sendPCStatus(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 20: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.sendStringCommand(parcel.readInt(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 21: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.mcuLogoUpdate(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 22: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    this.sendTouchMsg(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                }
                case 23: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    this.sendOSDButton(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                }
                case 24: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    this.sendKBDData(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                }
                case 25: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    this.sendKey(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                }
                case 26: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    this.sendADValue(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                }
                case 27: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.reportRRCtrlParamChange(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 28: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.reportCmdComplete(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 29: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.reportDeviceParamChanged(parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 30: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.reportDeviceInfo(parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 31: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlManager");
                    n = this.reportDeviceCmdComplete(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
            }
        }
        
        private static class Proxy implements IRRCtrlManager
        {
            private IBinder mRemote;
            
            Proxy(final IBinder mRemote) {
                this.mRemote = mRemote;
            }
            
            public IBinder asBinder() {
                return this.mRemote;
            }
            
            @Override
            public int audioMute(int int1) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    this.mRemote.transact(8, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int deviceClose(int int1, final IRRCtrlDeviceListener irrCtrlDeviceListener) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    IBinder binder;
                    if (irrCtrlDeviceListener != null) {
                        binder = irrCtrlDeviceListener.asBinder();
                    }
                    else {
                        binder = null;
                    }
                    obtain.writeStrongBinder(binder);
                    this.mRemote.transact(11, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int deviceControl(int int1, final int n, final int n2, final int n3) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeInt(n3);
                    this.mRemote.transact(12, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int deviceControlExt(int int1, final int n, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(13, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int deviceOpen(int int1, final int n, final int n2, final IRRCtrlDeviceListener irrCtrlDeviceListener) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    IBinder binder;
                    if (irrCtrlDeviceListener != null) {
                        binder = irrCtrlDeviceListener.asBinder();
                    }
                    else {
                        binder = null;
                    }
                    obtain.writeStrongBinder(binder);
                    this.mRemote.transact(10, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int getDeviceIntParam(int int1, final int n) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    this.mRemote.transact(14, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public byte[] getDeviceParam(final int n, final int n2) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    this.mRemote.transact(15, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.createByteArray();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int getIntParam(int int1) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            public String getInterfaceDescriptor() {
                return "android.rrctrl.IRRCtrlManager";
            }
            
            @Override
            public String getVersion() throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readString();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int mcuLogoUpdate(final String s, int int1, final int n, final int n2, final int n3) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeString(s);
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeInt(n3);
                    this.mRemote.transact(21, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int reportCmdComplete(int int1, final int n, final int n2, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(28, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int reportDeviceCmdComplete(int int1, final int n, final int n2, final int n3, final int n4, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeInt(n3);
                    obtain.writeInt(n4);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(31, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int reportDeviceInfo(int int1, final int n, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(30, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int reportDeviceParamChanged(int int1, final int n, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(29, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int reportRRCtrlParamChange(int int1, final int n, final int n2, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(27, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public void requestRRUpdates(final int n, final IRRCtrlListener irrCtrlListener) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    IBinder binder;
                    if (irrCtrlListener != null) {
                        binder = irrCtrlListener.asBinder();
                    }
                    else {
                        binder = null;
                    }
                    obtain.writeStrongBinder(binder);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int screenMute(int int1) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    this.mRemote.transact(9, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public void sendADValue(final int n, final int n2) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    this.mRemote.transact(26, obtain, obtain2, 0);
                    obtain2.readException();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int sendDeviceMsg(int int1, final int n, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(18, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public void sendKBDData(final int n, final int n2, final int n3) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeInt(n3);
                    this.mRemote.transact(24, obtain, obtain2, 0);
                    obtain2.readException();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public void sendKey(final int n, final int n2, final int n3) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeInt(n3);
                    this.mRemote.transact(25, obtain, obtain2, 0);
                    obtain2.readException();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public void sendOSDButton(final int n, final int n2) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    this.mRemote.transact(23, obtain, obtain2, 0);
                    obtain2.readException();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int sendPCStatus(int int1) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    this.mRemote.transact(19, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int sendStringCommand(int int1, final String s, final String s2) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeString(s);
                    obtain.writeString(s2);
                    this.mRemote.transact(20, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public void sendTouchMsg(final int n, final int n2, final int n3, final int n4) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeInt(n3);
                    obtain.writeInt(n4);
                    this.mRemote.transact(22, obtain, obtain2, 0);
                    obtain2.readException();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int setDeviceIntParam(int int1, final int n, final int n2) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    this.mRemote.transact(16, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int setDeviceParam(int int1, final int n, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(17, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int setIntParam(int int1, final int n) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int switchAudioOut(int int1, final int n) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public int switchScreenOut(int int1, final int n) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
            
            @Override
            public void unrequestRRUpdates(final int n, final IRRCtrlListener irrCtrlListener) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlManager");
                    obtain.writeInt(n);
                    IBinder binder;
                    if (irrCtrlListener != null) {
                        binder = irrCtrlListener.asBinder();
                    }
                    else {
                        binder = null;
                    }
                    obtain.writeStrongBinder(binder);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}
