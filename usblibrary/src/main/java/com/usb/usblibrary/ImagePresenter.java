package com.usb.usblibrary;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.Arrays;

public class ImagePresenter implements ImageContract.IImagePresenter {
    private ImageContract.IImageModel imageModel= ImageModel.getInstance();;
    private CySystemConfig cySysConfig= new CySystemConfig();
    private ImageContract.ITE_A imageView;

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

    private short[] pixelSbuf;
    private int[] pixelBuf;
    private short[] pixelOffset;
    private short[] pixelGain;

    private short[] nHist;
    private float[] pHist;
    private float[] cHist;
    private long[] PixelSum;

    /**
     * 非均匀校正标志
     */
    private volatile boolean correctionFlag;

    private long preTimeMillis;
    private int[] frequency=new int[2];

    private float[] tempCollect;  //温度集合

    private ImageContract.CallBack callBack=new ImageContract.CallBack() {
        @Override
        public void onResult(String tag, String msg) {
            if(isViewAttach()){
                getView().onResult(tag,msg);
            }
        }
    };

    public ImagePresenter( ImageContract.ITE_A view) {
        imageView=view;

        packBuf = new byte[packHead + packSize];
        firstFrameBuf = new byte[imageByteLen];
        secondFrameBuf = new byte[imageByteLen];

        pixelSbuf = new short[imageSize];
        pixelBuf = new int[imageSize];
        pixelOffset = new short[imageSize];
        pixelGain = new short[imageSize];
        Arrays.fill(pixelGain, (short) 0x1000);
        tempCollect = new float[imageSize];

        nHist = new short[256];
        pHist = new float[256];
        cHist = new float[256];
        PixelSum = new long[imageSize];
    }
    private boolean isViewAttach() {
        return imageView!=null;
    }
    private ImageContract.ITE_A getView() {
        return imageView==null?null:imageView;
    }
    /**
     * 获取图像高度
     * @return
     */
    @Override
    public int getImageHeight() {
        return imageHight;
    }

    /**
     * 获取图像宽度
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
    public boolean openTE_A(Context context, int index) throws InterruptedException {
        if (context == null) {
            return false;
        }
        if (imageModel.openUsbDevice(context, index,callBack)) {
            return imageZeroAdjust();
        }
        return false;
    }

    /**
     * 关闭图像传输
     */
    @Override
    public void closeTE() {
        threadClose();
        removeImageCorrect();
        imageModel.close();
    }

    /**
     * @param buf
     * @param agc
     * @return 1: Read image successfully
     * 2: Read image of size 0
     * 3: Recieved image size is not equal to requested size
     * 4: Data read fail
     * @throws InterruptedException
     */
    @Override
    public int recvImage(short[] buf, boolean agc) throws InterruptedException {

        if (!isConnected() || !getImageFrame(pixelSbuf, 0)) {
            return 4;
        }
        grayToTemp(cySysConfig,pixelSbuf, true);
        ImageProUtils.setImageOffset(pixelSbuf,  pixelGain, pixelOffset);
        ImageProUtils.MedianFlitering(pixelSbuf, buf, imageWidth, imageHight, 3);

        ImageProUtils.setRGBRange(buf, 2000, 20000);

//        GaussianFilter(pixelsBuf,buf,imageWidth,imageHight,9,1.2);
//        AverFiltering(pixelsBuf,buf,imageWidth,imageHight);

        if (agc) {
//            setContrast(buf, 1f, 0);
            ImageProUtils.setHist(buf, nHist, pHist, cHist);
        }
        return 1;
    }

