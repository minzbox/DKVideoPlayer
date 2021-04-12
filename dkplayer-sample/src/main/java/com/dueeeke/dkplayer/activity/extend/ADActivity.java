package com.dueeeke.dkplayer.activity.extend;

import android.widget.Toast;

import com.danikula.videocache.HttpProxyCacheServer;
import com.dueeeke.dkplayer.R;
import com.dueeeke.dkplayer.activity.BaseActivity;
import com.dueeeke.dkplayer.util.DataUtil;
import com.dueeeke.dkplayer.util.cache.ProxyVideoCacheManager;
import com.dueeeke.dkplayer.widget.component.AdControlView;
import com.dueeeke.videocontroller.StandardVideoController;
import com.dueeeke.videoplayer.player.VideoView;

/**
 * 广告
 * Created by dueeeke on 2017/4/7.
 */

public class ADActivity extends BaseActivity<VideoView> {

    private static final String URL_AD = "https://gslb.miaopai.com/stream/IR3oMYDhrON5huCmf7sHCfnU5YKEkgO2.mp4";

    private StandardVideoController mController;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_layout_common;
    }

    @Override
    protected int getTitleResId() {
        return R.string.str_ad;
    }

    @Override
    protected void initView() {
        super.initView();
        mVideoView = findViewById(R.id.video_view);
        mController = new StandardVideoController(this);
        AdControlView adControlView = new AdControlView(this);
        adControlView.setListener(new AdControlView.AdControlListener() {
            @Override
            public void onAdClick() {
                Toast.makeText(ADActivity.this, "Ad click to jump", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSkipAd() {
                playVideo();
            }
        });
        mController.addControlComponent(adControlView);

        HttpProxyCacheServer cacheServer = ProxyVideoCacheManager.getProxy(this);
        String proxyUrl = cacheServer.getProxyUrl(URL_AD);
        mVideoView.setUrl(proxyUrl);
        mVideoView.setVideoController(mController);

        //End of monitoring playback
        mVideoView.addOnStateChangeListener(new VideoView.SimpleOnStateChangeListener() {
            @Override
            public void onPlayStateChanged(int playState) {
                if (playState == VideoView.STATE_PLAYBACK_COMPLETED) {
                    playVideo();
                }
            }
        });

        mVideoView.start();
    }

    /**
     * Play the feature film
     */
    private void playVideo() {
        mVideoView.release();
        mController.removeAllControlComponent();
        mController.addDefaultControlComponent("正片", false);
        //Reset data
        mVideoView.setUrl(DataUtil.SAMPLE_URL);
        //Start playing
        mVideoView.start();
    }
}
