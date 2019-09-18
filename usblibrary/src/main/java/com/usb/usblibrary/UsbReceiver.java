package com.usb.usblibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

public class UsbReceiver extends BroadcastReceiver {
    public static final String ACTION_USB_PERMISSION = "android.usb.action.USB_PERMISSION";
    public static final String CLS_NAME = UsbReceiver.class.getPackage().getName() + ".UsbReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        switch (action) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                CommonUtils.showToastMsg(context, "USB插入");
                ImageModel.getInstance().openUsbDevice(context, 0, null);
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                CommonUtils.showToastMsg(context, "USB拔出");
                ImageModel.getInstance().close();
                break;
            case ACTION_USB_PERMISSION:
                if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    CommonUtils.showToastMsg(context, "USB权限不足");
                    return;
                }
                if (!ImageModel.getInstance().openUsbDevice(context, 0, null)) {
                    CommonUtils.showToastMsg(context, "USB连接失败");
                    return;
                }
                CommonUtils.showToastMsg(context, "USB连接成功");
                break;
            case UsbManager.ACTION_USB_ACCESSORY_ATTACHED:
                CommonUtils.showToastMsg(context, "ACTION_USB_ACCESSORY_ATTACHED");
                break;
            case UsbManager.ACTION_USB_ACCESSORY_DETACHED:
                CommonUtils.showToastMsg(context, "ACTION_USB_ACCESSORY_DETACHED");
                break;
        }
    }

}
