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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ca.Utils.CSConstants;
import com.ca.Utils.CSEvents;
import com.ca.wrapper.CSClient;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);
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
                    CSClientObj.activate(mMobileNumber, mActivationCodeEdt.getText().toString());
                } else {
                    mActivationCodeEdt.setError(getString(R.string.error_empty_otp));
                }
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        try {
            // Register the receivers for SDK events
            MainActivityReceiverObj = new MainActivityReceiver();
            IntentFilter filter = new IntentFilter(CSEvents.CSCLIENT_NETWORKERROR);
            filter.addAction(CSEvents.CSCLIENT_LOGIN_RESPONSE);
            filter.addAction(CSEvents.CSCLIENT_ACTIVATION_RESPONSE);
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(MainActivityReceiverObj, filter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            // unregister the receiver
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(MainActivityReceiverObj);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
    public class MainActivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                // enable validate button once receive event from SDK
                mValidateOTPButton.setEnabled(true);
                if (intent.getAction().equals(CSEvents.CSCLIENT_NETWORKERROR)) {
                    // if network fluctuation came in between registration  dismiss the progressbar and show netwrok error message to user

                    dismissProgressbar();
                    Toast.makeText(getApplicationContext(), getString(R.string.network_error_message), Toast.LENGTH_SHORT).show();

                } else if (intent.getAction().equals(CSEvents.CSCLIENT_LOGIN_RESPONSE)) {

                    // if Login is success dismiss the progressbar show message to user
                    // if Login failure show the error message to user

                    if (intent.getStringExtra(CSConstants.RESULT).equals(CSConstants.RESULT_SUCCESS)) {
                        dismissProgressbar();
                        Toast.makeText(getApplicationContext(), getString(R.string.login_success_message), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        dismissProgressbar();
                        Toast.makeText(getApplicationContext(), getString(R.string.login_failure_message), Toast.LENGTH_SHORT).show();
                    }
                } else if (intent.getAction().equals(CSEvents.CSCLIENT_ACTIVATION_RESPONSE)) {

                    // if Activation is success dismiss the progressbar and call Login API
                    // if Activation failure show the error message to user

                    if (intent.getStringExtra(CSConstants.RESULT).equals(CSConstants.RESULT_SUCCESS)) {
                        Constants.isAlreadySignedUp = true;
                        if (region == null || region.equals("")) {
                            region = "+91";
                        }
                        // Login API to login to server with credentials
                        CSClientObj.login(Constants.phoneNumber, Constants.password);
                    } else {
                        dismissProgressbar();
                        Toast.makeText(getApplicationContext(), "Wrong Code.", Toast.LENGTH_SHORT).show();

                    }
                }


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    MainActivityReceiver MainActivityReceiverObj = new MainActivityReceiver();

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
