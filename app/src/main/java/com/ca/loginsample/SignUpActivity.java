package com.ca.loginsample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.content.ContextCompat;
import androidx.multidex.MultiDex;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ca.Utils.CSConstants;
import com.ca.Utils.CSEvents;
import com.ca.dao.CSAppDetails;
import com.ca.loginsample.client.ApiClient;
import com.ca.wrapper.CSClient;
import com.ca.wrapper.CSDataProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class will initialize the application to server and generate OTP
 */
public class SignUpActivity extends Activity {

    private TextView mSignUpButtonTv;
    private EditText mMobileNumberEdt;
    private EditText mUserPassWordEdt;
    private String countryCode = "+92";
    private ProgressDialog mProgressDialog;
    private Handler mProgressBarHandler = new Handler();
    private int mProgressBarDelay = 40000;
    private Runnable mProgressBarRunnable;
    private CSClient CSClientObj = new CSClient();
    private AutoCompleteTextView spinner_country;
    private String countryList[];
    private TelephonyManager tm;
    private ProgressBar progress_circular;


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_signup);

            mMobileNumberEdt = findViewById(R.id.mobile_number_edt);
            mUserPassWordEdt = findViewById(R.id.password_edt);
            mSignUpButtonTv = findViewById(R.id.sign_up_button_tv);
            spinner_country=findViewById(R.id.spinner_country);
            progress_circular=findViewById(R.id.progress_circular);
            countryList=this.getResources().getStringArray(R.array.CountryCodes);
            tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
            String countryCodeValue = tm.getNetworkCountryIso();

            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.select_dialog_item,countryList);
            spinner_country.setAdapter(adapter);
            spinner_country.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Toast.makeText(SignUpActivity.this, ""+adapterView.getAdapter().getItem(i), Toast.LENGTH_SHORT).show();
                }
            });
            for (int i = 0; i <spinner_country.getAdapter().getCount() ; i++) {
                String country=spinner_country.getAdapter().getItem(i).toString().substring(
                        spinner_country.getAdapter().getItem(i).toString().indexOf(" "),
                        spinner_country.getAdapter().getItem(i).toString().length()
                );


                if(country.trim().equalsIgnoreCase(countryCodeValue.toUpperCase().trim())){
                    spinner_country.setText(spinner_country.getAdapter().getItem(i).toString(),false);
                }

            }
            //Sign Up button click listener
            mSignUpButtonTv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    countryCode=spinner_country.getText().subSequence(0,spinner_country.getText().toString().indexOf(" ")).toString();
                    // need to check number is empty or not to proceed registration

                    if (mMobileNumberEdt.getText().toString().trim().equals("")) {
                        mMobileNumberEdt.setError(getString(R.string.error_empty_number));
                    } else {

                        // need to check network available or not before call API
                        if (!isNetworkAvailable(getApplicationContext())) {
                            Toast.makeText(getApplicationContext(), getString(R.string.network_unavailable_message), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // need to check entered number is valid or not for selected country  this method will return null if given number is not belongs to selected country

                        Constants.phoneNumber = CSClientObj.getInternationalFormatNumber(mMobileNumberEdt.getText().toString(), countryCode);

                        // if given number is valid we will proceed for registration otherwise show error message to user
                        if (Constants.phoneNumber != null) {
                            // if user not gave any password we have to create random password
                            if (mUserPassWordEdt.getText().toString().equals("") || mUserPassWordEdt.getText().toString().length()<6) {
                                mUserPassWordEdt.setText(R.string.error_password);
                            } else {
                                progress_circular.setVisibility(View.VISIBLE);
                                Constants.password = mUserPassWordEdt.getText().toString();
                                ApiClient.getInstance(ApiClient.BASE_URL).createuser(Constants.phoneNumber,Constants.password)
                                        .enqueue(new Callback<JsonElement>() {
                                            @Override
                                            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                                                if(response.isSuccessful()){
                                                    JsonObject object=response.body().getAsJsonObject();
                                                    JsonObject data=object.getAsJsonObject("data");
                                                    String message=data.get("message").getAsString();
                                                    if(message.equalsIgnoreCase("User created")) {
                                                        progress_circular.setVisibility(View.GONE);
                                                        Intent intent = new Intent(getApplicationContext(), ActivationActivity.class);
                                                        startActivity(intent);
                                                        finish();


                                                    }else {
                                                        Toast.makeText(SignUpActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                                                    }

                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<JsonElement> call, Throwable t) {
                                                t.printStackTrace();
                                                Toast.makeText(SignUpActivity.this, "Error: "+t.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            }
                            // need to show confirmation message to user to check the number once
//                            showAlertToUser();
                        } else {
                            mMobileNumberEdt.setError(getString(R.string.error_empty_number));
                        }
                    }
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTIVATION_SCREEN_INTENT_CODE && Constants.isAlreadySignedUp) {
            finish();
        }
    }


    /**
     * This method will show the confirmation message to user to check the entered message
     *
     * @return
     */
    public boolean showAlertToUser() {
        try {
            Builder builderDialog = new Builder(SignUpActivity.this);
            builderDialog.setTitle(getString(R.string.user_alert_dialog_tittle));

            builderDialog.setMessage(getString(R.string.user_alert_confirm_message) + Constants.phoneNumber);

            builderDialog.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            // need to check network available or not before call API
                            if (!isNetworkAvailable(getApplicationContext())) {
                                Toast.makeText(getApplicationContext(), getString(R.string.network_unavailable_message), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            showProgressbar();
                            CSAppDetails csAppDetails = new CSAppDetails("LoginSample", "pid_39684a6d_5103_4254_9775_1b923b9b98d5");
                            CSClientObj.initialize(Constants.server, Constants.port, csAppDetails);
                        }
                    });

            builderDialog.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                        }
                    });
            builderDialog.show();

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

    }
    public boolean showLoginAlertToUser() {
        try {
            Builder builderDialog = new Builder(SignUpActivity.this);
            //builderDialog.setTitle(getString(R.string.user_alert_dialog_tittle));

            builderDialog.setMessage("User already logged in!!");

            builderDialog.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                           finish();
                        }
                    });

            builderDialog.show();

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

    }
    /**
     * This broadcast receiver wil, catch the events coming from SDK
     */
    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction().equals(CSEvents.CSCLIENT_NETWORKERROR)) {

                    // if network fluctuation came in between registration  dismiss the progressbar and show netwrok error message to user

                    Toast.makeText(getApplicationContext(), getString(R.string.network_error_message), Toast.LENGTH_SHORT).show();
                    dismissProgressbar();

                } else if (intent.getAction().equals(CSEvents.CSCLIENT_LOGIN_RESPONSE)) {

                    // if SignUp is success dismiss the progressbar and open Activation screen
                    // if SignUp failure show the error message to user

                    if (intent.getStringExtra(CSConstants.RESULT).equals(CSConstants.RESULT_SUCCESS)) {
                        dismissProgressbar();
                       showLoginAlertToUser();
                    } else {
                        dismissProgressbar();
                        Toast.makeText(SignUpActivity.this, getString(R.string.login_failure_message), Toast.LENGTH_SHORT).show();
                    }

                } else if (intent.getAction().equals(CSEvents.CSCLIENT_SIGNUP_RESPONSE)) {

                    // if SignUp is success dismiss the progressbar and open Activation screen
                    // if SignUp failure show the error message to user

                    if (intent.getStringExtra(CSConstants.RESULT).equals(CSConstants.RESULT_SUCCESS)) {
                        dismissProgressbar();
                        Intent activationIntent = new Intent(getApplicationContext(), ActivationActivity.class);
                        activationIntent.putExtra(Constants.INTENT_MOBILE_NUMBER, Constants.phoneNumber);
                        activationIntent.putExtra(Constants.INTENT_REGION, countryCode);
                        startActivityForResult(activationIntent, Constants.ACTIVATION_SCREEN_INTENT_CODE);
                    } else {
                        dismissProgressbar();
                        Toast.makeText(SignUpActivity.this, getString(R.string.sign_up_failure), Toast.LENGTH_SHORT).show();
                    }

                } else if (intent.getAction().equals(CSEvents.CSCLIENT_INITILIZATION_RESPONSE)) {

                    // if Initialization to server is success call SignUp API and call contacts loading API
                    // if initialization to server not success dismiss the progressbar and show the error response to user
                    if (intent.getStringExtra(CSConstants.RESULT).equals(CSConstants.RESULT_SUCCESS)) {
                        if (!CSDataProvider.getSignUpstatus()) {

                            // this API will enable contacts loading method in SDK
                            CSClientObj.enableNativeContacts(true, 91);

                            // This will call SignUp API to generate OTP
                            CSClientObj.signUp(Constants.phoneNumber, Constants.password, false);
                        }
                    } else {
                        dismissProgressbar();
                        int returnCode = intent.getIntExtra(CSConstants.RESULTCODE, 0);
                        if (returnCode == CSConstants.E_409_NOINTERNET) {
                            Toast.makeText(getApplicationContext(), getString(R.string.network_unavailable_message), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.initialization_failure_message), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    MyReceiver MyReceiverObj = new MyReceiver();

    @Override
    public void onResume() {
        super.onResume();

        try {

            MyReceiverObj = new MyReceiver();
            // register the receivers to catch the events coming from SDK
            IntentFilter filter = new IntentFilter();
            filter.addAction(CSEvents.CSCLIENT_NETWORKERROR);
            filter.addAction(CSEvents.CSCLIENT_SIGNUP_RESPONSE);
            filter.addAction(CSEvents.CSCLIENT_INITILIZATION_RESPONSE);
            filter.addAction(CSEvents.CSCLIENT_LOGIN_RESPONSE);

            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(MyReceiverObj, filter);
            // it will clear the user db for fresh registration
            //new CSClient().reset();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            // unregister the receiver
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(MyReceiverObj);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * This method will show the progressbar while doing registration
     */
    public void showProgressbar() {
        try {
            if (getApplicationContext() != null) {
                mProgressDialog = new ProgressDialog(SignUpActivity.this);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setMessage("Please Wait..");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setProgress(0);
                mProgressDialog.show();
                // this handler will close the progressbar if response not came with in specified time
                mProgressBarHandler = new Handler();
                mProgressBarRunnable = new Runnable() {
                    public void run() {
                        mProgressBarHandler.postDelayed(this, mProgressBarDelay);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                dismissProgressbar();
                                Toast.makeText(getApplicationContext(), getString(R.string.network_error_message), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                };
                mProgressBarHandler.postDelayed(mProgressBarRunnable, mProgressBarDelay);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * This will dismiss the progressbar  once got any response from server
     */
    public void dismissProgressbar() {
        try {

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
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
