package com.book.mmbookstore.fragment;

import static com.book.mmbookstore.util.Constant.BOOKS_2_COLUMNS;
import static com.book.mmbookstore.util.Constant.BOOKS_3_COLUMNS;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.book.mmbookstore.R;
import com.book.mmbookstore.activity.MainActivity;
import com.book.mmbookstore.adapter.AdapterBook;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.database.room.AppDatabase;
import com.book.mmbookstore.database.room.DAO;
import com.book.mmbookstore.model.Book;
import com.book.mmbookstore.util.Tools;

import java.util.ArrayList;
import java.util.List;

public class FragmentBookmark extends Fragment {

    View rootView;
    AdapterBook adapterBookmark;
    RecyclerView recyclerView;
    private DAO db;
    SharedPref sharedPref;
    Tools tools;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_bookmark, container, false);
        db = AppDatabase.getDb(getContext()).get();

        if (getActivity() != null) {
            tools = new Tools(getActivity());
            sharedPref = new SharedPref(getActivity());
        }

        recyclerView = rootView.findViewById(R.id.recyclerView);
        if (sharedPref.getBookColumnCount() == BOOKS_2_COLUMNS) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_2_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        } else if (sharedPref.getBookColumnCount() == BOOKS_3_COLUMNS) {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_3_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        } else {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(BOOKS_2_COLUMNS, StaggeredGridLayoutManager.VERTICAL));
        }

        //set data and list adapter
        adapterBookmark = new AdapterBook(getActivity(), recyclerView, new ArrayList<>());
        recyclerView.setAdapter(adapterBookmark);

        // on item list clicked
        adapterBookmark.setOnItemClickListener((v, obj, position) -> {
            tools.onBookClicked(obj);
            ((MainActivity) getActivity()).showInterstitialAd();
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        displayData(db.getAllBookmark());
    }

    private void displayData(final List<Book> notifications) {
        adapterBookmark.resetListData();
        List<Book> items = db.getAllBookmark();
        adapterBookmark.insertData(items);
        showNoItemView(false);
        if (notifications.size() == 0) {
            showNoItemView(true);
        }
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = rootView.findViewById(R.id.lyt_no_favorite);
        ((TextView) rootView.findViewById(R.id.no_item_message)).setText(R.string.msg_no_favorite);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }


}
