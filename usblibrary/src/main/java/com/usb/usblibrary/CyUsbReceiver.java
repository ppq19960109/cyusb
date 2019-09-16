package com.usb.usblibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

public class CyUsbReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        switch (action) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                CommonUtils.showToastMsg(context, "USB插入");
                ImageModel.getInstance().openUsbDevice(context,0,null);
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                CommonUtils.showToastMsg(context, "USB拔出");
                ImageModel.getInstance().close();
                break;
            case CyUsbDevice.ACTION_USB_PERMISSION:
                if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    CommonUtils.showToastMsg(context, "USB权限不足");
                    return;
                }
                if (CyUsbDevice.getInstance().openUsbPort(context, 0)!=0) {
                    CommonUtils.showToastMsg(context, "USB连接失败");
                    return;
                }
                CommonUtils.showToastMsg(context,"USB连接成功");
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
