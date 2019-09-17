package com.usb.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.usb.model.R;

public class MainActivity extends AppCompatActivity {

    private Button image;
    private RadioGroup rgToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    private void initView() {
        image = (Button) findViewById(R.id.bt_image);
        rgToolbar=(RadioGroup) findViewById(R.id.rg_toolbar);
    }

    private void initListener() {
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.usb.image");
                startActivity(intent);
            }
        });


        rgToolbar.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_main:

                        break;
                    case R.id.rb_image:
                        Intent intent = new Intent("android.intent.usb.image");
                        startActivity(intent);
                        break;
                }

            }
        });
    }


}