    @Override
    public int recvImage(int[] buf, boolean agc) {
        if (!isConnected() || !getImageFrame(buf, 0)) {
            return 4;
        }
        ImageProUtils.setImageOffset(buf, pixelGain, pixelOffset);
        ImageProUtils.setRGBRange(buf, 2000, 20000);
        if (agc) {
//            setContrast(buf, 1f, 0);
            ImageProUtils.setHist(buf, nHist, pHist, cHist);
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
        if (!isConnected()) {
            return 0;
        }
        return tempCollect[x + y * imageWidth];
    }

    /**
     * 返回图像温度信息集合
     *
     * @param buf
     */
    @Override
    public void calcTemp(float[] buf) {
        if (!isConnected()) {
            Arrays.fill(tempCollect, 0);
        }
        System.arraycopy(tempCollect, 0, buf, 0, buf.length);
    }

    /**
     * 图像校正
     *
     * @param aveCount
     * @param steeringEngineDelay
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean imageCorrection(final int aveCount, final int steeringEngineDelay) throws InterruptedException {

        imageModel.steeringEngine(false);
        correctionFlag = true;

        Thread.sleep(steeringEngineDelay);

            for (int i = 0; i < 4; ++i) {
                getImageFrame(pixelBuf, 1);
            }
            for (int i = 0; i < aveCount; ++i) {
                getImageFrame(pixelBuf, 1);
                for (int j = 0; j < PixelSum.length; ++j) {
                    PixelSum[j] += pixelBuf[j];
                }
            }
            ImageProUtils.NonUniformCorrection(PixelSum, pixelGain, pixelOffset, aveCount);

        Arrays.fill(PixelSum, 0);
        imageModel.steeringEngine(true);
        Thread.sleep(steeringEngineDelay);
        correctionFlag = false;
        return true;
    }

    @Override
    public int getID() {
        return 0;
    }

/******************************************/

    /**
     * 图像线程初始化
     */
    private void threadInit() {
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
     * 图像调零
     *
     * @param
     * @return
     * @throws InterruptedException
     */
    public boolean imageZeroAdjust() throws InterruptedException {
        imageModel.restartDetector();
        if ( !imageModel.readNonUniformCorrect(secondFrameBuf, imageByteLen)) {
            CommonUtils.showToastMsg(null, "readNonUniformCorrect");
        }

//        ImageProUtils.getImageArray(pixelGain, secondFrameBuf);
        if (!imageModel.readConfig(cySysConfig)) {
            CommonUtils.showToastMsg(null, "readConfig");
        }

        return true;
    }

    private void grayToTemp(CySystemConfig systemConfig,short[] buf, boolean bool) {
        if (bool) {
            for (int i = 0; i < imageSize; ++i) {
                tempCollect[i] = (buf[i] - systemConfig.m_swZeroGray) * systemConfig.m_TempSlop + systemConfig.m_fZeroTemp + systemConfig.m_TempOffset;
            }
        } else {
            for (int i = 0; i < imageSize; ++i) {
                float temp = 36 - systemConfig.m_fZeroTemp; //外置黑体温度（36°）与挡片温度差
                float outBlackGray = systemConfig.m_VtempSlop * temp + systemConfig.m_fVtempToGray * temp * temp + systemConfig.m_VtempOffset; //计算外置黑体灰度
                outBlackGray += systemConfig.m_swZeroGray;
                temp = systemConfig.m_fZeroTemp * systemConfig.m_VtempSlop + systemConfig.m_TempOffset; //根据挡片计算不同环境温度下的温度修正系数
                tempCollect[i] = (buf[i] - outBlackGray) * temp + systemConfig.m_fHum + 36; //计算目标温度值
            }
        }
    }

    public void removeImageCorrect() {
        Arrays.fill(pixelOffset, (short) 0);
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
        } else if (index == 2) {
            getFrameData();
            ImageProUtils.getImageArray(frame, firstFrameBuf);
        }
        return true;
    }

    private boolean getImageFrame(int[] frame, int index) {
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
        } else if (index == 2) {
            getFrameData();
            ImageProUtils.getImageArray(frame, firstFrameBuf);
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
     * @param bool
     * @return
     */
    private boolean getImagePack(boolean bool) {
        ++frequency[0];
        if (bool) {
            getFrameData();
            synchronized (this) {
                System.arraycopy(firstFrameBuf, 0, secondFrameBuf, 0, secondFrameBuf.length);
            }
            frameFlag = true;
        } else {
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
        }
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
                    getImagePack(false);
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
    public Bitmap createImage(short[] imageSource) {
        ++frequency[1] ;
        for (int i = 0; i < pixelBuf.length; ++i) {
            pixelBuf[i] = imageSource[i];
        }
        ImageProUtils.setArrayARGB(pixelBuf, true);
        Bitmap bitmap = Bitmap.createBitmap(pixelBuf, imageWidth, imageHight,
                Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    public Bitmap createImage(int[] imageSource) {
        ImageProUtils.setArrayARGB(imageSource, false);
        Bitmap bitmap = Bitmap.createBitmap(imageSource, imageWidth, imageHight,
                Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    public int[] getFrequency() {
        int[] fre=new int[2];
        long curTimeMillis = System.currentTimeMillis();
        int intervaTime = (int) (curTimeMillis - preTimeMillis);
        preTimeMillis = curTimeMillis;

        fre[0] = frequency[0] * 1000 / 16 / intervaTime;
        fre[1] = frequency[1] * 1000 / intervaTime;
        frequency[0] = 0;
        frequency[1] = 0;
        return fre;
    }

    public void getFrameData() {
        byte transferNum = 0;
        for (; ; ) {
            if (imageModel.bulkTransfer(0x81, packBuf, packHead + packSize, 100) < 0) {
                return;
            }
            if (packBuf[0] != (byte) 0x0c || packBuf[1] != (byte) 0x8c) {
                continue;
            }
            if (packBuf[2] != transferNum) {
                transferNum = 0;
                continue;
            }
            System.arraycopy(packBuf, 12, firstFrameBuf, packBuf[2] * packSize, packSize);

            if (++transferNum >= 16) {
                break;
            }
        }
    }
    /******************************************/
    @Override
    public boolean isConnected() {
        return imageModel.isConnected();
    }

    @Override
    public boolean setFocusing(boolean size,boolean range) {
        return imageModel.setFocusing(size,range);
    }

    @Override
    public int cameraPower(boolean enable) {
        if(enable) {
            if(!isConnected()){
                return 1;
            }
            if(!imageModel.cameraPower(enable)){
                return 2;
            }
            threadInit();
        }else {
            threadClose();
            if(!imageModel.cameraPower(enable)){
                return 3;
            }
        }
        return 0;
    }

    @Override
    public boolean connectToPC() {
        return imageModel.connectToPC();
    }


}
