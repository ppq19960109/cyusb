package com.usb.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatRadioButton;

import com.usb.model.R;


public class PicRadioButton extends AppCompatRadioButton {
//    public PicRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//
//    }

//    public PicRadioButton(Context context) {
//        super(context);
//    }

    public PicRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        Drawable drawableTop=null,drawableLeft,drawableRight,drawableBottom;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.picRadioButton);//获取我们定义的属性

        int attrCount = ta.getIndexCount();
//        drawable = typedArray.getDrawable(attr);
        String str=ta.getString(R.styleable.picRadioButton_text);
//        int pos = typedArray.getResourceId(R.styleable.PicRadioButton_drawableTop,0);
//        drawableTop = typedArray.getDrawable(pos);
//        drawableRight = typedArray.getDrawable(R.styleable.PicRadioButton_drawableTop);
//        drawableTop.setBounds(0, 0, 80, 80);

        setCompoundDrawables(null, drawableTop, null, null);
        ta.recycle();
    }


}
