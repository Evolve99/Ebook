package com.book.mmbookstore.database.pref;

import static com.book.mmbookstore.Config.DEFAULT_BOOK_COLUMNS_COUNT;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPref {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Get first time login from sharedPreferences
    public Boolean getFirstTimeLogin() {
        return sharedPreferences.getBoolean("first_time_login", true);
    }

    // Set first time login
    public void setFirstTimeLogin(Boolean isFirstTimeLogin) {
        editor.putBoolean("first_time_login", isFirstTimeLogin);
        editor.commit();
    }

    public Boolean getIsDarkTheme() {
        return sharedPreferences.getBoolean("theme", false);
    }

    public void setIsDarkTheme(Boolean isDarkTheme) {
        editor.putBoolean("theme", isDarkTheme);
        editor.apply();
    }

    public void setLastReadPage(boolean lastReadPage) {
        editor.putBoolean("last_read_page", lastReadPage);
        editor.commit();
    }

    public boolean isLastReadPage() {
        return sharedPreferences.getBoolean("last_read_page", false);
    }

    public Integer getDisplayPosition(int default_value) {
        return sharedPreferences.getInt("display_position", default_value);
    }

    public void updateDisplayPosition(int position) {
        editor.putInt("display_position", position);
        editor.apply();
    }

    public Integer getBookColumnCount() {
        return sharedPreferences.getInt("column_count", DEFAULT_BOOK_COLUMNS_COUNT);
    }

    public void setBookColumnCount(int columnCount) {
        editor.putInt("column_count", columnCount);
        editor.apply();
    }

    public void saveConfig(String api_url, String application_id) {
        editor.putString("api_url", api_url);
        editor.putString("application_id", application_id);
        editor.apply();
    }

    public String getApiUrl() {
        return sharedPreferences.getString("api_url", "https://solodroid.net/demo/android_ebook_app");
    }

    public String getApplicationId() {
        return sharedPreferences.getString("application_id", "com.app.androidebookapp");
    }

    public void saveCredentials(String fcmNotificationTopic, String onesignalAppId, String privacyPolicy, String moreAppsUrl, String bookSort, String bookOrder, String storySort, String storyOrder) {
        editor.putString("fcm_notification_topic", fcmNotificationTopic);
        editor.putString("onesignal_app_id", onesignalAppId);
        editor.putString("privacy_policy", privacyPolicy);
        editor.putString("more_apps_url", moreAppsUrl);
        editor.putString("book_sort", bookSort);
        editor.putString("book_order", bookOrder);
        editor.putString("story_sort", storySort);
        editor.putString("story_order", storyOrder);
        editor.apply();
    }

    public String getFcmNotificationTopic() {
        return sharedPreferences.getString("fcm_notification_topic", "your_recipes_app_topic");
    }

    public String getOneSignalAppId() {
        return sharedPreferences.getString("onesignal_app_id", "0");
    }

    public String getPrivacyPolicy() {
        return sharedPreferences.getString("privacy_policy", "");
    }

    public String getMoreAppsUrl() {
        return sharedPreferences.getString("more_apps_url", "https://play.google.com/store/apps/developer?id=Solodroid");
    }

    public String getBookSort() {
        return sharedPreferences.getString("book_sort", "book_id");
    }

    public String getBookOrder() {
        return sharedPreferences.getString("book_order", "DESC");
    }

    public String getStorySort() {
        return sharedPreferences.getString("story_sort", "story_title");
    }

    public String getStoryOrder() {
        return sharedPreferences.getString("story_order", "ASC");
    }

    public Integer getInAppReviewToken() {
        return sharedPreferences.getInt("in_app_review_token", 0);
    }

    public void updateInAppReviewToken(int value) {
        editor.putInt("in_app_review_token", value);
        editor.apply();
    }

}
