package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.BOOKS_2_COLUMNS;
import static com.book.mmbookstore.util.Constant.BOOKS_3_COLUMNS;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.book.mmbookstore.BuildConfig;
import com.book.mmbookstore.R;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.util.Tools;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.DecimalFormat;

public class ActivitySettings extends AppCompatActivity {

    ImageView btn_clear_cache;
    TextView txt_cache_size;
    Toolbar toolbar;
    SwitchMaterial switch_theme;
    private String single_choice_selected;
    SharedPref sharedPref;
    LinearLayout parentView;
    Tools tools;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_settings);
        tools = new Tools(this);
        sharedPref = new SharedPref(this);

        toolbar = findViewById(R.id.toolbar);
        tools.setupToolbar(this, toolbar, getString(R.string.menu_settings), true);
        tools.setNavigation();

        parentView = findViewById(R.id.parent_view);
        switch_theme = findViewById(R.id.switch_theme);
        switch_theme.setChecked(sharedPref.getIsDarkTheme());
        switch_theme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.e("INFO", "" + isChecked);
            sharedPref.setIsDarkTheme(isChecked);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.btn_switch_theme).setOnClickListener(v -> {
            if (switch_theme.isChecked()) {
                sharedPref.setIsDarkTheme(false);
                switch_theme.setChecked(false);
            } else {
                sharedPref.setIsDarkTheme(true);
                switch_theme.setChecked(true);
            }
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }, 200);
        });

        final TextView txt_wallpaper_columns = findViewById(R.id.txt_wallpaper_columns);
        if (sharedPref.getBookColumnCount() == BOOKS_2_COLUMNS) {
            txt_wallpaper_columns.setText(R.string.option_menu_books_2_columns);
        } else if (sharedPref.getBookColumnCount() == BOOKS_3_COLUMNS) {
            txt_wallpaper_columns.setText(R.string.option_menu_books_3_columns);
        }
        findViewById(R.id.btn_wallpaper_columns).setOnClickListener(v -> {
            String[] items = getResources().getStringArray(R.array.dialog_books_columns);
            int itemSelected;
            if (sharedPref.getBookColumnCount() == BOOKS_3_COLUMNS) {
                itemSelected = sharedPref.getDisplayPosition(1);
                single_choice_selected = items[sharedPref.getDisplayPosition(1)];
            } else {
                itemSelected = sharedPref.getDisplayPosition(0);
                single_choice_selected = items[sharedPref.getDisplayPosition(0)];
            }

            new AlertDialog.Builder(ActivitySettings.this)
                    .setTitle(getString(R.string.title_setting_display_wallpaper))
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                    .setPositiveButton(R.string.option_ok, (dialogInterface, i) -> {
                        if (single_choice_selected.equals(getResources().getString(R.string.option_menu_books_2_columns))) {
                            if (sharedPref.getBookColumnCount() != BOOKS_2_COLUMNS) {
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                sharedPref.setBookColumnCount(BOOKS_2_COLUMNS);
                                txt_wallpaper_columns.setText(R.string.option_menu_books_2_columns);
                            }
                            sharedPref.updateDisplayPosition(0);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.option_menu_books_3_columns))) {
                            if (sharedPref.getBookColumnCount() != BOOKS_3_COLUMNS) {
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                sharedPref.setBookColumnCount(BOOKS_3_COLUMNS);
                                txt_wallpaper_columns.setText(R.string.option_menu_books_3_columns);
                            }
                            sharedPref.updateDisplayPosition(1);
                        }
                        dialogInterface.dismiss();
                    })
                    .show();
        });

        findViewById(R.id.btn_notification).setOnClickListener(v -> {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID);
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", BuildConfig.APPLICATION_ID);
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            startActivity(intent);
        });

        txt_cache_size = findViewById(R.id.txt_cache_size);
        initializeCache();

        btn_clear_cache = findViewById(R.id.btn_clear_cache);
        btn_clear_cache.setOnClickListener(view -> clearCache());

        findViewById(R.id.lyt_clear_cache).setOnClickListener(v -> clearCache());

        findViewById(R.id.btn_about).setOnClickListener(view -> aboutDialog());

        findViewById(R.id.btn_privacy_policy).setOnClickListener(v -> {
            AlertDialog alert = new AlertDialog.Builder(ActivitySettings.this)
                    .setTitle(R.string.title_setting_privacy)
                    .setMessage(Html.fromHtml(sharedPref.getPrivacyPolicy()))
                    .setPositiveButton(R.string.option_ok, null)
                    .show();
            TextView textView = alert.findViewById(android.R.id.message);
            Typeface typeface = ResourcesCompat.getFont(getApplicationContext(), R.font.custom_font);
            assert textView != null;
            textView.setTypeface(typeface);
        });

    }

    private void clearCache() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ActivitySettings.this);
        dialog.setMessage(R.string.msg_clear_cache);
        dialog.setPositiveButton(R.string.option_yes, (dialogInterface, i) -> {

            FileUtils.deleteQuietly(getCacheDir());
            FileUtils.deleteQuietly(getExternalCacheDir());

            final ProgressDialog progressDialog = new ProgressDialog(ActivitySettings.this);
            progressDialog.setTitle(R.string.msg_clearing_cache);
            progressDialog.setMessage(getString(R.string.msg_please_wait));
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                txt_cache_size.setText(getString(R.string.sub_setting_clear_cache_start) + " 0 Bytes " + getString(R.string.sub_setting_clear_cache_end));
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.msg_cache_cleared), Snackbar.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }, 3000);

        });
        dialog.setNegativeButton(R.string.option_cancel, null);
        dialog.show();
    }

    private void initializeCache() {
        txt_cache_size.setText(getString(R.string.sub_setting_clear_cache_start) + " " + readableFileSize((0 + getDirSize(getCacheDir())) + getDirSize(getExternalCacheDir())) + " " + getString(R.string.sub_setting_clear_cache_end));
    }

    public long getDirSize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file != null && file.isDirectory()) {
                size += getDirSize(file);
            } else if (file != null && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0 Bytes";
        }
        String[] units = new String[]{"Bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10((double) size) / Math.log10(1024.0d));
        StringBuilder stringBuilder = new StringBuilder();
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        double d = (double) size;
        double pow = Math.pow(1024.0d, (double) digitGroups);
        Double.isNaN(d);
        stringBuilder.append(decimalFormat.format(d / pow));
        stringBuilder.append(" ");
        stringBuilder.append(units[digitGroups]);
        return stringBuilder.toString();
    }

    public void aboutDialog() {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(ActivitySettings.this);
        View view = layoutInflaterAndroid.inflate(R.layout.dialog_about, null);

        TextView txt_app_version = view.findViewById(R.id.txt_app_version);
        txt_app_version.setText(getString(R.string.msg_about_version) + " " + BuildConfig.VERSION_NAME);

        final AlertDialog.Builder alert = new AlertDialog.Builder(ActivitySettings.this);
        alert.setView(view);
        alert.setPositiveButton(R.string.option_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
