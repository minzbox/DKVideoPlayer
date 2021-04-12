package com.dueeeke.dkplayer.fragment.list;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.transition.Transition;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;

import com.dueeeke.dkplayer.R;
import com.dueeeke.dkplayer.activity.list.DetailActivity;
import com.dueeeke.dkplayer.adapter.VideoRecyclerViewAdapter;
import com.dueeeke.dkplayer.bean.VideoBean;
import com.dueeeke.dkplayer.util.IntentKeys;
import com.dueeeke.dkplayer.util.Tag;
import com.dueeeke.dkplayer.util.Utils;
import com.dueeeke.videoplayer.player.VideoView;

/**
 * Seamless playback
 */
public class SeamlessPlayFragment extends RecyclerViewAutoPlayFragment {

    private boolean mSkipToDetail;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_recycler_view;
    }

    @Override
    protected void initView() {
        super.initView();

        //Added to VideoViewManager in advance for detailed use
        getVideoViewManager().add(mVideoView, Tag.SEAMLESS);

        mAdapter.setOnItemClickListener(position -> {
            mSkipToDetail = true;
            Intent intent = new Intent(getActivity(), DetailActivity.class);
            Bundle bundle = new Bundle();
            VideoBean videoBean = mVideos.get(position);
            if (mCurPos == position) {
                //Need to play seamlessly
                bundle.putBoolean(IntentKeys.SEAMLESS_PLAY, true);
                bundle.putString(IntentKeys.TITLE, videoBean.getTitle());
            } else {
                //No need to play seamlessly, transfer the corresponding data to the details page
                mVideoView.release();
                //Need to restore the controller
                mController.setPlayState(VideoView.STATE_IDLE);
                bundle.putBoolean(IntentKeys.SEAMLESS_PLAY, false);
                bundle.putString(IntentKeys.URL, videoBean.getUrl());
                bundle.putString(IntentKeys.TITLE, videoBean.getTitle());
                mCurPos = position;
            }
            intent.putExtras(bundle);
            View sharedView = mLinearLayoutManager.findViewByPosition(position).findViewById(R.id.player_container);
            //Use shared element animation
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(getActivity(), sharedView, DetailActivity.VIEW_NAME_PLAYER_CONTAINER);
            ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
        });
    }

    @Override
    protected void startPlay(int position) {
        mVideoView.setVideoController(mController);
        super.startPlay(position);
    }

    @Override
    protected void pause() {
        if (!mSkipToDetail) {
            super.pause();
        }
    }

    @Override
    protected void resume() {
        if (mSkipToDetail) {
            mSkipToDetail = false;
        } else {
            super.resume();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !addTransitionListener()) {
            restoreVideoView();
        }
    }

    @RequiresApi(21)
    private boolean addTransitionListener() {
        final Transition transition = getActivity().getWindow().getSharedElementExitTransition();
        if (transition != null) {
            transition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    restoreVideoView();
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionStart(Transition transition) {

                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionPause(Transition transition) {
                }

                @Override
                public void onTransitionResume(Transition transition) {
                }
            });
            return true;
        }
        return false;
    }

    private void restoreVideoView() {
        //Restore player
        View itemView = mLinearLayoutManager.findViewByPosition(mCurPos);
        VideoRecyclerViewAdapter.VideoHolder viewHolder = (VideoRecyclerViewAdapter.VideoHolder) itemView.getTag();
        mVideoView = getVideoViewManager().get(Tag.SEAMLESS);
        Utils.removeViewFromParent(mVideoView);
        viewHolder.mPlayerContainer.addView(mVideoView, 0);

        mController.addControlComponent(viewHolder.mPrepareView, true);
        mController.setPlayState(mVideoView.getCurrentPlayState());
        mController.setPlayerState(mVideoView.getCurrentPlayerState());

        mRecyclerView.postDelayed(() -> mVideoView.setVideoController(mController), 100);
    }
}
