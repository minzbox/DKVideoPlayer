package com.dueeeke.dkplayer.fragment.list;

import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dueeeke.dkplayer.R;
import com.dueeeke.dkplayer.activity.MainActivity;
import com.dueeeke.dkplayer.adapter.VideoRecyclerViewAdapter;
import com.dueeeke.dkplayer.adapter.listener.OnItemChildClickListener;
import com.dueeeke.dkplayer.bean.VideoBean;
import com.dueeeke.dkplayer.fragment.BaseFragment;
import com.dueeeke.dkplayer.util.DataUtil;
import com.dueeeke.dkplayer.util.ProgressManagerImpl;
import com.dueeeke.dkplayer.util.Tag;
import com.dueeeke.dkplayer.util.Utils;
import com.dueeeke.videocontroller.StandardVideoController;
import com.dueeeke.videocontroller.component.CompleteView;
import com.dueeeke.videocontroller.component.ErrorView;
import com.dueeeke.videocontroller.component.GestureView;
import com.dueeeke.videocontroller.component.TitleView;
import com.dueeeke.videocontroller.component.VodControlView;
import com.dueeeke.videoplayer.exo.ExoMediaPlayer;
import com.dueeeke.videoplayer.player.VideoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView demo
 */
public class RecyclerViewFragment extends BaseFragment implements OnItemChildClickListener {

    protected List<VideoBean> mVideos = new ArrayList<>();
    protected VideoRecyclerViewAdapter mAdapter;
    protected RecyclerView mRecyclerView;
    protected LinearLayoutManager mLinearLayoutManager;

    protected VideoView mVideoView;
    protected StandardVideoController mController;
    protected ErrorView mErrorView;
    protected CompleteView mCompleteView;
    protected TitleView mTitleView;

    /**
     * Currently playing position
     */
    protected int mCurPos = -1;
    /**
     * The last played position, used to resume playing after the page is cut back
     */
    protected int mLastPos = mCurPos;

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_recycler_view;
    }

    @Override
    protected void initView() {
        super.initView();

        initVideoView();
        //Save progress
        mVideoView.setProgressManager(new ProgressManagerImpl());

        mRecyclerView = findViewById(R.id.rv);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mAdapter = new VideoRecyclerViewAdapter(mVideos);
        mAdapter.setOnItemChildClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {

            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                FrameLayout playerContainer = view.findViewById(R.id.player_container);
                View v = playerContainer.getChildAt(0);
                if (v != null && v == mVideoView && !mVideoView.isFullScreen()) {
                    releaseVideoView();
                }
            }
        });

        View view = findViewById(R.id.add);
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(v -> mAdapter.addData(DataUtil.getVideoList()));
    }

    protected void initVideoView() {
        mVideoView = new VideoView(Objects.requireNonNull(getActivity()));
        mVideoView.setOnStateChangeListener(new VideoView.SimpleOnStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                //Monitor the release of VideoViewManager and reset the state
                if (playState == VideoView.STATE_IDLE) {
                    Utils.removeViewFromParent(mVideoView);
                    mLastPos = mCurPos;
                    mCurPos = -1;
                }
            }
        });
        mController = new StandardVideoController(Objects.requireNonNull(getActivity()));
        mErrorView = new ErrorView(getActivity());
        mController.addControlComponent(mErrorView);
        mCompleteView = new CompleteView(getActivity());
        mController.addControlComponent(mCompleteView);
        mTitleView = new TitleView(getActivity());
        mController.addControlComponent(mTitleView);
        mController.addControlComponent(new VodControlView(getActivity()));
        mController.addControlComponent(new GestureView(getActivity()));
        mController.setEnableOrientation(true);
        mVideoView.setVideoController(mController);
    }

    @Override
    protected void initData() {
        super.initData();
        List<VideoBean> videoList = DataUtil.getVideoList();
        mVideos.addAll(videoList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected boolean isLazyLoad() {
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        pause();
    }

    /**
     * 由于onPause必须调用super。故增加此方法，
     * 子类将会重写此方法，改变onPause的逻辑
     */
    protected void pause() {
        releaseVideoView();
    }

    @Override
    public void onResume() {
        super.onResume();
        resume();
    }

    /**
     * 由于onResume必须调用super。故增加此方法，
     * 子类将会重写此方法，改变onResume的逻辑
     */
    protected void resume() {
        if (mLastPos == -1)
            return;
        if (MainActivity.mCurrentIndex != 1)
            return;
        //恢复上次播放的位置
        startPlay(mLastPos);
    }

    /**
     * PrepareView is clicked
     */
    @Override
    public void onItemChildClick(int position) {
        startPlay(position);
    }

    /**
     * Start playing
     * @param position list position
     */
    protected void startPlay(int position) {
        if (mCurPos == position) return;
        if (mCurPos != -1) {
            releaseVideoView();
        }
        VideoBean videoBean = mVideos.get(position);
        //Play and save
//        String proxyUrl = ProxyVideoCacheManager.getProxy(getActivity()).getProxyUrl(videoBean.getUrl());
//        mVideoView.setUrl(proxyUrl);

        mVideoView.setUrl(videoBean.getUrl());
        mTitleView.setTitle(videoBean.getTitle());
        View itemView = mLinearLayoutManager.findViewByPosition(position);
        if (itemView == null) return;
        VideoRecyclerViewAdapter.VideoHolder viewHolder = (VideoRecyclerViewAdapter.VideoHolder) itemView.getTag();
        //Add the PrepareView preset in the list to the controller. Note that isPrivate can only be true here.
        mController.addControlComponent(viewHolder.mPrepareView, true);
        Utils.removeViewFromParent(mVideoView);
        viewHolder.mPlayerContainer.addView(mVideoView, 0);
        //Add VideoView to VideoViewManager before playing so that it can be operated on other pages
        getVideoViewManager().add(mVideoView, Tag.LIST);
        mVideoView.start();
        mCurPos = position;

    }

    private void releaseVideoView() {
        mVideoView.release();
        if (mVideoView.isFullScreen()) {
            mVideoView.stopFullScreen();
        }
        if(Objects.requireNonNull(getActivity()).getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        mCurPos = -1;
    }
}
