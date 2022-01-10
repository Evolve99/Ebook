package com.book.mmbookstore.fragment;

import static com.book.mmbookstore.util.Constant.STORY_PER_PAGE;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.activity.ActivityStoryDetail;
import com.book.mmbookstore.adapter.AdapterStory;
import com.book.mmbookstore.callback.CallbackStory;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.model.Story;
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


public class FragmentStory extends DialogFragment {

    private RecyclerView recyclerView;
    private AdapterStory adapterStory;
    private Call<CallbackStory> callbackCall = null;
    private int postTotal = 0;
    private int failedPage = 0;
    View rootView;
    ImageButton btnBack;
    Toolbar toolbar;
    TextView title;
    RelativeLayout lytDialogFragment;
    String book_id;
    String book_name;
    private ShimmerFrameLayout lytShimmer;
    StaggeredGridLayoutManager staggeredGridLayoutManager;
    SharedPref sharedPref;
    Tools tools;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_story, container, false);
        initView();
        return rootView;
    }

    private void initView() {
        if (getArguments() != null) {
            book_id = getArguments().getString("book_id");
            book_name = getArguments().getString("book_name");
        }

        if (getActivity() != null) {
            sharedPref = new SharedPref(getActivity());
            tools = new Tools(getActivity());
        }

        toolbar = rootView.findViewById(R.id.toolbar);
        title = rootView.findViewById(R.id.dialog_title);
        title.setText(book_name);

        btnBack = rootView.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> new Handler().postDelayed(() -> {
            if (getActivity() != null) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                }
            }
            dismiss();
        }, 300));

        lytDialogFragment = rootView.findViewById(R.id.lytDialogFragment);
        lytDialogFragment.setOnClickListener(v -> {
            //do nothing
        });

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            lytDialogFragment.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            lytDialogFragment.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }

        lytShimmer = rootView.findViewById(R.id.shimmerViewContainer);

        recyclerView = rootView.findViewById(R.id.recyclerView);
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);

        //set data and list adapter
        adapterStory = new AdapterStory(getActivity(), recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterStory);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
            }
        });

        adapterStory.setOnItemClickListener((view, obj, position) -> new Handler().postDelayed(() -> {
            if (getActivity() != null) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                }
            }
            dismiss();
            ((ActivityStoryDetail) getActivity()).jumpPage(position);
        }, 300));

        adapterStory.setOnLoadMoreListener(this::setLoadMore);
        requestAction(1);

    }

    public void setLoadMore(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        if (postTotal > adapterStory.getItemCount() && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterStory.setLoaded();
        }
    }

    private void displayApiResult(final List<Story> stories) {
        adapterStory.insertData(stories);
        swipeProgress(false);
        if (stories.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getApiUrl());
        callbackCall = apiInterface.getStories(book_id, page_no, STORY_PER_PAGE, sharedPref.getStorySort(), sharedPref.getStoryOrder(), Config.REST_API_KEY);

        callbackCall.enqueue(new Callback<CallbackStory>() {
            @Override
            public void onResponse(Call<CallbackStory> call, Response<CallbackStory> response) {
                CallbackStory resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    postTotal = resp.count_total;
                    displayApiResult(resp.stories);
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

    private void onFailRequest(int page_no) {
        failedPage = page_no;
        adapterStory.setLoaded();
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
            adapterStory.setLoading();
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
            lytShimmer.setVisibility(View.GONE);
            lytShimmer.stopShimmer();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
}