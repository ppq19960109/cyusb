package com.usb.presenter;

import android.content.Context;
import android.graphics.Bitmap;

import com.usb.common.CommonUtils;
import com.usb.common.ImageContract;
import com.usb.common.ImageProUtils;
import com.usb.model.CySystemConfig;
import com.usb.model.ImageModel;

import java.util.Arrays;

public class ImagePresenter implements ImageContract.IImagePresenter {
    private ImageContract.IImageModel imageModel = ImageModel.getInstance();
    private CySystemConfig cySysConfig = new CySystemConfig();
    private ImageContract.ITE_A mView;

    private final int imageWidth = 384; //图像宽度
    private final int imageHight = 288;  //图像高度
    private final int imageSize = imageHight * imageWidth; //图像像素个数
    private final int imageByteLen = imageSize * 2; //图像像素的字节数，图像像素为16bit

    private UsbThread usbThread;
    private final int packSize = 13824; //数据包内容
    private final int packHead = 12;  //数据包头
    private byte[] packBuf;         //图像包缓存
    private byte[] firstFrameBuf;  //一级帧缓存
    private byte[] secondFrameBuf; //二级帧缓存
    private volatile boolean frameFlag;     //获取一帧图像标志
    private int prePackIndex; //上一个图像数据包的序号

    private short[] pixelShortSource;
    private short[] pixelShortTarget;
    private int[] pixelIntSource;
    private short[] pixelOffset;
    private short[] pixelGain;

    /**
     * 图像均衡化参数
     */
    private short[] nHist;
    private float[] pHist;
    private float[] cHist;
    private long[] PixelSum;

    /**
     * 非均匀校正标志
     */
    private volatile boolean correctionFlag;
    private volatile boolean powerFlag;

    private ImageContract.CallBack callBack = new ImageContract.CallBack() {
        @Override
        public void onResult(String tag, String msg) {
            onViewResult(tag, msg);
        }
    };

    public ImagePresenter(ImageContract.ITE_A view) {
        mView = view;

        packBuf = new byte[packHead + packSize];
        firstFrameBuf = new byte[imageByteLen];
        secondFrameBuf = new byte[imageByteLen];

        pixelShortSource = new short[imageSize];
        pixelShortTarget = new short[imageSize];
        pixelIntSource = new int[imageSize];
        pixelOffset = new short[imageSize];
        pixelGain = new short[imageSize];
        Arrays.fill(pixelGain, (short) 0x1000);

        nHist = new short[256];
        pHist = new float[256];
        cHist = new float[256];
        PixelSum = new long[imageSize];
    }

    private boolean isViewAttach() {
        return mView != null;
    }

    private ImageContract.ITE_A getView() {
        return mView == null ? null : mView;
    }

    private void onViewResult(String tag, String msg) {
        if (isViewAttach()) {
            getView().onResult(tag, msg);
        }
    }

    /**
     * 获取图像高度
     *
     * @return
     */
    @Override
    public int getImageHeight() {
        return imageHight;
    }

    /**
     * 获取图像宽度
     *
     * @return
     */
    @Override
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     * 打开图像传输
     *
     * @param context
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean openTE_A(Context context, int index) {
        if (context == null) {
            return false;
        }
        return imageModel.openUsbDevice(context, index, callBack);
    }

    /**
     * 关闭图像传输
     */
    @Override
    public void closeTE() {
        threadClose();
        imageModel.close();
        removeCalibrationOn();
    }

    /**
     * @param buf
     * @param agc
     * @return 1: Read image successfully
     * 2: Read image of size 0
     * 3: Recieved image size is not equal to requested size
     * 4: Data read fail
     */
    @Override
    public int recvImage(short[] buf, boolean agc) {
        if (!getImageFrame(pixelShortSource, 0)) {
            return 4;
        }
        ImageProUtils.setImageOffset(pixelShortSource, pixelShortTarget, pixelGain, pixelOffset);
        if (agc) {
            ImageProUtils.MedianFlitering(pixelShortTarget, buf, imageWidth, imageHight, 3);
        } else {
            System.arraycopy(pixelShortTarget, 0, buf, 0, buf.length);
        }
        return 1;
    }

    /**
     * 像素点测温
     *
     * @param x
     * @param y
     * @return
     */
    @Override
    public float calcTemp(short x, short y) {
        if (isConnected()) {
            return cySysConfig.grayToTemp(pixelShortTarget[x + y * imageWidth], false);
        }
        return 0;
    }

    /**
     * 返回图像温度信息集合
     *
     * @param buf
     */
    @Override
    public void calcTemp(float[] buf) {
        if (isConnected()) {
            cySysConfig.grayToTemp(pixelShortTarget, buf, false);
        }
    }

    public void steeringEngine(boolean bool) throws InterruptedException {
        if (bool) {
            imageModel.steeringEngine(1);

        }else {
            imageModel.steeringEngine(2);

        }
        Thread.sleep(100);
        imageModel.steeringEngine(3);
        Thread.sleep(200);
    }
    /**
     * 图像校正
     *
     * @param aveCount
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean shutterCalibrationOn(final int aveCount) throws InterruptedException {
        correctionFlag = true;
        steeringEngine(false);
        Arrays.fill(PixelSum, 0);
        for (int i = 0; i < aveCount; ++i) {
            getImageFrame(pixelShortSource, 1);
        }
        for (int i = 0; i < aveCount; ++i) {
            getImageFrame(pixelShortSource, 1);
            for (int j = 0; j < PixelSum.length; ++j) {
                PixelSum[j] += pixelShortSource[j];
            }
        }
        cySysConfig.m_swZeroGray = ImageProUtils.nonUniformCorrection(PixelSum, pixelGain, pixelOffset, aveCount);
        steeringEngine(true);
        correctionFlag = false;
        return true;
    }

    @Override
    public void removeCalibrationOn() {
        Arrays.fill(pixelOffset, (short) 0);
    }

    @Override
    public int getID() {
        return 0;
    }

    /******************************************/
    @Override
    public boolean isConnected() {
        return imageModel.isConnected();
    }

