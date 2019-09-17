package com.usb.usblibrary;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CommonUtils {
    /**
     * 调试标志
     */
    private static boolean debugFlag=true;
    public static Context debugContext;

    private CommonUtils(){}

    /**
     * 判断当前线程是否是主线程
     * @return
     */
    public static boolean isInMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * 屏幕显示提示信息
     * @param context
     * @param msg
     */
    public static void showToastMsg(Context context, String msg) {
        if(debugFlag==false||isInMainThread()==false){
            return;
        }
        if(context==null){
            if(debugContext==null){
                return;
            }else {
                context=debugContext;
            }
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            if(context.isDestroyed()){
//                return;
//            }
//        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 解决安卓6.0以上版本不能读取外部存储权限的问题
     * @param activity
     * @param requestCode
     * @return
     */
    public static boolean isGrantExternalRW(AppCompatActivity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ||
                        activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED)) {

            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, requestCode);

            return false;
        }
        return true;
    }

}
