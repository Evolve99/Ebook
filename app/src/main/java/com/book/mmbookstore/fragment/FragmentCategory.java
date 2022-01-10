package com.book.mmbookstore.fragment;

import static com.book.mmbookstore.util.Constant.EXTRA_OBJECT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.activity.ActivityCategoryDetails;
import com.book.mmbookstore.activity.MainActivity;
import com.book.mmbookstore.adapter.AdapterCategory;
import com.book.mmbookstore.callback.CallbackCategory;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.model.Category;
import com.book.mmbookstore.rest.ApiInterface;
import com.book.mmbookstore.rest.RestAdapter;
import com.book.mmbookstore.util.Constant;
import com.book.mmbookstore.util.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentCategory extends Fragment {

    View rootView;
    private RecyclerView recyclerView;
    private AdapterCategory adapterCategory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Call<CallbackCategory> callbackCall = null;
    private int postTotal = 0;
    private int failedPage = 0;
    SharedPref sharedPref;
    Tools tools;
    private ShimmerFrameLayout lytShimmer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_category, container, false);

        if (getActivity() != null) {
            tools = new Tools(getActivity());
            sharedPref = new SharedPref(getActivity());
        }

        lytShimmer = rootView.findViewById(R.id.shimmerViewContainer);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));

        //set data and list adapter
        adapterCategory = new AdapterCategory(getActivity(), recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterCategory);

        // on item list clicked
        adapterCategory.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityCategoryDetails.class);
            intent.putExtra(EXTRA_OBJECT, obj);
            startActivity(intent);
            ((MainActivity) getActivity()).showInterstitialAd();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
            }
        });

        adapterCategory.setOnLoadMoreListener(this::setLoadMore);

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterCategory.resetListData();
            requestAction(1);
        });

        requestAction(1);

        return rootView;
    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        if (postTotal > adapterCategory.getItemCount() && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterCategory.setLoaded();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    private void displayApiResult(final List<Category> categories) {
        adapterCategory.insertData(categories);
        swipeProgress(false);
        if (categories.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getApiUrl());
        callbackCall = apiInterface.getCategories(page_no, Constant.CATEGORIES_PER_PAGE, Config.REST_API_KEY);

        callbackCall.enqueue(new Callback<CallbackCategory>() {
            @Override
            public void onResponse(Call<CallbackCategory> call, Response<CallbackCategory> response) {
                CallbackCategory resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    postTotal = resp.count_total;
                    displayApiResult(resp.categories);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackCategory> call, Throwable t) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }

        });
    }

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        adapterCategory.setLoaded();
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
            adapterCategory.setLoading();
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
        View lyt_failed = rootView.findViewById(R.id.lyt_failed);
        ((TextView) rootView.findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failedPage));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = rootView.findViewById(R.id.lyt_no_item);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
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

//    private void initShimmerLayout() {
//        View lytShimmerGrid2 = rootView.findViewById(R.id.lyt_shimmer_recipes_grid2);
//        View lytShimmerGrid3 = rootView.findViewById(R.id.lyt_shimmer_recipes_grid3);
//        if (sharedPref.getBookColumnCount() == 2) {
//            lytShimmerGrid2.setVisibility(View.VISIBLE);
//            lytShimmerGrid3.setVisibility(View.GONE);
//        } else if (sharedPref.getBookColumnCount() == 3) {
//            lytShimmerGrid2.setVisibility(View.GONE);
//            lytShimmerGrid3.setVisibility(View.VISIBLE);
//        }
//    }

//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        activityNavigationDrawer.setupNavigationDrawer(toolbar);
//    }
//
//    private void setupToolbar() {
//        toolbar = rootView.findViewById(R.id.toolbar);
//        toolbar.setTitle(getString(R.string.app_name));
//        activityNavigationDrawer.setSupportActionBar(toolbar);
//    }

}
