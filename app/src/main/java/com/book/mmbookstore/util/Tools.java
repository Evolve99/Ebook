package com.book.mmbookstore.util;

import static com.book.mmbookstore.Config.ENABLE_RTL_MODE;
import static com.book.mmbookstore.util.Constant.BOOK_PDF_UPLOAD;
import static com.book.mmbookstore.util.Constant.BOOK_PDF_URL;
import static com.book.mmbookstore.util.Constant.BOOK_STORY;
import static com.book.mmbookstore.util.Constant.EXTRA_OBJECT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.Html;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.book.mmbookstore.BuildConfig;
import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.activity.ActivityPDFView;
import com.book.mmbookstore.activity.ActivityStoryDetail;
import com.book.mmbookstore.activity.ActivityWebView;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.model.Book;

import java.nio.charset.StandardCharsets;

public class Tools {

    private final Activity activity;
    SharedPref sharedPref;

    public Tools(Activity activity) {
        this.activity = activity;
        this.sharedPref = new SharedPref(activity);
    }

    public static void getTheme(Context context) {
        SharedPref sharedPref = new SharedPref(context);
        if (sharedPref.getIsDarkTheme()) {
            context.setTheme(R.style.AppDarkTheme);
        } else {
            context.setTheme(R.style.AppTheme);
        }
    }

    public void setNavigation() {
        if (sharedPref.getIsDarkTheme()) {
            darkNavigation(activity);
        } else {
            lightNavigation(activity);
        }
        getLayoutDirection();
    }

    public void getLayoutDirection() {
        if (ENABLE_RTL_MODE) {
            activity.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
    }

    public static void darkNavigation(Activity activity) {
        activity.getWindow().setNavigationBarColor(ContextCompat.getColor(activity, R.color.colorBackgroundDark));
        activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorBackgroundDark));
        activity.getWindow().getDecorView().setSystemUiVisibility(0);
    }

    public static void lightNavigation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(ContextCompat.getColor(activity, R.color.colorBackgroundLight));
            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    public void fullScreenMode(View lytTop, View lytBottom, boolean on) {
        if (on) {
            lytTop.setVisibility(View.GONE);
            lytTop.animate().translationY(-lytTop.getHeight());
            lytBottom.setVisibility(View.GONE);
            lytBottom.animate().translationY(lytBottom.getHeight());
            hideSystemUI();

        } else {
            lytTop.setVisibility(View.VISIBLE);
            lytTop.animate().translationY(0);
            lytBottom.setVisibility(View.VISIBLE);
            lytBottom.animate().translationY(0);
            showNavigation();
        }
    }

    private void hideSystemUI() {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void showNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    public void showKeyboard(boolean show) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (show) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        } else {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }
    }

    public boolean networkCheck() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                return activeNetworkInfo.isConnected() || activeNetworkInfo.isConnectedOrConnecting();
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void setupToolbar(AppCompatActivity activity, Toolbar toolbar, String title, boolean backButton) {
        activity.setSupportActionBar(toolbar);
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(activity.getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(activity.getResources().getColor(R.color.colorPrimary));
        }
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(backButton);
            activity.getSupportActionBar().setHomeButtonEnabled(backButton);
            activity.getSupportActionBar().setTitle(title);
        }
    }

    public static String decrypt(String code) {
        return decodeBase64(decodeBase64(code));
    }

    public static String decodeBase64(String code) {
        byte[] valueDecoded = Base64.decode(code.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        return new String(valueDecoded);
    }

    public void onBookClicked(Book obj) {
        switch (obj.type) {
            case BOOK_STORY: {
                Intent intent = new Intent(activity, ActivityStoryDetail.class);
                intent.putExtra(EXTRA_OBJECT, obj);
                activity.startActivity(intent);
                break;
            }
            case BOOK_PDF_UPLOAD:
            case BOOK_PDF_URL: {
                Intent intent = new Intent(activity, ActivityPDFView.class);
                intent.putExtra(EXTRA_OBJECT, obj);
                activity.startActivity(intent);
                break;
            }
        }
    }

    public static void notificationOpenHandler(Context context, Intent getIntent) {

        String uniqueId = getIntent.getStringExtra("unique_id");
        String postId = getIntent.getStringExtra("post_id");
        String title = getIntent.getStringExtra("title");
        String link = getIntent.getStringExtra("link");

        if (getIntent.hasExtra("unique_id")) {

            if (link != null && !link.equals("")) {
                Intent intent = new Intent(context, ActivityWebView.class);
                intent.putExtra("title", title);
                intent.putExtra("url", link);
                context.startActivity(intent);
            }

        }

    }

    public static void shareContent(Activity activity, String bookName) {
        String title = Html.fromHtml(activity.getResources().getString(R.string.share_content)).toString();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TITLE, title);
        sendIntent.putExtra(Intent.EXTRA_TEXT, title + "\n\n" + bookName + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
        sendIntent.setType("text/plain");
        activity.startActivity(sendIntent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void displayContent(WebView webView, String htmlData) {
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        webView.setFocusableInTouchMode(false);
        webView.setFocusable(false);

        if (!Config.ENABLE_TEXT_SELECTION) {
            webView.setOnLongClickListener(v -> true);
            webView.setLongClickable(false);
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setDefaultFontSize(Constant.FONT_SIZE_MEDIUM);

        String bgParagraph;
        String mimeType = "text/html; charset=UTF-8";
        String encoding = "utf-8";

        if (sharedPref.getIsDarkTheme()) {
            bgParagraph = "<style type=\"text/css\">body{color: #eeeeee;} a{color:#ffffff; font-weight:bold;}";
        } else {
            bgParagraph = "<style type=\"text/css\">body{color: #212121;} a{color:#1e88e5; font-weight:bold;}";
        }

        String fontStyle = "<style type=\"text/css\">@font-face {font-family: MyFont;src: url(\"file:///android_asset/font/custom_font.ttf\")}body {font-family: MyFont;font-size: medium; text-align: left;}</style>";
        String fontStyleRTL = "<style type=\"text/css\">@font-face {font-family: MyFont;src: url(\"file:///android_asset/font/custom_font.ttf\")}body {font-family: MyFont;font-size: medium; text-align: right;}</style>";

        String textDefault = "<html><head>"
                + fontStyle
                + "<style>img{max-width:100%;height:auto;} figure{max-width:100%;height:auto;} iframe{width:100%;}</style> "
                + bgParagraph
                + "</style></head>"
                + "<body>"
                + htmlData
                + "</body></html>";

        String textRtl = "<html dir='rtl'><head>"
                + fontStyleRTL
                + "<style>img{max-width:100%;height:auto;} figure{max-width:100%;height:auto;} iframe{width:100%;}</style> "
                + bgParagraph
                + "</style></head>"
                + "<body>"
                + htmlData
                + "</body></html>";

        if (Config.ENABLE_RTL_MODE) {
            webView.loadDataWithBaseURL(null, textRtl, mimeType, encoding, null);
        } else {
            webView.loadDataWithBaseURL(null, textDefault, mimeType, encoding, null);
        }
    }

}
