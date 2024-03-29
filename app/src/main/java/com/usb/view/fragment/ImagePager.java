package com.usb.view.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.usb.common.CommonUtils;
import com.usb.common.ImageProUtils;
import com.usb.model.R;
import com.usb.presenter.TE_A;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class ImagePager extends BaseFragment {

    private CheckBox cameraPower;
    private TextView tvfrequency;
    private TextView tv_temp;
    private ImageView imageView;

    private TE_A cyImage;

    private BitmapPlayThread BmpPlayThread;
    private Timer timerThread;
    private TimerTask timerTask;
    private Handler handlerPost = new Handler();
    private short[] imagebuf = new short[288 * 384];

    private CheckBox cb_play;
    private CheckBox cb_correct;

    private ArrayList<Button> rbList ;
    private ArrayList<Drawable> rbDraw;

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
        if (cyImage.cameraPower(true) != 0) {
            CommonUtils.showToastMsg(getContext(), "打开摄像机电源失败");
        }

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                for (int i = 0; i < 4; ++i) {
                    Drawable drawable = rbDraw.get(i);
                    drawable.setBounds(0, 0, rbList.get(i).getWidth(), rbList.get(i).getHeight());
                    rbList.get(i).setCompoundDrawables(null, drawable, null, null);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ThreadStop();
        cyImage.cameraPower(false);
        cyImage.closeTE();
    }

    private void initFindView(View view) {
        cameraPower = view.findViewById(R.id.cameraPower);
        tv_temp= view.findViewById(R.id.tv_temp);
        tvfrequency = view.findViewById(R.id.tv_fre);
        imageView = view.findViewById(R.id.iv_image);
        cb_play = view.findViewById(R.id.cb_play);
        cb_correct = view.findViewById(R.id.cb_correct);

        initRadioGroup(view);
    }
    ;

    private void initRadioGroup(View view) {
        rbList = new ArrayList<>();
        rbDraw = new ArrayList<>();
        rbList.add(view.findViewById(R.id.rb_nearl));
        rbList.add(view.findViewById(R.id.rb_nears));
        rbList.add(view.findViewById(R.id.rb_fars));
        rbList.add(view.findViewById(R.id.rb_farl));


        rbDraw.add(getResources().getDrawable(R.drawable.scan_nearl));
        rbDraw.add(getResources().getDrawable(R.drawable.scan_nears));
        rbDraw.add(getResources().getDrawable(R.drawable.scan_fars));
        rbDraw.add(getResources().getDrawable(R.drawable.scan_farl));

        rbList.get(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cyImage.setFocusing(true, true);
            }
        });
        rbList.get(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cyImage.setFocusing(false, true);
            }
        });
        rbList.get(2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cyImage.setFocusing(false, false);
            }
        });
        rbList.get(3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cyImage.setFocusing(true, false);
            }
        });

        cameraPower.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cyImage.cameraPower(isChecked);

            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i("ImagePager","onSaveInstanceState");
    }

    public static void rotate(float degree,int[] coordinate,int width,int height) {
        if(degree==90){
            int temp=coordinate[1];
            coordinate[1] =coordinate[0];
            coordinate[0]=height-temp;
        }
        else if(degree==180) {
            coordinate[0] = width - 1 - coordinate[0];
            coordinate[1] = height - 1 - coordinate[1];
        }
    }
    private void initListener() {
        cb_play.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ThreadStart();
                } else {
                    ThreadStop();
                }
            }
        });
        cb_correct.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cyImage.shutterCalibrationOn();
                } else {
                    cyImage.removeCalibrationOn();
                }
            }
        });


        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    int[] viewCoord = new int[2];
                    int[] coord = new int[2];
                    v.getLocationOnScreen(viewCoord);

                    coord[0] = (int) e.getX();
                    coord[1] = (int) e.getY();

                    coord[0] = coord[0] * 384 / v.getWidth();
                    coord[1] = coord[1] * 288 / v.getHeight();
                    rotate(180,coord,384,288);
                    float temp = cyImage.calcTemp((short)coord[0], (short) coord[1]);
                    tv_temp.setText("X:" + coord[0] + "Y:" + coord[1] + "温度:" + temp);
                    return true;
                }
                return false;
            }
        });

    }

    private void ThreadStart() {
        BmpPlayThread = new BitmapPlayThread(this);
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

        private WeakReference<ImagePager> imagePager;
        public BitmapPlayThread(ImagePager imagePager){
            this.imagePager=new WeakReference<>(imagePager);
        }
        @Override
        protected Void doInBackground(Integer... integer) {
            Bitmap bitmap = null;
            for (; ; ) {
                ImagePager image=imagePager.get();
                if(image==null){
                  break;
                }
                if (image.cyImage.recvImage(image.imagebuf, true) != 1) {
                    continue;
                }
                bitmap = image.cyImage.createBitmap(image.imagebuf);
//                bitmap = Bitmap.createScaledBitmap(bitmap, imageView.getWidth()*384/288, imageView.getWidth(), false);
                bitmap = ImageProUtils.rotateBimap(180, bitmap);
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
            ImagePager image=imagePager.get();
            if(image!=null){
                image.imageView.setImageBitmap(values[0]);
            }

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
