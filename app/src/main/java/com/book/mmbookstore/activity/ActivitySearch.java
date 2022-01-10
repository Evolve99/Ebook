package com.book.mmbookstore.activity;

import static com.book.mmbookstore.util.Constant.BANNER_SEARCH;
import static com.book.mmbookstore.util.Constant.BOOKS_2_COLUMNS;
import static com.book.mmbookstore.util.Constant.BOOKS_3_COLUMNS;
import static com.book.mmbookstore.util.Constant.INTERSTITIAL_BOOK_LIST;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.adapter.AdapterBook;
import com.book.mmbookstore.callback.CallbackBook;
import com.book.mmbookstore.database.pref.AdsPref;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.model.Book;
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

public class ActivitySearch extends AppCompatActivity {

    private EditText et_search;
    private RecyclerView recyclerView;
    private AdapterBook adapterBook;
    private ImageButton bt_clear;
    private Call<CallbackBook> callbackCall = null;
    private int postTotal = 0;
    private int failedPage = 0;
    private ShimmerFrameLayout lyt_shimmer;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;
    Tools tools;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_search);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        tools = new Tools(this);
        tools.setNavigation();

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(BANNER_SEARCH);
        adsManager.loadInterstitialAd(INTERSTITIAL_BOOK_LIST, adsPref.getInterstitialAdInterval());

        toolbar = findViewById(R.id.toolbar);
        et_search = findViewById(R.id.et_search);
        bt_clear = findViewById(R.id.bt_clear);
        bt_clear.setVisibility(View.GONE);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        swipeProgress(false);

        recyclerView = findViewById(R.id.recyclerView);

        if (sharedPref.getBookColumnCount() == BOOKS_2_COLUMNS) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_2_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        } else if (sharedPref.getBookColumnCount() == BOOKS_3_COLUMNS) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_3_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        } else {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_2_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        }

        et_search.addTextChangedListener(textWatcher);

        //set data and list mAdapterSearch
        adapterBook = new AdapterBook(this, recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterBook);

        bt_clear.setOnClickListener(view -> et_search.setText(""));

        et_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchAction(1);
                return true;
            }
            return false;
        });

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

        tools.setupToolbar(this, toolbar, "", true);
        initShimmerLayout();

    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterBook.getItemCount() - current_page);
        if (postTotal > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            searchAction(next_page);
        } else {
            adapterBook.setLoaded();
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                bt_clear.setVisibility(View.GONE);
            } else {
                bt_clear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void displayApiResult(final List<Book> books) {
        adapterBook.insertDataWithNativeAd(books);
        swipeProgress(false);
        if (books.size() == 0) {
            showNotFoundView(true);
        }
    }

    private void requestSearchApi(final int page_no, final String query) {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getApiUrl());
        callbackCall = apiInterface.getSearch(page_no, Constant.BOOKS_PER_PAGE, query, Config.REST_API_KEY);
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
                onFailRequest(page_no);
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

    private void searchAction(final int page_no) {
        showFailedView(false, "");
        showNotFoundView(false);
        final String query = et_search.getText().toString().trim();
        if (!query.equals("")) {
            if (page_no == 1) {
                adapterBook.resetListData();
                swipeProgress(true);
            } else {
                adapterBook.setLoading();
            }
            new Handler().postDelayed(() -> requestSearchApi(page_no, query), Constant.DELAY_TIME);
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_search_input), Toast.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
        findViewById(R.id.failed_retry).setOnClickListener(view -> searchAction(1));
    }

    private void showNotFoundView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_post_found);
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
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        } else {
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        }
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
    public void onBackPressed() {
        if (et_search.length() > 0) {
            et_search.setText("");
        } else {
            super.onBackPressed();
        }
    }

}