    @Override
    public boolean setFocusing(boolean size, boolean range) {
        return imageModel.setFocusing(size, range);
    }

    @Override
    public int cameraPower(boolean enable) {
        if (enable) {
            powerFlag = true;
            if (!imageModel.cameraPower(enable)) {
                return 1;
            }
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(800);
                        zeroAdjust();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    powerFlag = false;
                }
            }.start();

        } else {
            while (powerFlag == true) ;
            threadClose();
            if (!imageModel.cameraPower(enable)) {
                return 2;
            }
        }
        return 0;
    }

    @Override
    public boolean connectToPC() {
        return imageModel.connectToPC();
    }
/******************************************/
    /**
     * 图像调零
     *
     * @param
     * @return
     */
    public boolean zeroAdjust() throws InterruptedException {
        imageModel.restartDetector();
        Thread.sleep(10);
        cySysConfig.m_fZeroTemp = imageModel.readZeroTemp();
        Thread.sleep(10);
        if (imageModel.readNonUniformCorrect(secondFrameBuf, imageByteLen)) {
            ImageProUtils.getImageArray(pixelGain, secondFrameBuf);
        } else {
            onViewResult("ImagePresenter", "读取矫正数据失败");
        }
        threadInit();
        Thread.sleep(10);
        if (!imageModel.readConfig(cySysConfig)) {
            onViewResult("ImagePresenter", "读取配置数据失败");
        }
        return true;
    }

    /**
     * 图像线程初始化
     */
    public void threadInit() {
        threadClose();
        usbThread = new UsbThread();
        usbThread.start();
    }

    /**
     * 图像线程关闭
     */
    private void threadClose() {
        if (usbThread != null) {
            usbThread.interrupt();
            usbThread = null;
        }
    }

    /**
     * 获取一帧图像
     *
     * @param frame
     * @param index
     * @return
     */
    private boolean getImageFrame(short[] frame, int index) {
        if (index == 0) {
            if (frameFlag == false || correctionFlag == true) {
                return false;
            }
            frameFlag = false;
            synchronized (this) {
                ImageProUtils.getImageArray(frame, secondFrameBuf);
            }
        } else if (index == 1) {
            for (; ; ) {
                if (frameFlag == false) {
                    continue;
                }
                frameFlag = false;
                synchronized (this) {
                    ImageProUtils.getImageArray(frame, secondFrameBuf);
                }
                break;
            }
        }
        return true;
    }

    /**
     * 根据行序号（0xf000）对一帧图像中数据包的顺序进行行移位排序
     *
     * @param source
     * @param target
     * @param byteLen
     * @param width
     * @return
     */
    private boolean imageSort(byte[] source, byte[] target, int byteLen, int width) {
        int widthLen = width * 2;
        for (int i = widthLen - 1; i < byteLen; i += 2) {
            if (source[i] == (byte) 0xf0 && source[i - 1] == (byte) 0x00) {
                if (i < widthLen) {
                    return true;
                }
                int srcPos = i - (widthLen - 1);
                int desPos = byteLen - srcPos;
                System.arraycopy(source, srcPos, target, 0, desPos);
                System.arraycopy(source, 0, target, desPos, srcPos);
                return true;
            }
        }
        return false;
    }

    /**
     * 通过USB，获取图像数据包，此函数放在线程中不停执行
     *
     * @return
     */
    private boolean getImagePack() {
        CommonUtils.setFrequency(0);
        if (imageModel.bulkTransfer(0x81, packBuf, packHead + packSize, 100) < 0) {
            return false;
        }
        if (prePackIndex >= packBuf[2]) {
            synchronized (this) {
                imageSort(firstFrameBuf, secondFrameBuf, imageByteLen, imageWidth);
            }
            frameFlag = true;
        }
        if (packBuf[2] < 16 && packBuf[2] >= 0) {
            System.arraycopy(packBuf, packHead, firstFrameBuf, packBuf[2] * packSize, packSize);
        }
        prePackIndex = packBuf[2];
        return true;
    }

    /**
     * 线程不停从USB中读取图像数据
     */
    public class UsbThread extends Thread {
        @Override
        public void run() {
            super.run();
            for (; ; ) {
                if (isConnected()) {
                    getImagePack();
                }
                if (isInterrupted()) {
                    return;
                }
            }
        }
    }

    /******************************************/
    /**
     * 未使用到的函数
     */
    public Bitmap createBitmap(short[] imageSource) {
        for (int i = 0; i < imageSource.length; ++i) {
            pixelIntSource[i] = imageSource[i];
        }
        return createBitmap(pixelIntSource);
    }

    public Bitmap createBitmap(int[] imageSource) {
        CommonUtils.setFrequency(1);
        ImageProUtils.setRGBRange(imageSource, 2000, 20000);
//        ImageProUtils.setHist(imageSource, nHist, pHist, cHist);
        ImageProUtils.setArrayARGB(imageSource, true);
        return Bitmap.createBitmap(imageSource, imageWidth, imageHight,
                Bitmap.Config.ARGB_8888);
    }


}
