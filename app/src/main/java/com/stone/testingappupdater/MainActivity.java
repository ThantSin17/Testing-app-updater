package com.stone.testingappupdater;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.stone.appupdater.AppUpdaterTesting;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppUpdaterTesting updater=new AppUpdaterTesting();
        Toast.makeText(this, updater.getStr(), Toast.LENGTH_SHORT).show();
    }
}