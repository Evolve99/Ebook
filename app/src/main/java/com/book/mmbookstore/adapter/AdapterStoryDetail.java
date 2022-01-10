package com.book.mmbookstore.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.book.mmbookstore.R;
import com.book.mmbookstore.activity.ActivityStoryDetail;
import com.book.mmbookstore.model.Story;
import com.book.mmbookstore.util.Tools;

import java.util.List;

public class AdapterStoryDetail extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Story> items;
    private final Context context;
    boolean flag = true;
    Tools tools;

    public AdapterStoryDetail(Context context, List<Story> items) {
        this.items = items;
        this.context = context;
        this.tools = new Tools((Activity) context);
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {

        public TextView storyTitle;
        public TextView storySubtitle;
        public WebView storyDescription;
        public LinearLayout lytParent;

        public OriginalViewHolder(View v) {
            super(v);
            storyTitle = v.findViewById(R.id.storyTitle);
            storySubtitle = v.findViewById(R.id.storySubtitle);
            storyDescription = v.findViewById(R.id.storyDescription);
            lytParent = v.findViewById(R.id.lytParent);
        }

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OriginalViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_detail, parent, false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Story story = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.storyTitle.setText(story.story_title);
            vItem.storySubtitle.setText(story.story_subtitle);
            tools.displayContent(vItem.storyDescription, story.story_description);

            vItem.lytParent.setOnClickListener(view -> {
                if (flag) {
                    ((ActivityStoryDetail) context).fullScreenMode(true);
                    flag = false;
                } else {
                    ((ActivityStoryDetail) context).fullScreenMode(false);
                    flag = true;
                }
            });

            vItem.storyDescription.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_UP:
                        if (flag) {
                            ((ActivityStoryDetail) context).fullScreenMode(true);
                            flag = false;
                        } else {
                            ((ActivityStoryDetail) context).fullScreenMode(false);
                            flag = true;
                        }
                        break;
                }
                return false;
            });

        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void insertData(List<Story> items) {
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setLoaded() {
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

}