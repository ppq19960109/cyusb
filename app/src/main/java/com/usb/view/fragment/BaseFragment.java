package com.usb.view.fragment;

import android.content.Context;
import android.view.View;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    public View rootView;
    public Context context;

    public BaseFragment(Context context) {
        this.context = context;
        rootView = initView();
    }

    public abstract View initView();

    public void initData() {
    }

}
