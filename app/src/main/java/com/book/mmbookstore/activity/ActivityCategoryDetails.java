package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.BANNER_CATEGORY_DETAILS;
import static com.book.mmbookstore.util.Constant.BOOKS_2_COLUMNS;
import static com.book.mmbookstore.util.Constant.BOOKS_3_COLUMNS;
import static com.book.mmbookstore.util.Constant.EXTRA_OBJECT;
import static com.book.mmbookstore.util.Constant.INTERSTITIAL_BOOK_LIST;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.adapter.AdapterBook;
import com.book.mmbookstore.callback.CallbackBook;
import com.book.mmbookstore.database.pref.AdsPref;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.model.Book;
import com.book.mmbookstore.model.Category;
import com.book.mmbookstore.rest.ApiInterface;
import com.book.mmbookstore.rest.RestAdapter;
import com.book.mmbookstore.util.AdsManager;
import com.book.mmbookstore.util.Constant;
import com.book.mmbookstore.util.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityCategoryDetails extends AppCompatActivity {

    Toolbar toolbar;
    private RecyclerView recyclerView;
    private AdapterBook adapterBook;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackBook> callbackCall = null;
    private int postTotal = 0;
    private int failedPage = 0;
    SharedPref sharedPref;
    Tools tools;
    private ShimmerFrameLayout lytShimmer;
    Category category;
    AdsManager adsManager;
    AdsPref adsPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_category_details);

        category = (Category) getIntent().getSerializableExtra(EXTRA_OBJECT);

        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);

        tools = new Tools(this);
        tools.setNavigation();

        adsManager.loadBannerAd(BANNER_CATEGORY_DETAILS);
        adsManager.loadInterstitialAd(INTERSTITIAL_BOOK_LIST, adsPref.getInterstitialAdInterval());

        toolbar = findViewById(R.id.toolbar);
        lytShimmer = findViewById(R.id.shimmerViewContainer);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = findViewById(R.id.recyclerView);

        if (sharedPref.getBookColumnCount() == BOOKS_2_COLUMNS) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_2_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        } else if (sharedPref.getBookColumnCount() == BOOKS_3_COLUMNS) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_3_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        } else {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_2_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        }

        //set data and list adapter
        adapterBook = new AdapterBook(this, recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterBook);

        // on item list clicked
        adapterBook.setOnItemClickListener((v, obj, position) -> {
            tools.onBookClicked(obj);
            adsManager.showInterstitialAd();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
            }
        });

        adapterBook.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterBook.resetListData();
            requestAction(1);
        });

        requestAction(1);
        initShimmerLayout();
        tools.setupToolbar(this, toolbar, category.category_name, true);
    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterBook.getItemCount() - current_page);
        if (postTotal > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterBook.setLoaded();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    private void displayApiResult(final List<Book> books) {
        adapterBook.insertDataWithNativeAd(books);
        swipeProgress(false);
        if (books.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getApiUrl());
        callbackCall = apiInterface.getCategoryDetails(category.category_id, page_no, Constant.BOOKS_PER_PAGE, sharedPref.getBookSort(), sharedPref.getBookOrder(), Config.REST_API_KEY);

        callbackCall.enqueue(new Callback<CallbackBook>() {
            @Override
            public void onResponse(Call<CallbackBook> call, Response<CallbackBook> response) {
                CallbackBook resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    postTotal = resp.count_total;
                    displayApiResult(resp.books);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackBook> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        adapterBook.setLoaded();
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
        } else {
            adapterBook.setLoading();
        }
        new Handler().postDelayed(() -> requestListPostApi(page_no), Constant.DELAY_TIME);
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
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failedPage));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lytShimmer.setVisibility(View.VISIBLE);
            lytShimmer.startShimmer();
        });
    }

    private void initShimmerLayout() {
        View lytShimmerGrid2 = findViewById(R.id.lyt_shimmer_recipes_grid2);
        View lytShimmerGrid3 = findViewById(R.id.lyt_shimmer_recipes_grid3);
        if (sharedPref.getBookColumnCount() == 2) {
            lytShimmerGrid2.setVisibility(View.VISIBLE);
            lytShimmerGrid3.setVisibility(View.GONE);
        } else if (sharedPref.getBookColumnCount() == 3) {
            lytShimmerGrid2.setVisibility(View.GONE);
            lytShimmerGrid3.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (menuItem.getItemId() == R.id.search) {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

}