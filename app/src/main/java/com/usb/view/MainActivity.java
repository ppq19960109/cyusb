package com.usb.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.usb.model.R;

public class MainActivity extends AppCompatActivity {

    private Button image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    private void initView() {
        image = (Button) findViewById(R.id.bt_image);
    }

    private void initListener() {
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.usb.image");
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this, ImageActivity.class);
                startActivity(intent);
            }
        });
    }
}
