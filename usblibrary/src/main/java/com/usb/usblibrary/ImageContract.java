package com.usb.usblibrary;

import android.content.Context;
import android.graphics.Bitmap;

public interface ImageContract {
    interface CallBack {
        void onResult(String tag, String msg);
    }
    interface ITE_A {
         void onResult(String tag, String msg);
         boolean openTE_A(Context context, int hnd_dev);

        void closeTE();

        boolean isConnected();

        int recvImage(short[] buf, boolean agc);

        int recvImage(int[] buf, boolean agc);

        int getImageHeight();

        int getImageWidth();

        float calcTemp(short x, short y);

        void calcTemp(float[] buf);

        boolean shutterCalibrationOn();

        int getID();

        boolean setFocusing(boolean size, boolean range);

        int cameraPower(boolean enable);

        boolean connectToPC();
    }

    interface IImagePresenter {
        boolean openTE_A(Context context, int index) throws InterruptedException;

        void closeTE();

        boolean isConnected();

        int recvImage(short[] buf, boolean agc) throws InterruptedException;

        int recvImage(int[] buf, boolean agc);

        int getImageHeight();

        int getImageWidth();

        float calcTemp(short x, short y);

        void calcTemp(float[] buf);

        boolean imageCorrection(final int aveCount, final int steeringEngineDelay) throws InterruptedException;

        int getID();

        boolean setFocusing(boolean size, boolean range);
        int cameraPower(boolean enable);
        boolean connectToPC();
        Bitmap createImage(short[] buf);
        int[]  getFrequency();
        void removeImageCorrect();
    }

    interface IImageModel {
        boolean isConnected();

        boolean openUsbDevice(Context context, int index, ImageContract.CallBack callBack);

        void close();

        boolean ControlTransfer(int requestType, int request, int value,
                                int index, byte[] buffer, int length, int timeout);

        int bulkTransfer(int endpoint, byte[] buffer, final int length,
                         int timeout);

        boolean restartDetector();

        boolean readConfig(CySystemConfig sysConfig);

        boolean readNonUniformCorrect(byte[] outData, int len);

        boolean steeringEngine(boolean bool) throws InterruptedException;

        boolean setFocusing(boolean size, boolean range);

        boolean cameraPower(boolean enable);

        boolean connectToPC();
    }

}
