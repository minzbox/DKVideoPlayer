package com.dueeeke.dkplayer.fragment.list;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dueeeke.dkplayer.adapter.VideoRecyclerViewAdapter;
import com.dueeeke.videoplayer.util.L;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

/**
 * Sliding list to play automatically, only contains the logic of automatic play
 */
public class RecyclerViewAutoPlayFragment extends RecyclerViewFragment {

    @Override
    protected void initView() {
        super.initView();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == SCROLL_STATE_IDLE) { //Scroll stop
                    autoPlayVideo(recyclerView);
                }
            }

            private void autoPlayVideo(RecyclerView view) {
                if (view == null) return;
                //Traverse the RecyclerView child controls and start playing if mPlayerContainer is fully visible
                int count = view.getChildCount();
                L.d("ChildCount:" + count);
                for (int i = 0; i < count; i++) {
                    View itemView = view.getChildAt(i);
                    if (itemView == null) continue;
                    VideoRecyclerViewAdapter.VideoHolder holder = (VideoRecyclerViewAdapter.VideoHolder) itemView.getTag();
                    Rect rect = new Rect();
                    holder.mPlayerContainer.getLocalVisibleRect(rect);
                    int height = holder.mPlayerContainer.getHeight();
                    if (rect.top == 0 && rect.bottom == height) {
                        startPlay(holder.mPosition);
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();

        mRecyclerView.post(() -> {
            //Automatically play the first
            startPlay(0);
        });
    }
}
