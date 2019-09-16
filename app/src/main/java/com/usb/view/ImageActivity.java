package com.usb.view;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.usb.common.CommonUtils;
import com.usb.common.ImageProUtils;
import com.usb.model.R;
import com.usb.presenter.TE_A;

import java.util.Timer;
import java.util.TimerTask;

public class ImageActivity extends Activity {

    private Button focusNear;
    private Button focusFar;
    private Button imageClear;
    private Button imageplay;
    private Button imagestop;
    private Button imagecorrect;
    private CheckBox cameraPower;

    private TextView tvtext;
    private TextView tvfrequency;
    private ImageView imageView;

    private TE_A cyImage;

    private BitmapPlayThread BmpPlayThread;

    private Timer timerThread;
    private TimerTask timerTask;

    private Handler handlerPost = new Handler();
    private short[] imagebuf = new short[288 * 384];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        initView();
        initListener();

        cyImage = new TE_A();
        cyImage.initialize(this);
        cyImage.openTE_A(this, 0);
        int ret=cyImage.cameraPower(true);
        if(ret!=0)
        {
            CommonUtils.showToastMsg(ImageActivity.this,"cameraPower"+ret);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThreadStop();
        cyImage.closeTE();
    }

    private void initView() {
        focusNear = (Button) findViewById(R.id.focusNear);
        focusFar = (Button) findViewById(R.id.focusFar);
        imageClear = (Button) findViewById(R.id.imageClear);
        imageplay = (Button) findViewById(R.id.imageplay);
        imagestop = (Button) findViewById(R.id.imagestop);
        imagecorrect = (Button) findViewById(R.id.imagecorrect);
        cameraPower = (CheckBox) findViewById(R.id.cameraPower);

        tvfrequency = (TextView) findViewById(R.id.tv_fre);
        imageView = (ImageView) findViewById(R.id.iv_image);

    }

    private void initListener() {
        Manager manager = new Manager();
        imageClear.setOnClickListener(manager);
        imageplay.setOnClickListener(manager);
        imagestop.setOnClickListener(manager);
        imagecorrect.setOnClickListener(manager);
        cameraPower.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cyImage.cameraPower(isChecked);
            }
        });
        focusNear.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        cyImage.setFocusing(true, true);
                        return true;
                    }
                    return false;
                }
        );
        focusFar.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        cyImage.setFocusing(true, false);
                        return true;
                    }
                    return false;
                }
        );

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    int[] viewCoord = new int[2];
                    v.getLocationOnScreen(viewCoord);
                    int touchX = (int) e.getX();
                    int touchY = (int) e.getY();

                    int x = touchX * 384 / v.getWidth();
                    int y = touchY * 288 / v.getHeight();
                    float temp = cyImage.calcTemp((short) x, (short) y);
                    tvtext.setText("X:" + x + "Y:" + y + "温度:" + temp);
                    return true;
                } else {
                    return false;
                }
            }
        });

        Button mNextButton=(Button)findViewById(R.id.rb_nearl);
        Drawable drawable=getResources().getDrawable(R.drawable.scan_nearl);
        drawable.setBounds(0,0,200,100);
        mNextButton.setCompoundDrawables(null,drawable,null,null);

    }

    private void ThreadStart() {
        BmpPlayThread = new BitmapPlayThread();
        BmpPlayThread.execute();

        timerThread = new Timer();
        timerTask = new BitmapTimerTask();
        timerThread.schedule(timerTask, 0, 1000);

    }

    private void ThreadStop() {

        if (BmpPlayThread != null) {
            BmpPlayThread.cancel(true);
            BmpPlayThread = null;
        }
        if (timerThread != null) {
            timerThread.cancel();
            timerThread = null;
        }
    }

    class Manager implements View.OnClickListener {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imagecorrect: {
                    cyImage.shutterCalibrationOn();
                }
                break;
                case R.id.imageplay: {
                    ThreadStart();
                }
                break;
                case R.id.imagestop: {
                    ThreadStop();
                }
                break;
                case R.id.imageClear: {
                    cyImage.removeImageCorrect();
                }
                break;
                case R.id.focusNear: {

                }
                break;
                case R.id.focusFar: {

                }
                break;
            }
        }
    }


    class BitmapPlayThread extends AsyncTask<Integer, Bitmap, Void> {

        @Override
        protected Void doInBackground(Integer... integer) {
            Bitmap bitmap = null;
            for (; ; ) {

                if (cyImage.recvImage(imagebuf, true) != 1) {
                    continue;
                }
                bitmap = cyImage.createBitmap(imagebuf);
//                bitmap = Bitmap.createScaledBitmap(bitmap, imageView.getWidth()*384/288, imageView.getWidth(), false);
                bitmap = ImageProUtils.rotateBimap(90, bitmap);
                if (bitmap != null) {
                    publishProgress(bitmap);
                }
                if (isCancelled()) {
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Bitmap... values) {
            super.onProgressUpdate(values);
            imageView.setImageBitmap(values[0]);
        }
    }

    class BitmapTimerTask extends TimerTask {

        @Override
        public void run() {
            handlerPost.post(new Runnable() {
                @Override
                public void run() {
                    tvfrequency.setText(cyImage.getFrequency());
                }
            });

        }
    }

}