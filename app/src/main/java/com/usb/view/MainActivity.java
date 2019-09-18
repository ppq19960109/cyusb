package com.usb.view;

import android.os.Bundle;
import android.widget.RadioGroup;

import com.usb.common.CommonUtils;
import com.usb.model.R;
import com.usb.view.fragment.BaseFragment;
import com.usb.view.fragment.ImagePager;
import com.usb.view.fragment.MainPager;

import java.util.ArrayList;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {

    private RadioGroup rgToolbar;
    private ArrayList<BaseFragment> baseFragment = new ArrayList<>();
    private int fragmentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CommonUtils.debugContext = this;
        initView();
        initListener();
        baseFragment.add(new MainPager(this));
        baseFragment.add(new ImagePager(this));
        rgToolbar.check(R.id.rb_main);
    }

    private void initView() {
        rgToolbar = (RadioGroup) findViewById(R.id.rg_toolbar);
    }

    private void initListener() {

        rgToolbar.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_main:
                        fragmentIndex = 0;
                        break;
                    case R.id.rb_image:
                        fragmentIndex = 1;
                        break;
                }
                setfragment();
            }
        });
    }

    private void setfragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fl_main, baseFragment.get(fragmentIndex));
        fragmentTransaction.commit();
    }
}
