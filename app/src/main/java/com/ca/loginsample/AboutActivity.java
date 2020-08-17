package com.ca.loginsample;

import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.ca.wrapper.CSClient;

public class AboutActivity extends AppCompatActivity {
    private TextView mAppVersionTv, mSDKVersionTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mAppVersionTv = findViewById(R.id.app_version_tv);
        mSDKVersionTv = findViewById(R.id.sdk_version_tv);
        CSClient CSClientObj = new CSClient();
        mSDKVersionTv.setText("SDK Version: " + CSClientObj.getVersion());
        try {
            mAppVersionTv.setText("App Version: " + this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
