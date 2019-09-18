package com.usb.view.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.usb.common.CommonUtils;
import com.usb.common.ImageProUtils;
import com.usb.model.R;
import com.usb.presenter.TE_A;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class ImagePager extends BaseFragment {

    private CheckBox cameraPower;
    private TextView tvfrequency;
    private ImageView imageView;

    private TE_A cyImage;
    private RadioGroup rgFcous;

    private BitmapPlayThread BmpPlayThread;
    private Timer timerThread;
    private TimerTask timerTask;
    private Handler handlerPost = new Handler();
    private short[] imagebuf = new short[288 * 384];

    private CheckBox cb_play;
    private CheckBox cb_correct;

    public ImagePager(Context context) {
        super(context);
        initData();
    }

    @Override
    public View initView() {

        return null;
    }

    @Override
    public void initData() {
        super.initData();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cyImage = new TE_A();
        cyImage.initialize(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFindView(view);
        initListener();

        cyImage.openTE_A(getActivity(), 0);
        int ret = cyImage.cameraPower(true);
        if (ret != 0) {
            CommonUtils.showToastMsg(getContext(), "cameraPower失败");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ThreadStop();
        cyImage.closeTE();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initFindView(View view) {
        cameraPower = view.findViewById(R.id.cameraPower);
        tvfrequency = view.findViewById(R.id.tv_fre);
        imageView = view.findViewById(R.id.iv_image);
        cb_play=view.findViewById(R.id.cb_play);
        cb_correct=view.findViewById(R.id.cb_correct);
        initRadioGroup(view);
    }

    private void initRadioGroup(View view) {
        ArrayList<Button> rbList = new ArrayList<>();
        rbList.add(view.findViewById(R.id.rb_nearl));
        rbList.add(view.findViewById(R.id.rb_nears));
        rbList.add(view.findViewById(R.id.rb_fars));
        rbList.add(view.findViewById(R.id.rb_farl));

        ArrayList<Drawable> rbDraw = new ArrayList<>();
        rbDraw.add(getResources().getDrawable(R.drawable.scan_nearl));
        rbDraw.add(getResources().getDrawable(R.drawable.scan_nears));
        rbDraw.add(getResources().getDrawable(R.drawable.scan_fars));
        rbDraw.add(getResources().getDrawable(R.drawable.scan_farl));

        for (int i = 0; i < 4; ++i) {
            Drawable drawable = rbDraw.get(i);
            drawable.setBounds(0, 0, 100, 100);
            rbList.get(i).setCompoundDrawables(null, drawable, null, null);
        }

        rgFcous = view.findViewById(R.id.rg_tag);
        rgFcous.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_nearl:
                        cyImage.setFocusing(true, true);
                        CommonUtils.showToastMsg(null, "rb_nearl");
                        break;
                    case R.id.rb_nears:
                        cyImage.setFocusing(false, true);
                        CommonUtils.showToastMsg(null, "rb_nears");
                        break;
                    case R.id.rb_fars:
                        cyImage.setFocusing(false, false);
                        CommonUtils.showToastMsg(null, "rb_fars");
                        break;
                    case R.id.rb_farl:
                        cyImage.setFocusing(true, false);
                        CommonUtils.showToastMsg(null, "rb_farl");
                        break;
                }

            }
        });


    }

    private void initListener() {
        cb_play.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    ThreadStart();
                }else {
                    ThreadStop();
                }
            }
        });
        cb_correct.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    cyImage.shutterCalibrationOn();
                }else {
                    cyImage.removeImageCorrect();
                }
            }
        });
        cameraPower.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cyImage.cameraPower(isChecked);
            }
        });

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
                    CommonUtils.showToastMsg(null,"X:" + x + "Y:" + y + "温度:" + temp);
                    return true;
                }
                return false;
            }
        });

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