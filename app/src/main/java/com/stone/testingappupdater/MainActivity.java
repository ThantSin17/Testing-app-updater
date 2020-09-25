package com.stone.testingappupdater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.stone.appupdater.AppUpdaterTesting;
import com.stone.appupdater.SampleUpdater;

public class MainActivity extends AppCompatActivity {
SampleUpdater sampleUpdater;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sampleUpdater=new SampleUpdater(this);
        sampleUpdater.check("https://appsamples.000webhostapp.com/testingupdateurl.php");
        AppUpdaterTesting updater=new AppUpdaterTesting();
        Toast.makeText(this, updater.getStr(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        sampleUpdater.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}