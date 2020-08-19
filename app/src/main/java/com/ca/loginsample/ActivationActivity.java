package com.ca.loginsample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import androidx.multidex.MultiDex;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.StrictMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ca.Utils.CSConstants;
import com.ca.Utils.CSEvents;
import com.ca.loginsample.client.ApiClient;
import com.ca.wrapper.CSClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivationActivity extends Activity {
    private ProgressDialog mProgressBar;
    private Handler mProgressBarHandler = new Handler();
    private int mTimeDelay = 40000;
    private Runnable mProgressBarRunnable;
    private TextView mMobileNumberTv;
    private Button mValidateOTPButton;
    private EditText mActivationCodeEdt;
    private CSClient CSClientObj = new CSClient();
    private String mMobileNumber = "";
    private String region = "";
    private static final String PROJECT_ID="pid_2767077b_ee96_4a42_93c8_affbd5ec4a18";
    private static final String AUTH_TOKEN="52883082_ad50_4f19_a6e5_8fb1f3751ef1";
    private static final String USERNAME=Constants.phoneNumber;
    private static final String PASSWORD=Constants.password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        mMobileNumberTv = findViewById(R.id.mobile_number_tv);
        mValidateOTPButton = findViewById(R.id.validate_button);
        mActivationCodeEdt = findViewById(R.id.activation_code_edt);

        mMobileNumber = getIntent().getStringExtra(Constants.INTENT_MOBILE_NUMBER);
        region = getIntent().getStringExtra(Constants.INTENT_REGION);
        mMobileNumberTv.setText(Constants.phoneNumber);

        // Validation button click listener
        mValidateOTPButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // need to check network available of not before call Activate API
                if (!isNetworkAvailable(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), getString(R.string.network_unavailable_message), Toast.LENGTH_SHORT).show();
                    return;
                }
                // need to check OTP entered or not
                if (!mActivationCodeEdt.getText().toString().isEmpty()) {
                    // disable validate OTP Button to avoid multiple clicks
                    mValidateOTPButton.setEnabled(false);
                    showProgressbar();
                    // Activation API to validate OTP
//                    CSClientObj.activate(mMobileNumber, mActivationCodeEdt.getText().toString());
                    ApiClient.getInstance(ApiClient.BASE_URL).activateaccount(Constants.phoneNumber,Constants.password,mActivationCodeEdt.getText().toString())
                            .enqueue(new Callback<JsonElement>() {
                                @Override
                                public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                                    if(response.isSuccessful()){
                                        JsonObject object=response.body().getAsJsonObject();
                                        JsonObject data=object.getAsJsonObject("data");
                                        String message=data.get("message").getAsString();

                                        if(message.equalsIgnoreCase("User Activated")){

                                            ApiClient.getInstanceVOX(ApiClient.BASE_VOX).user(PROJECT_ID,AUTH_TOKEN,USERNAME,PASSWORD)
                                                    .enqueue(new Callback<JsonElement>() {
                                                        @Override
                                                        public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                                                            if(response.isSuccessful()){
                                                                JsonObject main_object=response.body().getAsJsonObject();
                                                                JsonObject data=main_object.getAsJsonObject("data");
                                                                String message=data.get("message").getAsString();
                                                                if(message.equalsIgnoreCase("User created")) {
                                                                    Intent intent=new Intent(getApplicationContext(),AboutActivity.class);
                                                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                                    startActivity(intent);
                                                                    finish();
                                                                }else{
                                                                    Toast.makeText(ActivationActivity.this, ""+message, Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onFailure(Call<JsonElement> call, Throwable t) {
                                                            t.printStackTrace();
                                                        }
                                                    });

                                        }else{
                                            Toast.makeText(ActivationActivity.this, ""+message, Toast.LENGTH_SHORT).show();
                                            dismissProgressbar();
                                        }
                                    }else{
                                      dismissProgressbar();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JsonElement> call, Throwable t) {
                                    t.printStackTrace();
                                    dismissProgressbar();
                                }
                            });
                } else {
                    mActivationCodeEdt.setError(getString(R.string.error_empty_otp));
                }
            }
        });
    }




    @Override
    public void onBackPressed() {
        super.onBackPressed();
        dismissProgressbar();
        finish();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * BroadCastReceiver to handle SDK events
     */





    /**
     * This method will show the progressbar while validating the OTP
     */
    public void showProgressbar() {
        try {
            if (getApplicationContext() != null) {
                mProgressBar = new ProgressDialog(ActivationActivity.this);
                mProgressBar.setCancelable(false);
                mProgressBar.setMessage("Please Wait..");
                mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressBar.setProgress(0);
                mProgressBar.show();
                // this handler will close the progressbar if response not came with in specified time
                mProgressBarHandler = new Handler();
                mProgressBarRunnable = new Runnable() {

                    public void run() {
                        mProgressBarHandler.postDelayed(this, mTimeDelay);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                dismissProgressbar();
                                Toast.makeText(getApplicationContext(), getString(R.string.network_error_message), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                };
                mProgressBarHandler.postDelayed(mProgressBarRunnable, mTimeDelay);

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method will dismiss the progressbar after response from server
     */
    public void dismissProgressbar() {
        try {
            mValidateOTPButton.setEnabled(true);
            if (mProgressBar != null) {
                mProgressBar.dismiss();

            }
            if (mProgressBarHandler != null) {
                mProgressBarHandler.removeCallbacks(mProgressBarRunnable);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method checks network available or not.
     *
     * @param context application context.
     * @return boolean variable
     */
    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager
                    .getActiveNetworkInfo();

            return activeNetworkInfo != null
                    && activeNetworkInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
