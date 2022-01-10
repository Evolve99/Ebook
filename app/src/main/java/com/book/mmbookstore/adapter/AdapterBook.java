package com.book.mmbookstore.adapter;

import static com.book.mmbookstore.util.Constant.BOOKS_PER_PAGE;
import static com.book.mmbookstore.util.Constant.NATIVE_AD_BOOK_LIST;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.MOPUB;
import static com.solodroid.ads.sdk.util.Constant.UNITY;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.book.mmbookstore.Config;
import com.book.mmbookstore.R;
import com.book.mmbookstore.database.pref.AdsPref;
import com.book.mmbookstore.database.pref.SharedPref;
import com.book.mmbookstore.model.Book;
import com.book.mmbookstore.util.Constant;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.solodroid.ads.sdk.format.NativeAdViewHolder;

import java.util.ArrayList;
import java.util.List;

public class AdapterBook extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_PROG = 0;
    private final int VIEW_ITEM = 1;
    private final int VIEW_AD = 2;

    private List<Book> items;

    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;

    private Context context;
    private OnItemClickListener mOnItemClickListener;
    boolean scrolling = false;

    private SharedPref sharedPref;
    private AdsPref adsPref;

    public interface OnItemClickListener {
        void onItemClick(View view, Book obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public AdapterBook(Context context, RecyclerView view, List<Book> items) {
        this.items = items;
        this.context = context;
        this.sharedPref = new SharedPref(context);
        this.adsPref = new AdsPref(context);
        lastItemViewDetector(view);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrolling = false;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {

        public ImageView bookImage;
        public TextView bookTitle;
        public TextView bookAuthor;
        public LinearLayout lytParent;
        public ImageView overflow;

        public OriginalViewHolder(View v) {
            super(v);
            bookTitle = v.findViewById(R.id.bookTitle);
            bookAuthor = v.findViewById(R.id.bookAuthor);
            bookImage = v.findViewById(R.id.bookImage);
            lytParent = v.findViewById(R.id.lytParent);
            overflow = v.findViewById(R.id.overflow);
        }

    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.load_more);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
            vh = new OriginalViewHolder(v);
        } else if (viewType == VIEW_AD) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_native_ad_medium, parent, false);
            vh = new NativeAdViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_load_more, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Book book = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.bookTitle.setText(book.book_name);
            vItem.bookAuthor.setText(book.author);

            Glide.with(context)
                    .load(sharedPref.getApiUrl() + "/upload/category/" + book.book_image.replace(" ", "%20"))
                    .thumbnail(0.1f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_thumbnail)
                    .into(vItem.bookImage);

            vItem.lytParent.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, book, position);
                }
            });

        } else if (holder instanceof NativeAdViewHolder) {

            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;

            vItem.loadNativeAd(context,
                    adsPref.getAdStatus(),
                    NATIVE_AD_BOOK_LIST,
                    adsPref.getAdType(),
                    adsPref.getAdMobNativeId(),
                    sharedPref.getIsDarkTheme(),
                    context.getResources().getDimensionPixelOffset(R.dimen.gridSpaceBook),
                    Config.ENABLE_LEGACY_GDPR
            );

        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }

        if (getItemViewType(position) == VIEW_PROG) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        } else if (getItemViewType(position) == VIEW_AD) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        } else {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(false);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        Book book = items.get(position);
        if (book != null) {
            if (book.book_name == null || book.book_name.equals("")) {
                return VIEW_AD;
            }
            return VIEW_ITEM;
        } else {
            return VIEW_PROG;
        }
    }

    public void insertDataWithNativeAd(List<Book> items) {
        setLoaded();
        int positionStart = getItemCount();
        for (Book post : items) {
            Log.d("item", "TITLE: " + post.book_name);
            //Log.d("AdapterBook", "book name null position : " + position);
        }

        if (items.size() >= Constant.NATIVE_AD_INDEX) {
            items.add(Constant.NATIVE_AD_INDEX, new Book());
            Log.d("item ads", "space for Native Ad");
        }

        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void insertData(List<Book> items) {
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.items.add(null);
            notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void resetListData() {
        this.items = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {

        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int lastPos = getLastVisibleItem(layoutManager.findLastVisibleItemPositions(null));
                    if (!loading && lastPos == getItemCount() - 1 && onLoadMoreListener != null) {
                        if (NATIVE_AD_BOOK_LIST != 0) {
                            switch (adsPref.getAdType()) {
                                case UNITY:
                                case APPLOVIN:
                                case MOPUB: {
                                    int current_page = getItemCount() / (BOOKS_PER_PAGE);
                                    onLoadMoreListener.onLoadMore(current_page);
                                    break;
                                }
                                default: {
                                    int current_page = getItemCount() / (BOOKS_PER_PAGE + 1); //posts per page plus 1 Ad
                                    onLoadMoreListener.onLoadMore(current_page);
                                    break;
                                }
                            }
                        } else {
                            int current_page = getItemCount() / (BOOKS_PER_PAGE);
                            onLoadMoreListener.onLoadMore(current_page);
                        }
                        loading = true;
                    }
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }

    private int getLastVisibleItem(int[] into) {
        int lastIdx = into[0];
        for (int i : into) {
            if (lastIdx < i) lastIdx = i;
        }
        return lastIdx;
    }

}