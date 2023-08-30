package com.dev_bd.khanjk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.dev_bd.khanjk.utils.Helper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Random;

import es.dmoral.toasty.Toasty;

public class SpinActivity extends AppCompatActivity implements AppLovinAdDisplayListener, AppLovinAdClickListener {

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (mTimerRunning2) {
            pauseTimer();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mTimerRunning2) {
            pauseTimer();
        }
        super.onBackPressed();
    }


    private long START_TIME_IN_MILLIS = 0;
    SharedPreferences.Editor editor;
    private TextView waitingTV, waitTimeShow;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis;
    private long mEndTime;
    int waitingScore;
    LinearLayout waitingAlert;
    ImageView wheelImage;
    TextView tapBtn, tapWaitingLuckyTimerShow;
    private Random r;
    private int degree = 0, degree_old = 0;
    private static final float FACTOR = 15f;
    private MediaPlayer player;
    int spinScore = 0;
    TextView spinner2Counter, spin2DayLimit;
    private StartAppAd startAppAd = new StartAppAd(this);
    int li;
    BottomSheetDialog bottomSheetDialog;
    private long START_TIME_IN_MILLIS2 = 60000;
    private CountDownTimer mCountDownTimer2;
    private boolean mTimerRunning2;
    private long mTimeLeftInMillis2;
    private long mEndTime2;
    private int clickTime = 0;
    private int waitingTimeStart = 0;

    String USER_ACCOUNT;
    App_Controller app_controller;
    int loadAds = 0;

    private AppLovinInterstitialAdDialog interstitialAd;
    private AppLovinRewardedInterstitialAd rewardedInterstitialAd;
    private AppLovinAd loadedAd;
    String videoAdsPoints;
    String waitingTime;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spin);

        wheelImage = findViewById(R.id.wheelImage2);
        tapBtn = findViewById(R.id.wheelTapBtn2);
        spinner2Counter = findViewById(R.id.spinner2Counter);
        spin2DayLimit = findViewById(R.id.spin2DayLimit);
        waitingAlert = findViewById(R.id.Spin2WaitingAlert);
        waitingTV = findViewById(R.id.spin2WaitingTV_id);
        waitTimeShow = findViewById(R.id.spin2WaitTimeShow);
        tapWaitingLuckyTimerShow = findViewById(R.id.tapWaitingLuckyTimerShow);
        USER_ACCOUNT = Helper.getUserAccount(this);
        app_controller = new App_Controller(this);

        setTitle("Lucky Spin");
        StartAppSDK.init(this, getString(R.string.startapp_app_id), true);
        StartAppAd.disableSplash();
        StartAppAd.disableAutoInterstitial();
        loadSatrtAppAds();

        AppLovinSdk.getInstance(this);
        interstitialAd = AppLovinInterstitialAd.create( AppLovinSdk.getInstance( this ), this );
        interstitialAd.setAdDisplayListener( this );
        interstitialAd.setAdClickListener( this );
        loadAdlovineAds();

        spinner2Counter.setText(app_controller.getSpin1DailyTaskCounter1() + "/" + app_controller.getSpin_limit());
        spin2DayLimit.setText(app_controller.getSpin1DailyTaskLimitCounter1() + "/" + app_controller.getDaliy_task_limit());

        fetchTaskData(USER_ACCOUNT);
        r = new Random();
        li = Integer.parseInt(app_controller.getDaliy_task_limit());

        if (app_controller.getSpin1DailyTaskLimitCounter1() >= li) {
            Toasty.info(SpinActivity.this, "Your Today limit finished", Toasty.LENGTH_LONG).show();
            tapBtn.setEnabled(false);
            return;
        }

        if (app_controller.getSpin1_control().equals("Close")){
            workControlAlert();
        }

        tapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int limit = Integer.parseInt(app_controller.getSpin_limit());
                if (app_controller.getSpin1DailyTaskCounter1() < limit) {
                    StartAppSDK.setTestAdsEnabled(false);
                    startWheel();
                    mTimeLeftInMillis2 = 5000;
                    clickTime = 2;
                } else {
                    popUp();

                }

            }
        });
    }



    private void loadAdlovineAds() {
        AppLovinSdk.getInstance(this).getAdService().loadNextAd(AppLovinAdSize.INTERSTITIAL, new AppLovinAdLoadListener() {
            @Override
            public void adReceived(AppLovinAd ad) {
                loadedAd = ad;
            }
            @Override
            public void failedToReceiveAd(int errorCode) {
                loadAdlovineAds();
            }
        });
    }


    @Override
    public void adClicked(AppLovinAd ad) {

    }

    @Override
    public void adDisplayed(AppLovinAd ad) {

    }

    @Override
    public void adHidden(AppLovinAd ad) {
        loadAdlovineAds();
    }

    private void loadSatrtAppAds() {
        startAppAd.loadAd(StartAppAd.AdMode.AUTOMATIC, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
            }
            @Override
            public void onFailedToReceiveAd(Ad ad) {
                loadSatrtAppAds();
            }
        });

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
                    public void onResponse(String response)
                    {
                        try
                        {
                            JSONObject data = new JSONObject(response);
                            if ( !data.getBoolean("success") ) {
                                sweetAlertDialog( data.getString("message"), "WARNING_TYPE", true );
                            }

                        }
                        catch (JSONException e)
                        {
                            //Toast.makeText(SpinActivity.this, ""+e, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                sweetAlertDialog("Network error!", "WARNING_TYPE", true);
            }
        });
        queue.add(stringRequest);
    }

    private void sweetAlertDialog(String dialogMessage, final String dialogType, final Boolean finishActivity)
    {
        SweetAlertDialog sweetAlert = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        sweetAlert.setTitleText(dialogMessage);
        sweetAlert.setCancelable(false);
        if (dialogType == "WARNING_TYPE")
        {
            sweetAlert.changeAlertType(SweetAlertDialog.WARNING_TYPE);
        }
        sweetAlert.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlert)
            {
                if (finishActivity)
                {
                    finish();
                }
                else
                {
                    sweetAlert.dismiss();
                }
            }
        });
        sweetAlert.show();
    }


    private void authTask(String user)
    {
        final SweetAlertDialog pDialog = new SweetAlertDialog(this,
                SweetAlertDialog.PROGRESS_TYPE);
        pDialog.setTitleText("Please Wait");
        pDialog.setCancelable(false);
        pDialog.show();
        RequestQueue queue = Volley.newRequestQueue(this);
        final String url = Constants.TASK_AUTH_API_URL
                + "?user=" + Helper.getUserAccount(this)
                + "&did=" + Helper.getDeviceId(this)
                + "&" + Constants.EXTRA_PARAMS;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        pDialog.dismiss();
                        app_controller.spin1DailyTaskCounter(0);
                        app_controller.spin1DailyTaskLimitCounter(app_controller.getSpin1DailyTaskLimitCounter1()+1);
                        Intent i = new Intent(SpinActivity.this, MainActivity.class);
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
                //Toast.makeText(SpinActivity.this, "", Toast.LENGTH_SHORT).show();

            }
        });
        queue.add(stringRequest);
    }

    private void startTimer2() {
        mEndTime2 = System.currentTimeMillis() + mTimeLeftInMillis2;
        mCountDownTimer2 = new CountDownTimer(mTimeLeftInMillis2, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis2 = millisUntilFinished;
                updateCountDownText2();
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                mTimerRunning2 = false;
                if (clickTime == 1) {
                    waitingTimeStart++;
                } else if (clickTime == 2) {
                    Toasty.success(SpinActivity.this, "Impression success", Toasty.LENGTH_LONG).show();
                    if (clickTime == 2) {
                        app_controller.spin1DailyTaskCounter(app_controller.getSpin1DailyTaskCounter1() + 1);
                        spinner2Counter.setText(app_controller.getSpin1DailyTaskCounter1() + "/" + app_controller.getSpin_limit());
                    }
                    clickTime = 0;
                } else if (clickTime == 3) {
                    tapBtn.setVisibility(View.VISIBLE);
                    tapWaitingLuckyTimerShow.setVisibility(View.GONE);
                }
                resetTimer2();
            }
        }.start();

        mTimerRunning2 = true;
    }

    private void pauseTimer() {
        mCountDownTimer2.cancel();
        mTimerRunning2 = false;
        clickTime = 0;
        mCountDownTimer2.onFinish();

    }

    private void resetTimer2() {
        mTimeLeftInMillis2 = START_TIME_IN_MILLIS2;
        updateCountDownText2();
    }

    private void updateCountDownText2() {
        int seconds = (int) (mTimeLeftInMillis2 / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d", seconds);
        if (clickTime == 1) {
            Toasty.info(SpinActivity.this, "Wait: " + timeLeftFormatted, Toasty.LENGTH_SHORT).show();
        } else if (clickTime == 2) {
            Toasty.info(SpinActivity.this, "Wait: " + timeLeftFormatted, Toasty.LENGTH_SHORT).show();
        } else if (clickTime == 3) {
            tapWaitingLuckyTimerShow.setText(timeLeftFormatted);
        }
    }

    private void startWheel() {

        {
            degree_old = degree % 360;
            degree = r.nextInt(3600) + 720;
            RotateAnimation animationRotate = new RotateAnimation(degree_old, degree, RotateAnimation.RELATIVE_TO_SELF,
                    0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            animationRotate.setDuration(3600);
            animationRotate.setFillAfter(true);
            animationRotate.setInterpolator(new DecelerateInterpolator());
            animationRotate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (player == null) {
                        player = MediaPlayer.create(SpinActivity.this, R.raw.sound);
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                stopPlayer();
                            }
                        });
                    }
                    player.start();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    stopPlayer();
                    StartAppSDK.setTestAdsEnabled(false);
                    currentNumber(360 - (degree % 360));
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            wheelImage.startAnimation(animationRotate);
        }

    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }


    void popUp() {
        bottomSheetDialog = new BottomSheetDialog(SpinActivity.this,
                R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(SpinActivity.this).inflate(R.layout.category_popup_model,
                (LinearLayout) findViewById(R.id.pendingAlertPopUp_id));
        TextView message1 = bottomSheetView.findViewById(R.id.payAlertmessage);
        TextView message2 = bottomSheetView.findViewById(R.id.payAlertmessage2);
        TextView message3 = bottomSheetView.findViewById(R.id.payAlertmessage3);
        Button payNow = bottomSheetView.findViewById(R.id.payAlertPayNowBtn);
        message1.setText("1) You need to wait at least: 1 min");
        message2.setText("2) You will get: 100 Coins");
        message3.setVisibility(View.GONE);
        payNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mTimerRunning) {
                    bottomSheetDialog.dismiss();
                    clickTime = 1;
                    climAlert();
                } else {
                    Toast.makeText(SpinActivity.this, "You have already done this work", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

    }

    private String currentNumber(int degrees) {
        String text = "";

        if (degrees >= (FACTOR * 0) && degrees <= (FACTOR * 2)) {
            spinScore = 1;
            loadAds();
        }
        if (degrees >= (FACTOR * 2) && degrees <= (FACTOR * 4)) {
            spinScore = 2;
            loadAds();
        }
        if (degrees >= (FACTOR * 4) && degrees <= (FACTOR * 6)) {
            spinScore = 3;
            loadAds();
        }
        if (degrees >= (FACTOR * 6) && degrees <= (FACTOR * 8)) {
            spinScore = 4;
            loadAds();
        }
        if (degrees >= (FACTOR * 8) && degrees <= (FACTOR * 10)) {
            spinScore = 5;
            loadAds();
        }
        if (degrees >= (FACTOR * 10) && degrees <= (FACTOR * 12)) {
            spinScore = 6;
            loadAds();
        }
        if (degrees >= (FACTOR * 12) && degrees <= (FACTOR * 14)) {
            spinScore = 7;
            loadAds();
        }
        if (degrees >= (FACTOR * 14) && degrees <= (FACTOR * 16)) {
            spinScore = 8;
            loadAds();
        }
        if (degrees >= (FACTOR * 16) && degrees <= (FACTOR * 18)) {
            spinScore = 9;
            loadAds();
        }
        if (degrees >= (FACTOR * 18) && degrees <= (FACTOR * 20)) {
            spinScore = 10;
            loadAds();
        }
        if (degrees >= (FACTOR * 20) && degrees <= (FACTOR * 22)) {
            spinScore = 11;
            loadAds();
        }
        if ((degrees >= (FACTOR * 22) && degrees <= (FACTOR * 24))) {
            spinScore = 12;
            loadAds();
        }
        return text;

    }


    private void loadAds() {

        startAppAd.showAd(new AdDisplayListener() {
            @Override
            public void adHidden(Ad ad) {
                loadSatrtAppAds();
                if (clickTime != 1) {
                    tapWaitingLuckyTimerShow.setVisibility(View.VISIBLE);
                    mTimeLeftInMillis2 = 12000;
                    clickTime = 3;
                    startTimer2();
                    completeAlert();
                }
            }

            @Override
            public void adDisplayed(Ad ad) {
                tapBtn.setVisibility(View.GONE);
                if (clickTime == 2) {
                    startTimer2();
                }
            }

            @Override
            public void adClicked(Ad ad) {
                if (clickTime == 1) {
                    resetTimer2();
                    mTimeLeftInMillis2 = 59000;
                    startTimer2();
                    waitingScore++;
                }
            }

            @Override
            public void adNotDisplayed(Ad ad) {
                loadSatrtAppAds();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences prefs = getSharedPreferences("Spin2", MODE_PRIVATE);
        mTimeLeftInMillis = prefs.getLong("millisLeft", START_TIME_IN_MILLIS);
        mTimerRunning = prefs.getBoolean("timerRunning", false);

        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();

            if (mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                //updateCountDownText();
                resetTimer();
            } else {
                waitingScore++;
                startTimer();
            }
        }
        if (waitingTimeStart == 1) {
            authTask(USER_ACCOUNT);
           /* resetTimer();
            startTimer();*/
        }


    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences("Spin2", MODE_PRIVATE);
        editor = prefs.edit();
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);
        editor.apply();

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                waitingScore = 0;
                wheelImage.setVisibility(View.VISIBLE);
                spinner2Counter.setVisibility(View.VISIBLE);
                tapBtn.setVisibility(View.VISIBLE);
                resetTimer();

            }
        }.start();

        mTimerRunning = true;
    }

    private void resetTimer() {
        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText();
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        if (waitingScore >= 1) {
            waitingAlert.setVisibility(View.VISIBLE);
            wheelImage.setVisibility(View.GONE);
            tapBtn.setVisibility(View.GONE);
            spinner2Counter.setVisibility(View.GONE);
            waitTimeShow.setText("Wait: " + timeLeftFormatted);
            waitingTV.setText("Please try after finished time");

            if (clickTime == 1) {
                authTask(USER_ACCOUNT);
                //taskPointAdd(saveUserInfo.getRefer_code(), app_controller.getClick_point());
                clickTime = 100;
            }


        } else {
            waitingAlert.setVisibility(View.GONE);

        }
    }

    private void workControlAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(SpinActivity.this);
        builder.setTitle("Task status")
                .setMessage("This task is stop for few hours.")
                .setCancelable(false)
                .setPositiveButton("Try again later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void climAlert() {
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setContentText("Congratulation")
                .setConfirmText("Get Bonus")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismiss();
                        loadAds();
                    }
                })
                .show();
    }

    public void completeAlert() {
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setContentText("Well Done")
                .setConfirmText("Go Ahead")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        if (interstitialAd.isAdReadyToDisplay()){
                            interstitialAd.show();
                            sDialog.dismiss();
                        }else {
                            sDialog.dismiss();
                        }

                    }
                })
                .show();
    }


}