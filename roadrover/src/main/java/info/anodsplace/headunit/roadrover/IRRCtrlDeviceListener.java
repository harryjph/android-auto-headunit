package info.anodsplace.headunit.roadrover;

import android.os.*;

public interface IRRCtrlDeviceListener extends IInterface
{
    int onDeviceCmdComplete(final int p0, final int p1, final int p2, final int p3, final int p4, final byte[] p5) throws RemoteException;
    
    int onDeviceInfo(final int p0, final int p1, final byte[] p2) throws RemoteException;
    
    int onDeviceParamChanged(final int p0, final int p1, final byte[] p2) throws RemoteException;
    
    public abstract static class Stub extends Binder implements IRRCtrlDeviceListener
    {
        private static final String DESCRIPTOR = "android.rrctrl.IRRCtrlDeviceListener";
        static final int TRANSACTION_onDeviceCmdComplete = 3;
        static final int TRANSACTION_onDeviceInfo = 2;
        static final int TRANSACTION_onDeviceParamChanged = 1;
        
        public Stub() {
            this.attachInterface((IInterface)this, "android.rrctrl.IRRCtrlDeviceListener");
        }
        
        public static IRRCtrlDeviceListener asInterface(final IBinder binder) {
            if (binder == null) {
                return null;
            }
            final IInterface queryLocalInterface = binder.queryLocalInterface("android.rrctrl.IRRCtrlDeviceListener");
            if (queryLocalInterface != null && queryLocalInterface instanceof IRRCtrlDeviceListener) {
                return (IRRCtrlDeviceListener)queryLocalInterface;
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
                    parcel2.writeString("android.rrctrl.IRRCtrlDeviceListener");
                    return true;
                }
                case 1: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlDeviceListener");
                    n = this.onDeviceParamChanged(parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 2: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlDeviceListener");
                    n = this.onDeviceInfo(parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 3: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlDeviceListener");
                    n = this.onDeviceCmdComplete(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
            }
        }
        
        private static class Proxy implements IRRCtrlDeviceListener
        {
            private IBinder mRemote;
            
            Proxy(final IBinder mRemote) {
                this.mRemote = mRemote;
            }
            
            public IBinder asBinder() {
                return this.mRemote;
            }
            
            public String getInterfaceDescriptor() {
                return "android.rrctrl.IRRCtrlDeviceListener";
            }
            
            @Override
            public int onDeviceCmdComplete(int int1, final int n, final int n2, final int n3, final int n4, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlDeviceListener");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
                    obtain.writeInt(n3);
                    obtain.writeInt(n4);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(3, obtain, obtain2, 0);
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
            public int onDeviceInfo(int int1, final int n, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlDeviceListener");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(2, obtain, obtain2, 0);
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
            public int onDeviceParamChanged(int int1, final int n, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlDeviceListener");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeByteArray(array);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    int1 = obtain2.readInt();
                    return int1;
                }
                finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}
