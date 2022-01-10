package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.BANNER_READING_PAGE;
import static com.book.mmbookstore.util.Constant.EXTRA_OBJECT;

import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.adapter.AdapterStoryDetail;
import com.book.mmbookstore.callback.CallbackStory;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.database.room.AppDatabase;
import com.book.mmbookstore.database.room.DAO;
import com.book.mmbookstore.fragment.FragmentStory;
import com.book.mmbookstore.model.Book;
import com.book.mmbookstore.model.Story;
import com.book.mmbookstore.rest.ApiInterface;
import com.book.mmbookstore.rest.RestAdapter;
import com.book.mmbookstore.util.AdsManager;
import com.book.mmbookstore.util.InputFilterIntRange;
import com.book.mmbookstore.util.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityStoryDetail extends AppCompatActivity {

    public static final String TAG = "ActivityStoryDetail";
    private Call<CallbackStory> callbackCall = null;
    SharedPref sharedPref;
    Tools tools;
    Book book;
    private int postTotal = 0;
    private int failedPage = 0;
    private ShimmerFrameLayout lytShimmer;
    ViewPager2 viewPager2;
    CoordinatorLayout parentView;
    RelativeLayout lytPage;
    TextView txtPage;
    AdsManager adsManager;
    AppBarLayout lytTop;
    Toolbar toolbar;
    LinearLayout lytBottom;
    AdapterStoryDetail adapterStory;
    private boolean flag_read_later;
    DAO db;
    ImageButton btnBookmark;
    int savedReadingPages = 0;
    int lastReadPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_story_detail);
        db = AppDatabase.getDb(this).get();

        book = (Book) getIntent().getSerializableExtra(EXTRA_OBJECT);

        if (db.getBookmark(book.book_id) != null) {
            Book page = db.getBookmark(book.book_id);
            savedReadingPages = page.page_position;
            Log.d(TAG, "last page visited : " + savedReadingPages);
        }

        sharedPref = new SharedPref(this);
        tools = new Tools(this);
        tools.setNavigation();
        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_READING_PAGE);

        adapterStory = new AdapterStoryDetail(this, new ArrayList<>());

        lytTop = findViewById(R.id.appBarLayout);
        lytBottom = findViewById(R.id.lytBottom);

        toolbar = findViewById(R.id.toolbar);
        parentView = findViewById(R.id.coordinatorLayout);
        lytShimmer = findViewById(R.id.shimmerViewContainer);
        viewPager2 = findViewById(R.id.viewPager);
        lytPage = findViewById(R.id.lytPage);
        txtPage = findViewById(R.id.txtPage);
        btnBookmark = findViewById(R.id.btnBookmark);

        refreshBookmark();
        tools.setupToolbar(this, toolbar, "", true);
        setupToolbar();
        openStoryList();

        requestAction(1);
    }

    private void openStoryList() {
        findViewById(R.id.btnStoryList).setOnClickListener(v -> {
            FragmentStory fragmentStory = new FragmentStory();
            Bundle args = new Bundle();
            args.putString("book_id", book.book_id);
            args.putString("book_name", book.book_name);
            fragmentStory.setArguments(args);

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.add(android.R.id.content, fragmentStory).addToBackStack("story");
            transaction.commit();
        });
    }

    private void setupToolbar() {
        ((TextView) findViewById(R.id.toolbar_title)).setText(book.book_name);
        findViewById(R.id.btnJumpPage).setOnClickListener(v -> showPageDialog());
        findViewById(R.id.btnShare).setOnClickListener(v -> Tools.shareContent(ActivityStoryDetail.this, book.book_name));
    }

    private void requestListPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getApiUrl());
        callbackCall = apiInterface.getStories(book.book_id, page_no, 1000, sharedPref.getStorySort(), sharedPref.getStoryOrder(), Config.REST_API_KEY);

        callbackCall.enqueue(new Callback<CallbackStory>() {
            @Override
            public void onResponse(Call<CallbackStory> call, Response<CallbackStory> response) {
                CallbackStory resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    postTotal = resp.count_total;
                    displayStories(resp.stories);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackStory> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void displayStories(final List<Story> stories) {
        viewPager2.setAdapter(adapterStory);
        viewPager2.setOffscreenPageLimit(stories.size());
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                txtPage.setText(String.format("%s %s / %s", "", position + 1, postTotal));
                onBookmarkPage(position);
                lastReadPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        adapterStory.insertData(stories);
        if (stories.size() == 0) {
            showNoItemView(true);
        }

        new Handler().postDelayed(() -> {
            viewPager2.setVisibility(View.VISIBLE);
            lytPage.setVisibility(View.VISIBLE);
            swipeProgress(false);
        }, 2000);

        new Handler().postDelayed(() -> viewPager2.setCurrentItem(savedReadingPages), 100);

    }

    private void onBookmarkPage(int pagePosition) {
        btnBookmark.setOnClickListener(v -> {
            String str;
            if (flag_read_later) {
                db.deleteBookmark(book.book_id);
                str = getString(R.string.bookmark_removed);
            } else {
                db.insertBookmark(
                        book.book_id,
                        book.category_id,
                        book.book_name,
                        book.book_image,
                        book.author,
                        book.type,
                        book.pdf_name,
                        pagePosition,
                        System.currentTimeMillis()
                );
                str = getString(R.string.bookmark_added);
            }
            Snackbar.make(parentView, str, Snackbar.LENGTH_SHORT).show();
            refreshBookmark();
        });
    }

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        swipeProgress(false);
        if (tools.networkCheck()) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        }
        new Handler().postDelayed(() -> requestListPostApi(page_no), 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lytShimmer.stopShimmer();
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failedPage));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
        if (show) {
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
        }
    }

    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count != 0) {
            getSupportFragmentManager().popBackStack();
            Log.d(TAG, "close dialog fragment");
        } else {
            super.onBackPressed();
            updateLastPageRead();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            super.onBackPressed();
            updateLastPageRead();
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private void updateLastPageRead() {
        flag_read_later = db.getBookmark(book.book_id) != null;
        if (flag_read_later) {
            db.updateBookmark(book.book_id, lastReadPage);
            Log.d(TAG, "update last page bookmarked");
        }
    }

    public void showPageDialog() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityStoryDetail.this);
        LayoutInflater inflater = LayoutInflater.from(ActivityStoryDetail.this);
        View view = inflater.inflate(R.layout.dialog_jump_page, null);

        LinearLayout lytDialogHeader = view.findViewById(R.id.lytDialogHeader);
        LinearLayout lytDialogContent = view.findViewById(R.id.lytDialogContent);
        if (sharedPref.getIsDarkTheme()) {
            lytDialogHeader.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            lytDialogContent.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        }

        TextView txtInputPageNumber = view.findViewById(R.id.txtInputPageNumber);
        txtInputPageNumber.setText(String.format("%s %s - %s", getString(R.string.input_page_number), "1", postTotal));

        EditText edtPageNumber = view.findViewById(R.id.edtPageNumber);
        edtPageNumber.setHint(String.format("%s - %s", "1", postTotal));

        edtPageNumber.requestFocus();
        tools.showKeyboard(true);

        InputFilterIntRange rangeFilter = new InputFilterIntRange(1, postTotal);
        edtPageNumber.setFilters(new InputFilter[]{rangeFilter});
        edtPageNumber.setOnFocusChangeListener(rangeFilter);

        dialog.setView(view);
        dialog.setCancelable(false);

        AlertDialog alertDialog = dialog.create();

        TextView btnPositive = view.findViewById(R.id.btnPositive);
        btnPositive.setOnClickListener(v -> new Handler().postDelayed(() -> {
            if (!edtPageNumber.getText().toString().equals("")) {
                int pageNumber = (Integer.parseInt(edtPageNumber.getText().toString()) - 1);
                new Handler().postDelayed(() -> viewPager2.setCurrentItem(pageNumber), 200);
                tools.showKeyboard(false);
                alertDialog.dismiss();
            } else {
                Snackbar.make(parentView, getString(R.string.msg_input_page), Snackbar.LENGTH_SHORT).show();
            }
        }, 300));

        TextView btnNegative = view.findViewById(R.id.btnNegative);
        btnNegative.setOnClickListener(v -> new Handler().postDelayed(() -> {
            tools.showKeyboard(false);
            alertDialog.dismiss();
        }, 300));

        alertDialog.show();
    }

    public void fullScreenMode(boolean fullscreenMode) {
        tools.fullScreenMode(lytTop, lytBottom, fullscreenMode);
    }

    private void refreshBookmark() {
        flag_read_later = db.getBookmark(book.book_id) != null;
        if (flag_read_later) {
            btnBookmark.setImageResource(R.drawable.ic_bookmark_white);
        } else {
            btnBookmark.setImageResource(R.drawable.ic_bookmark_outline_white);
        }
    }

    public void jumpPage(int position) {
        viewPager2.setCurrentItem(position);
    }

}
