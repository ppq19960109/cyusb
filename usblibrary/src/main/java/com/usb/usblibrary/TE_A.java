package com.usb.usblibrary;

import android.content.Context;
import android.graphics.Bitmap;

public class TE_A implements ImageContract.ITE_A {

    private ImageContract.IImagePresenter imagePresenter;

    public TE_A() {
        imagePresenter = new ImagePresenter(this);
    }

    //摄像头初始化
//    @Override
    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
//        if (context instanceof Activity) {
//            CommonUtils.isGrantExternalRW((Activity) context, 0);
//        }
        CommonUtils.debugContext = context;

    }

    //连接摄像头
    @Override
    public boolean openTE_A(Context context, int hnd_dev) {
        return imagePresenter.openTE_A(context, hnd_dev);
    }

    //关闭摄像头连接
    @Override
    public void closeTE() {
        imagePresenter.closeTE();
    }

    //判断摄像头是否连接
    @Override
    public boolean isConnected() {
        return imagePresenter.isConnected();
    }

    //返回图像灰度数据集合
    @Override
    public int recvImage(short[] buf, boolean agc) {
        return imagePresenter.recvImage(buf, agc);
    }

    @Override
    public int getImageHeight() {
        return imagePresenter.getImageHeight();
    }

    @Override
    public int getImageWidth() {
        return imagePresenter.getImageWidth();
    }

    //点测温
    @Override
    public float calcTemp(short x, short y) {
        return imagePresenter.calcTemp(x, y);
    }

    //返回图像温度信息集合
    @Override
    public void calcTemp(float[] buf) {
        imagePresenter.calcTemp(buf);
    }

    //矫正
    @Override
    public boolean shutterCalibrationOn() {
        try {
            return imagePresenter.shutterCalibrationOn(8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void removeCalibrationOn() {
        imagePresenter.removeCalibrationOn();
    }
    //获取摄像头编号
    @Override
    public int getID() {
        return imagePresenter.getID();
    }

    /**
     * 摄像头调焦
     *
     * @param size  true:粗调
     *              false:细调
     * @param range true:调近
     *              false:调远
     * @return true：成功
     * false:失败
     */
    @Override
    public boolean setFocusing(boolean size, boolean range) {
        return imagePresenter.setFocusing(size, range);
    }

    /**
     * 打开或关闭摄像机电源,在打开摄像机电源前需要连接USB。
     *
     * @param enable true:打开摄像机电源
     *               false:关闭摄像机电源
     * @return 0：成功
     * 1：USB未连接
     * 2：打开摄像机电源错误
     * 3：关闭摄像机电源错误
     */
    @Override
    public int cameraPower(boolean enable) {
        return imagePresenter.cameraPower(enable);
      }

    /**
     * 断开与android连接，连接PC，
     *
     * @return true：成功
     * false:失败
     */
    @Override
    public boolean connectToPC() {
        return imagePresenter.connectToPC();
    }

    public Bitmap createBitmap(short[] imageSource) {
        return imagePresenter.createBitmap(imageSource);
    }


    public String getFrequency() {
        int[] frequency = CommonUtils.getFrequency();
        return "数据包频:" + frequency[0] + "HZ " + "图像帧频:" + frequency[1] + "HZ";
    }

    @Override
    public void onResult(String tag, String msg) {
        CommonUtils.showToastMsg(null, tag + ":" + msg);
    }

}
