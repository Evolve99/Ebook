package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.LOCALHOST_ADDRESS;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import com.book.mmbookstore.Config;
import com.book.mmbookstore.callback.CallbackSetting;
import com.book.mmbookstore.model.Setting;
import com.book.mmbookstore.rest.RestAdapter;
import com.book.mmbookstore.util.Tools;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;
import com.solodroid.ads.sdk.format.AppOpenAd;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApplication extends Application {

    public static final String TAG = "MyApplication";
    private static MyApplication mInstance;
    String message = "";
    String bigPicture = "";
    String title = "";
    String link = "";
    String postId = "";
    String uniqueId = "";
    FirebaseAnalytics mFirebaseAnalytics;
    private AppOpenAd appOpenAdManager;
    Call<CallbackSetting> callbackCall = null;
    Setting setting;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        MobileAds.initialize(this, initializationStatus -> {
        });
        appOpenAdManager = new AppOpenAd.Builder(this).build();
        initNotification();
    }

    public void initNotification() {
        OneSignal.disablePush(false);
        Log.d(TAG, "OneSignal Notification is enabled");

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        requestConfig();

        OneSignal.setNotificationOpenedHandler(
                result -> {
                    title = result.getNotification().getTitle();
                    message = result.getNotification().getBody();
                    bigPicture = result.getNotification().getBigPicture();
                    Log.d(TAG, title + ", " + message + ", " + bigPicture);
                    try {
                        uniqueId = result.getNotification().getAdditionalData().getString("unique_id");
                        postId = result.getNotification().getAdditionalData().getString("post_id");
                        link = result.getNotification().getAdditionalData().getString("link");
                        Log.d(TAG, postId + ", " + uniqueId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("unique_id", uniqueId);
                    intent.putExtra("post_id", postId);
                    intent.putExtra("title", title);
                    intent.putExtra("link", link);
                    startActivity(intent);
                });

        OneSignal.unsubscribeWhenNotificationsAreDisabled(true);
    }

    private void requestConfig() {
        String decode = Tools.decodeBase64(Config.SERVER_KEY);
        String data = Tools.decrypt(decode);
        String[] results = data.split("_applicationId_");
        String apiUrl = results[0].replace("http://localhost", LOCALHOST_ADDRESS);

        callbackCall = RestAdapter.createAPI(apiUrl).getSettings(Config.REST_API_KEY);
        callbackCall.enqueue(new Callback<CallbackSetting>() {
            public void onResponse(@NonNull Call<CallbackSetting> call, @NonNull Response<CallbackSetting> response) {
                CallbackSetting resp = response.body();
                if (resp != null) {
                    setting = resp.setting;
                    FirebaseMessaging.getInstance().subscribeToTopic(setting.fcm_notification_topic);
                    OneSignal.setAppId(setting.onesignal_app_id);
                    Log.d(TAG, "FCM Subscribe topic : " + setting.fcm_notification_topic);
                    Log.d(TAG, "OneSignal App ID : " + setting.onesignal_app_id);
                }
            }

            public void onFailure(@NonNull Call<CallbackSetting> call, @NonNull Throwable th) {
                Log.e(TAG, "initialize failed");
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    public AppOpenAd getAppOpenAdManager() {
        return this.appOpenAdManager;
    }

}
