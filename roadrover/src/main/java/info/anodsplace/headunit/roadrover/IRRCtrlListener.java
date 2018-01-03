package info.anodsplace.headunit.roadrover;

import android.os.*;

public interface IRRCtrlListener extends IInterface
{
    int onCmdComplete(final int p0, final int p1, final int p2, final byte[] p3) throws RemoteException;
    
    int onRRCtrlParamChange(final int p0, final int p1, final int p2, final byte[] p3) throws RemoteException;
    
    public abstract static class Stub extends Binder implements IRRCtrlListener
    {
        private static final String DESCRIPTOR = "android.rrctrl.IRRCtrlListener";
        static final int TRANSACTION_onCmdComplete = 2;
        static final int TRANSACTION_onRRCtrlParamChange = 1;
        
        public Stub() {
            this.attachInterface((IInterface)this, "android.rrctrl.IRRCtrlListener");
        }
        
        public static IRRCtrlListener asInterface(final IBinder binder) {
            if (binder == null) {
                return null;
            }
            final IInterface queryLocalInterface = binder.queryLocalInterface("android.rrctrl.IRRCtrlListener");
            if (queryLocalInterface != null && queryLocalInterface instanceof IRRCtrlListener) {
                return (IRRCtrlListener)queryLocalInterface;
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
                    parcel2.writeString("android.rrctrl.IRRCtrlListener");
                    return true;
                }
                case 1: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlListener");
                    n = this.onRRCtrlParamChange(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
                case 2: {
                    parcel.enforceInterface("android.rrctrl.IRRCtrlListener");
                    n = this.onCmdComplete(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(n);
                    return true;
                }
            }
        }
        
        private static class Proxy implements IRRCtrlListener
        {
            private IBinder mRemote;
            
            Proxy(final IBinder mRemote) {
                this.mRemote = mRemote;
            }
            
            public IBinder asBinder() {
                return this.mRemote;
            }
            
            public String getInterfaceDescriptor() {
                return "android.rrctrl.IRRCtrlListener";
            }
            
            @Override
            public int onCmdComplete(int int1, final int n, final int n2, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlListener");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
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
            public int onRRCtrlParamChange(int int1, final int n, final int n2, final byte[] array) throws RemoteException {
                final Parcel obtain = Parcel.obtain();
                final Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.rrctrl.IRRCtrlListener");
                    obtain.writeInt(int1);
                    obtain.writeInt(n);
                    obtain.writeInt(n2);
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
