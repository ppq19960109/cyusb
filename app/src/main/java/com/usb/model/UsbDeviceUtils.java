package com.usb.model;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.HashMap;
import java.util.Iterator;

public class UsbDeviceUtils {
    private static UsbDeviceUtils Instance = new UsbDeviceUtils();

    private boolean usbConnected;
    private PendingIntent mPermissionIntent;

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private UsbDeviceConnection usbConnection;
    private UsbEndpoint[] usbEndpointIn = new UsbEndpoint[16];
    private UsbEndpoint[] usbEndpointOut = new UsbEndpoint[16];

    private UsbDeviceUtils() {

    }

    public static UsbDeviceUtils getInstance() {
        return Instance;
    }

    public boolean isUsbConnected() {
        return usbConnected && (usbConnection != null);
    }

    public void setUsbConnected(boolean usbConnected) {
        this.usbConnected = usbConnected;
    }

    /**
     * 关闭USB传输
     */
    public void close() {
        setUsbConnected(false);
        if (usbConnection != null) {
            usbConnection.releaseInterface(usbInterface);
            usbConnection.close();
            usbConnection = null;
        }
    }

    /**
     * 获取指定USB设备
     *
     * @param context
     * @param vendorId
     * @param productId
     * @return
     */
    private UsbDevice getUsbDevice(Context context, int vendorId, int productId) {
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            if (device.getVendorId() == vendorId
                    && device.getProductId() == productId) {
                return device;
            }
        }
        return null;
    }

    /**
     * 请求USB权限
     *
     * @param context
     * @return
     */
    private boolean requestPermission(Context context) {
        if (usbDevice == null) {
            return false;
        }
        if (!usbManager.hasPermission(usbDevice)) {
            if (mPermissionIntent == null) {
                Intent intent = new Intent(UsbReceiver.ACTION_USB_PERMISSION);
                intent.setComponent(new ComponentName(context,
                        UsbReceiver.CLS_NAME));
                mPermissionIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

                if (mPermissionIntent != null) {
                    usbManager.requestPermission(usbDevice, mPermissionIntent);
                }
            } else {
                usbManager.requestPermission(usbDevice, mPermissionIntent);
            }
            return false;
        }
        return true;
    }

    /**
     * 打开USB端口端点
     *
     * @param context
     * @param index
     * @return
     */
    public int openUsbPort(Context context, int index) {
        usbInterface = usbDevice.getInterface(index);

        if (usbManager.hasPermission(usbDevice)) {
            if ((usbConnection = usbManager.openDevice(usbDevice)) == null) {
                return 4;
            }
            if (!usbConnection.claimInterface(usbInterface, true)) {
                usbConnection.close();
                return 5;
            }
        } else {
            return 6;
        }

        for (int i = 0; i < usbInterface.getEndpointCount(); ++i) {
            UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
            if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                usbEndpointIn[usbEndpoint.getAddress() & 0x7f] = usbEndpoint;
            } else {
                usbEndpointOut[usbEndpoint.getAddress()] = usbEndpoint;
            }
        }
        setUsbConnected(true);
        return 0;
    }

    /**
     * 打开指定USB设备，并获取端点
     *
     * @param context
     * @param vendorId
     * @param productId
     * @return
     */
    public int openUsbDevice(Context context, int vendorId, int productId) {
        if (isUsbConnected()) {
            return 0;
        }
        if ((usbDevice = getUsbDevice(context, vendorId, productId)) == null) {
            return 2;
        }
        if (!requestPermission(context)) {
            return 3;
        }
        return openUsbPort(context, 0);
    }


    /**
     * USB端点0控制传输
     *
     * @param requestType
     * @param request
     * @param value
     * @param index
     * @param buffer
     * @param length
     * @param timeout
     * @return
     */
    public int controlTransfer(int requestType, int request, int value,
                               int index, byte[] buffer, int length, int timeout) {
        if (!isUsbConnected()) {
            return 1;
        }
        int ret = usbConnection.controlTransfer(requestType, request, value,
                index, buffer, length, timeout);
        if (ret < 0) {
            return 2;
        }
        return 0;
    }

    /**
     * USB批量传输
     *
     * @param endpoint
     * @param buffer
     * @param length
     * @param timeout
     * @return
     */
    public int bulkTransfer(int endpoint, byte[] buffer, int length,
                            int timeout) {
        UsbEndpoint Endpoint;
        if (!isUsbConnected()) {
            return -1;
        }
        if (endpoint > 0x80) {
            Endpoint = usbEndpointIn[endpoint & 0x7f];
        } else {
            Endpoint = usbEndpointOut[endpoint];
        }
        if (Endpoint == null) {
            return -2;
        }
        if (Endpoint.getAddress() != endpoint) {
            return -3;
        }

        int transferLen = usbConnection.bulkTransfer(Endpoint, buffer, length, timeout);
        if (transferLen < 0) {
            return -4;
        }

        return transferLen;
    }


    /**
     * 未使用到的函数
     */
    public String getUsbDeviceList(Context context) {
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        StringBuffer strBuf = new StringBuffer();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            strBuf.append("VID:" + device.getVendorId() + " PID:" + device.getProductId());
        }
        return strBuf.toString();
    }

    public Intent registerReceiver(Context context, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbReceiver.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        return context.registerReceiver(receiver, filter);
    }

    public void unregisterReceiver(Context context, BroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
    }

}

