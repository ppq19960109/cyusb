package com.usb.model;

import android.content.Context;

import com.usb.common.ImageContract;

public class ImageModel implements ImageContract.IImageModel {
    private static ImageModel Instance = new ImageModel();

    private final int
            DIR_TO_DEVICE = 0,
            DIR_FROM_DEVICE = 1 << 7;
    private final int
            REQ_STD = 0,
            REQ_CLASS = 1 << 5,
            REQ_VENDOR = 1 << 6;
    private final int
            TGT_DEVICE = 0,
            TGT_INTFC = 1 << 0,
            TGT_ENDPT = 1 << 1,
            TGT_OTHER = 3;

    private final int USBCONWRITE = DIR_TO_DEVICE | REQ_VENDOR | TGT_DEVICE;
    private final int USBCONREAD = DIR_FROM_DEVICE | REQ_VENDOR | TGT_DEVICE;
    private final int BULKMAXLEN = 16384;
    private final int CONTROLTIMEOUT = 500;
    private UsbDeviceUtils usbDevice;
    String[] usbConnnectInfo = {
            "USB成功连接",
            "USB已经连接",
            "USB设备不存在",
            "USB权限请求中",
            "usbConnection为空",
            "USB claim Interface失败",
            "USB权限不存在",
    };
    String[] controlTransferErr = {
            "USB设备未连接",
            "Control Transfer失败",
    };
    String[] bulkTransferErr = {
            "USB设备未连接",
            "端点不存在",
            "端点方向不正确",
            "Bulk Transfer失败"
    };

    private ImageModel() {
        usbDevice = UsbDeviceUtils.getInstance();
    }

    public static ImageModel getInstance() {
        return Instance;
    }

    @Override
    public boolean isConnected() {
        return usbDevice.isUsbConnected();
    }

    @Override
    public boolean openUsbDevice(Context context, int index, ImageContract.CallBack callBack) {
        int err = usbDevice.openUsbDevice(context, 0x4b4, 0xf1);
        if (callBack != null) {
            callBack.onResult("ImageModel", usbConnnectInfo[err]);
        }
        return err == 0 ? true : false;
    }

    @Override
    public void close() {
        usbDevice.close();
    }

    @Override
    public boolean ControlTransfer(int requestType, int request, int value,
                                   int index, byte[] buffer, int length, int timeout) {
        int err = usbDevice.controlTransfer(requestType, request, value, index, buffer, length, timeout);

        return err == 0 ? true : false;
    }

    @Override
    public int bulkTransfer(int endpoint, byte[] buffer, final int length,
                            int timeout) {
        if (length < BULKMAXLEN) {
            return usbDevice.bulkTransfer(endpoint, buffer, length, timeout);
        } else {
            int transferLen = 0, len = 0;
            byte[] bulkBuf = new byte[BULKMAXLEN];
            while (transferLen < length) {
                if (transferLen + BULKMAXLEN > length) {
                    if ((len = usbDevice.bulkTransfer(endpoint, bulkBuf, length - transferLen, timeout)) != length - transferLen) {
                        return transferLen;
                    }
                } else {
                    if ((len = usbDevice.bulkTransfer(endpoint, bulkBuf, BULKMAXLEN, timeout)) != BULKMAXLEN) {
                        return transferLen;
                    }
                }
                if ((endpoint & 0x80) == 0x80) {
                    System.arraycopy(bulkBuf, 0, buffer, transferLen, len);
                }
                transferLen += len;
            }
            return transferLen;
        }
    }

    /**
     * 重启探测器
     *
     * @return
     */
    @Override
    public boolean restartDetector() {
        byte[] buf = new byte[4];
        int cmd = usbCmdToData(0x30, 0, 0);
        if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean readConfig(CySystemConfig sysConfig) {
        if (sysConfig == null) {
            return false;
        }
        int cmd, len = CySystemConfig.SIZE;
        byte[] buf = new byte[len];
        cmd = usbCmdToData(0x18, 0, 0);
        if (!ControlTransfer(USBCONREAD, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, len, CONTROLTIMEOUT)) {
            return false;
        }

        return sysConfig.byteToConfig(buf);
    }
    @Override
    public float readZeroTemp() {
        int cmd, len = 4;
        byte[] buf = new byte[4];
        cmd = usbCmdToData(0x12, 0, 0);
        if (!ControlTransfer(USBCONREAD, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, len, CONTROLTIMEOUT)) {
            return 0;
        }
        int temp=(buf[0]&0xff)|((buf[1]&0xff)<<8)|((buf[2]&0xff)<<16)|((buf[3]&0xff)<<24);

        float ftemp =(float)((temp&0xffff)*0.0625);
        return ftemp;
    }

    @Override
    public boolean readNonUniformCorrect(byte[] outData, int len) {
        int cmd, transLen;
        byte[] buf = new byte[4];

        cmd = usbCmdToData(0x16, len >> 20, len);
        if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
            return false;
        }

        if ((transLen = bulkTransfer(0x82, outData, len, 5000)) < 0) {
            return false;
        }
        cmd = usbCmdToData(0x21, 0, 0);
        if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
            return false;
        }
        if (transLen != len) {
            return false;
        }
        return true;
    }

    /**
     * 控制舵机打开与关闭
     *
     * @param bool
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean steeringEngine(boolean bool) throws InterruptedException {
        int cmd;
        byte[] buf = new byte[4];
        if (bool) {
            cmd = usbCmdToData(0x0b, 0, 2);
        } else {
            cmd = usbCmdToData(0x0b, 0, 1);
        }
        if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
            return false;
        }

        Thread.sleep(100);

        cmd = usbCmdToData(0x0b, 0, 3);
        if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
            return false;
        }
        return true;
    }

    public boolean controlTransferWriteCmd(int cmd) {
        byte[] buf = new byte[4];
        if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean setFocusing(boolean size, boolean range) {
        int cmd;
        byte[] buf = new byte[4];
        if (size) {
            if (range) {
                cmd = usbCmdToData(0x13, 0, 2);
            } else {
                cmd = usbCmdToData(0x13, 0, 1);
            }
        } else {
            if (range) {
                cmd = usbCmdToData(0x13, 0, 6);
            } else {
                cmd = usbCmdToData(0x13, 0, 5);
            }
        }
        if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean cameraPower(boolean enable) {
        int cmd;
        byte[] buf = new byte[4];
        if (enable) {
            cmd = usbCmdToData(0xAA, 0, 1);
            if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
                return false;
            }
        } else {
            cmd = usbCmdToData(0xAA, 0, 0);
            if (!ControlTransfer(USBCONWRITE, 0x05, (cmd >> 16) & 0xffff, cmd & 0xffff, buf, 4, CONTROLTIMEOUT)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean connectToPC() {
        return false;
    }

    /**
     * 按照指定格式转化成发送给CYUSB的数据，用于控制传输
     *
     * @param CMD
     * @param Param1
     * @param Param2
     * @return
     */
    private int usbCmdToData(int CMD, int Param1, int Param2) {
        return ((CMD & 0xff) << 24) | ((Param1 & 0x0f) << 20) | (Param2 & 0xfffff);
    }

}

