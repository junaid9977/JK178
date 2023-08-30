package com.dev_bd.khanjk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import java.util.HashMap;
import java.util.Map;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.impl.adview.AppLovinRewardedInterstitialAd;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.dev_bd.khanjk.utils.Facts;
import com.dev_bd.khanjk.utils.Helper;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;
import com.startapp.sdk.adsbase.StartAppAd;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskActivity extends AppCompatActivity implements AppLovinAdDisplayListener, AppLovinAdClickListener {

    ConnectivityManager connectivityManager;
    NetworkInfo activeNetwork;
    Button taskButton;
    TextView factText, impressionCountText, impressionTargetText;
    ProgressBar loader;
    LinearLayout taskContainer;
    CountDownTimer countDownTimer;

    int adImpressionCount = 0;
    int adReloadTrial = 0;

    int VIEW_TARGET = 20, IMPRESSION_TIME = 1000 * 5,
            CLICK_TIME = 1000 * 10, BUTTON_CT_TIME = 1000 * 3,
            CLICK_TTIMER = 1, IM_TTIMER = 1,IM_TTIMER_SUCCESS = 1,
            CLICK_TTIMER_SUCCESS = 1, AUTO_BACK = 1;

    String USER_ACCOUNT;

    StartAppAd startAppAd;

    Boolean AD_CLICKED = false;
    Boolean AD_LOADED = false;
    Boolean AD_VIEW_SUCCESS = false;
    Boolean AD_CLICK_SUCCESS = false;
    Boolean SHOW_AD_AUTOMATICALLY = false;

    private AppLovinInterstitialAdDialog interstitialAd;
    private AppLovinRewardedInterstitialAd rewardedInterstitialAd;
    private AppLovinAd loadedAd;
    String videoAdsPoints;
    String waitingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        AppLovinSdk.getInstance(this);
        interstitialAd = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(this), this);
        interstitialAd.setAdDisplayListener(this);
        interstitialAd.setAdClickListener(this);
        loadAdlovineAds();

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = connectivityManager.getActiveNetworkInfo();

        taskButton = findViewById(R.id.taskButton);
        loader = findViewById(R.id.loader);
        taskContainer = findViewById(R.id.taskContainer);

        factText = findViewById(R.id.factText);
        impressionCountText = findViewById(R.id.impressionCountText);
        impressionTargetText = findViewById(R.id.impressionTargetText);

        USER_ACCOUNT = Helper.getUserAccount(this);
        adImpressionCount = Helper.getSuccessImpressionCount(this);

        factText.setTypeface(Typeface.createFromAsset(this.getAssets(), "kalpurush.ttf"));
        impressionCountText.setTypeface(Typeface.createFromAsset(this.getAssets(), "kalpurush.ttf"));
        impressionTargetText.setTypeface(Typeface.createFromAsset(this.getAssets(), "kalpurush.ttf"));

        taskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View p1) {
                if (interstitialAd.isAdReadyToDisplay()) {
                    adReloadTrial = 0;
                    interstitialAd.show();
                } else {
                    SHOW_AD_AUTOMATICALLY = true;
                    loadAdlovineAds();
                    showLoader();
                }
            }
        });

        fetchTaskData(USER_ACCOUNT);
    }


    private void loadAdlovineAds() {
        AppLovinSdk.getInstance(this).getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, new AppLovinAdLoadListener() {
            @Override
            public void adReceived(AppLovinAd ad) {
                loadedAd = ad;
                if (SHOW_AD_AUTOMATICALLY) {
                    if (interstitialAd.isAdReadyToDisplay()) {
                        interstitialAd.show();
                        hideLoader();
                    }
                }
            }

            @Override
            public void failedToReceiveAd(int errorCode) {
                loadAdlovineAds();
                if (adReloadTrial < 4) {
                    adReloadTrial++;
                    return;
                }
                Toast("Ad failed to load");

            }
        });
    }

    @Override
    public void adClicked(AppLovinAd ad) {
        AD_CLICKED = true;
        if (!isClaimEnabled()) {
            addInvalidClick(USER_ACCOUNT);
            countDownTimer.cancel();
        }else {
            clickTime();
        }

    }

    @Override
    public void adDisplayed(AppLovinAd ad) {
        AD_CLICKED = false;
        AD_VIEW_SUCCESS = false;
        SHOW_AD_AUTOMATICALLY = false;

        if (!isClaimEnabled()) {
            countDownTimer = new CountDownTimer(IMPRESSION_TIME, 500) {
                public void onTick(long millisUntilFinished) {
                    Toast("Wait " + millisUntilFinished / 500);
                }

                public void onFinish() {
                    AD_VIEW_SUCCESS = true;
                    Toast("Close this Ad");
                }
            }.start();
        } else {
            Toast("Click this Ad");
        }


    }

    @Override
    public void adHidden(AppLovinAd ad) {
        if (!isClaimEnabled()) {
            countDownTimer.cancel();
            if (AD_VIEW_SUCCESS) {
                onViewSuccess();
            } else {
                sweetAlertDialog("You must wait " + IMPRESSION_TIME / 1000 + " seconds",
                        "WARNING_TYPE",
                        false);
            }
        }
//        else {
//            if (AD_CLICKED) {
//                showLoader();
//                countDownTimer = new CountDownTimer(CLICK_TIME, 1000) {
//                    public void onTick(long millisUntilFinished) {
//                        Toast("Wait " + millisUntilFinished / 1000);
//                    }
//
//                    public void onFinish() {
//                        AD_CLICK_SUCCESS = true;
//                        hideLoader();
//                        Toast("Please wait. Back automatically");
//                        countDownTimer.cancel();
//                        Helper.resetSuccessImpressionCount(TaskActivity.this);
//                        authTask(USER_ACCOUNT);
//                    }
//                }.start();
//            } else {
//                sweetAlertDialog("You must click the ad", "WARNING_TYPE", false);
//            }
//        }
    }


    private void sweetAlertDialog(String dialogMessage, final String dialogType, final Boolean finishActivity) {
        SweetAlertDialog sweetAlert = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        sweetAlert.setTitleText(dialogMessage);
        sweetAlert.setCancelable(false);
        if (dialogType == "WARNING_TYPE") {
            sweetAlert.changeAlertType(SweetAlertDialog.WARNING_TYPE);
        }
        sweetAlert.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlert) {
                if (finishActivity) {
                    finish();
                } else {
                    sweetAlert.dismiss();
                }
            }
        });
        sweetAlert.show();
    }

    public void loadUrl(String mUrl) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(mUrl));
        startActivity(i);
    }

    private void clickTime(){
        if (AD_CLICKED) {
            showLoader();
            countDownTimer = new CountDownTimer(CLICK_TIME, 1000) {
                public void onTick(long millisUntilFinished) {
                    Toast("Wait " + millisUntilFinished / 1000);
                }

                public void onFinish() {
                    AD_CLICK_SUCCESS = true;
                    hideLoader();
                    Toast("Please wait. Back automatically");
                    countDownTimer.cancel();
                    Helper.resetSuccessImpressionCount(TaskActivity.this);
                    authTask(USER_ACCOUNT);
                }
            }.start();
        } else {
            sweetAlertDialog("You must click the ad", "WARNING_TYPE", false);
        }
    }

    private void Toast(String ToastString) {
        Toast.makeText(getApplicationContext(), ToastString, Toast.LENGTH_SHORT).show();
    }

    private void authTask(String user)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = Constants.TASK_AUTH_API_URL;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {

                        startActivity(new Intent(TaskActivity.this, MainActivity.class));
                        finishAffinity();


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                sweetAlertDialog("Network Error", "WARNING_TYPE", true);
            }

        }) {
            @Override
            public Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("user", USER_ACCOUNT);
                params.put("did", Helper.getDeviceId(TaskActivity.this));
                params.put("task-app", Constants.TASK_APP_INDEX);
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", Base64.encodeToString(USER_ACCOUNT.getBytes(), Base64.DEFAULT));
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private void addInvalidClick(String user)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = Constants.INVALID_CLICK_API_URL
                + "?user=" + Helper.getUserAccount(this)
                + "&did=" + Helper.getDeviceId(this)
                + "&" + Constants.EXTRA_PARAMS;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        Intent i = new Intent(TaskActivity.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                sweetAlertDialog("Network Error", "WARNING_TYPE", true);
            }
        });
        queue.add(stringRequest);
    }

    private void fetchTaskData(String user) {
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = Constants.TASK_DATA_API_URL
                + "?user=" + Helper.getUserAccount(this)
                + "&did=" + Helper.getDeviceId(this)
                + "&" + Constants.EXTRA_PARAMS;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject data = new JSONObject(response);

                            if (!data.getBoolean("success")) {
                                sweetAlertDialog(data.getString("message"), "WARNING_TYPE", true);
                                return;
                            }

                            if (!data.isNull("vpn_required")) {
                                if (!data.getBoolean("vpn_required") && is_VPN_connected()) {
                                    sweetAlertDialog("VPN not allowed", "WARNING_TYPE", true);
                                    return;
                                }

                                if (data.getBoolean("vpn_required") && !is_VPN_connected()) {
                                    sweetAlertDialog("Connect VPN", "WARNING_TYPE", true);
                                    return;
                                }
                            }

                            VIEW_TARGET = data.getInt("view_target");

                            IMPRESSION_TIME = data.getInt("impression_time") * 1000;
                            CLICK_TIME = data.getInt("click_time") * 1000;
                            BUTTON_CT_TIME = data.getInt("button_timer") * 1000;

                            setText();
                            hideLoader();

                            loadAdlovineAds();

                        } catch (JSONException e) {
                            Toast(e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sweetAlertDialog("Network error!", "WARNING_TYPE", true);
            }
        });
        queue.add(stringRequest);
    }

    public Boolean is_VPN_connected() {
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN).isConnectedOrConnecting();
    }

    private Boolean isClaimEnabled() {
        return adImpressionCount >= VIEW_TARGET;
    }

    private void setText() {
        impressionCountText.setText(Helper.getBangla(String.valueOf(adImpressionCount)));
        impressionTargetText.setText("/" + Helper.getBangla(String.valueOf(VIEW_TARGET)));
        factText.setText(Facts.getFactByIndex(adImpressionCount));
        if (isClaimEnabled()) {
            taskButton.setText("Claim");
            return;
        }
        taskButton.setText("Next");
    }

    private void onViewSuccess() {
        adImpressionCount = adImpressionCount + 1;
        Helper.setSuccessImpressionCount(this, adImpressionCount);
        setText();
        startButttonTimer(BUTTON_CT_TIME);
        completeAlert();
    }

    public void completeAlert() {
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setContentText("Well Done")
                .setConfirmText("Go Ahead")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (interstitialAd.isAdReadyToDisplay()) {
                            interstitialAd.show();
                            sDialog.dismiss();
                        } else {
                            sDialog.dismiss();
                        }

                    }
                })
                .show();
    }


    private void startButttonTimer(int second) {
        if (second < 3) return;
        CountDownTimer cTimer = new CountDownTimer(second, 1000) {
            public void onTick(long millisUntilFinished) {
                taskButton.setEnabled(false);
                taskButton.setText("Wait " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                taskButton.setEnabled(true);
                if (isClaimEnabled()) {
                    taskButton.setText("Claim");
                    return;
                }
                taskButton.setText("Next");
            }
        }.start();
    }

    private void showLoader() {
        loader.setVisibility(View.VISIBLE);
        taskContainer.setVisibility(View.GONE);
    }

    private void hideLoader() {
        loader.setVisibility(View.GONE);
        taskContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
