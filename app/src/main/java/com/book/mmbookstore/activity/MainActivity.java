package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.BANNER_HOME;
import static com.book.mmbookstore.util.Constant.INTERSTITIAL_BOOK_LIST;

import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.book.mmbookstore.BuildConfig;
import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.database.pref.AdsPref;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.fragment.FragmentBook;
import com.book.mmbookstore.fragment.FragmentBookmark;
import com.book.mmbookstore.fragment.FragmentCategory;
import com.book.mmbookstore.util.AdsManager;
import com.book.mmbookstore.util.AppBarLayoutBehavior;
import com.book.mmbookstore.util.RtlViewPager;
import com.book.mmbookstore.util.Tools;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ViewPager viewPager;
    private RtlViewPager viewPagerRTL;
    BottomNavigationView navigation;
    private long exitTime = 0;
    Toolbar toolbar;
    SharedPref sharedPref;
    AdsPref adsPref;
    CoordinatorLayout parentView;
    AdsManager adsManager;
    Tools tools;
    MenuItem prevMenuItem;
    int PAGER_NUMBER = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_main);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        tools = new Tools(this);
        tools.setNavigation();

        initComponent();

        adsManager = new AdsManager(this);
        adsManager.initializeAd();
        adsManager.updateConsentStatus();
        adsManager.loadBannerAd(BANNER_HOME);
        adsManager.loadInterstitialAd(INTERSTITIAL_BOOK_LIST, adsPref.getInterstitialAdInterval());

        Tools.notificationOpenHandler(this, getIntent());
        inAppReview();

    }

    public void showInterstitialAd() {
        adsManager.showInterstitialAd();
    }

    public void showSnackBar(String msg) {
        Snackbar.make(parentView, msg, Snackbar.LENGTH_SHORT).show();
    }

    public void initComponent() {

        parentView = findViewById(R.id.tab_coordinator_layout);

        AppBarLayout appBarLayout = findViewById(R.id.tab_appbar_layout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        toolbar = findViewById(R.id.toolbar);
        tools.setupToolbar(this, toolbar, getString(R.string.app_name), false);
        if (!sharedPref.getIsDarkTheme()) {
            toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        } else {
            toolbar.getContext().setTheme(R.style.ThemeOverlay_AppCompat_Dark);
        }

        navigation = findViewById(R.id.navigation);
        navigation.getMenu().clear();
        navigation.inflateMenu(R.menu.menu_navigation);
        navigation.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);

        viewPager = findViewById(R.id.viewPager);
        viewPagerRTL = findViewById(R.id.viewPagerRTL);

        if (Config.ENABLE_RTL_MODE) {
            setupViewPagerRTL();
            viewPager.setVisibility(View.GONE);
            viewPagerRTL.setVisibility(View.VISIBLE);
        } else {
            setupViewPager();
            viewPager.setVisibility(View.VISIBLE);
            viewPagerRTL.setVisibility(View.GONE);
        }

    }

    private void setupViewPager() {
        viewPager.setAdapter(new BottomNavigationAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(PAGER_NUMBER);
        navigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_recent) {
                viewPager.setCurrentItem(0);
            } else if (itemId == R.id.navigation_category) {
                viewPager.setCurrentItem(1);
            } else if (itemId == R.id.navigation_favorite) {
                viewPager.setCurrentItem(2);
            } else {
                viewPager.setCurrentItem(0);
            }
            return false;
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    navigation.getMenu().getItem(0).setChecked(false);
                }
                navigation.getMenu().getItem(position).setChecked(true);
                prevMenuItem = navigation.getMenu().getItem(position);

                int currentItem = viewPager.getCurrentItem();
                if (currentItem == 0) {
                    toolbar.setTitle(getResources().getString(R.string.app_name));
                } else if (currentItem == 1) {
                    toolbar.setTitle(getResources().getString(R.string.title_nav_category));
                } else if (currentItem == 2) {
                    toolbar.setTitle(getResources().getString(R.string.title_nav_bookmark));
                } else {
                    toolbar.setTitle(getResources().getString(R.string.app_name));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setupViewPagerRTL() {
        viewPagerRTL.setAdapter(new BottomNavigationAdapter(getSupportFragmentManager()));
        viewPagerRTL.setOffscreenPageLimit(PAGER_NUMBER);
        navigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_recent) {
                viewPagerRTL.setCurrentItem(0);
            } else if (itemId == R.id.navigation_category) {
                viewPagerRTL.setCurrentItem(1);
            } else if (itemId == R.id.navigation_favorite) {
                viewPagerRTL.setCurrentItem(2);
            } else {
                viewPagerRTL.setCurrentItem(0);
            }
            return false;
        });
        viewPagerRTL.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    navigation.getMenu().getItem(0).setChecked(false);
                }
                navigation.getMenu().getItem(position).setChecked(true);
                prevMenuItem = navigation.getMenu().getItem(position);

                int currentItem = viewPagerRTL.getCurrentItem();
                if (currentItem == 0) {
                    toolbar.setTitle(getResources().getString(R.string.app_name));
                } else if (currentItem == 1) {
                    toolbar.setTitle(getResources().getString(R.string.title_nav_category));
                } else if (currentItem == 2) {
                    toolbar.setTitle(getResources().getString(R.string.title_nav_bookmark));
                } else {
                    toolbar.setTitle(getResources().getString(R.string.app_name));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public class BottomNavigationAdapter extends FragmentPagerAdapter {

        public BottomNavigationAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new FragmentBook();
                case 1:
                    return new FragmentCategory();
                case 2:
                    return new FragmentBookmark();
            }
            return null;
        }

        @Override
        public int getCount() {
            return PAGER_NUMBER;
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(getApplicationContext(), ActivitySettings.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_rate) {
            final String package_name = BuildConfig.APPLICATION_ID;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + package_name)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + package_name)));
            }
        } else if (item.getItemId() == R.id.menu_more) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getMoreAppsUrl())));
        } else if (item.getItemId() == R.id.menu_share) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_about) {
            aboutDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    public void aboutDialog() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View view = inflater.inflate(R.layout.dialog_about, null);

        TextView txtAppVersion = view.findViewById(R.id.txt_app_version);
        txtAppVersion.setText(String.format("%s %s", getString(R.string.msg_about_version), BuildConfig.VERSION_NAME));

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setView(view);
        alert.setPositiveButton(R.string.option_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() != 0) {
            viewPager.setCurrentItem((0), true);
        } else {
            exitApp();
        }
    }

    public void exitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            showSnackBar(getString(R.string.press_again_to_exit));
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> {
                                Log.d(TAG, "In-App Review Success");
                            }
                    ).addOnFailureListener(failure -> {
                        Log.d(TAG, "In-App Review Rating Failed");
                    });
                }
            }).addOnFailureListener(failure -> Log.d("In-App Review", "In-App Request Failed " + failure));
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
    }

}
