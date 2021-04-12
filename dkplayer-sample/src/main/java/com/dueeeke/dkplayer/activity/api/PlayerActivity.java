package com.dueeeke.dkplayer.activity.api;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.dueeeke.dkplayer.R;
import com.dueeeke.dkplayer.activity.BaseActivity;
import com.dueeeke.dkplayer.util.IntentKeys;
import com.dueeeke.dkplayer.widget.component.DebugInfoView;
import com.dueeeke.dkplayer.widget.component.PlayerMonitor;
import com.dueeeke.videocontroller.StandardVideoController;
import com.dueeeke.videocontroller.component.CompleteView;
import com.dueeeke.videocontroller.component.ErrorView;
import com.dueeeke.videocontroller.component.GestureView;
import com.dueeeke.videocontroller.component.LiveControlView;
import com.dueeeke.videocontroller.component.PrepareView;
import com.dueeeke.videocontroller.component.TitleView;
import com.dueeeke.videocontroller.component.VodControlView;
import com.dueeeke.videoplayer.player.VideoView;
import com.dueeeke.videoplayer.util.L;

/**
 * 播放器演示
 * Created by dueeeke on 2017/4/7.
 */

public class PlayerActivity extends BaseActivity<VideoView> {

    private static final String THUMB = "https://cms-bucket.nosdn.127.net/eb411c2810f04ffa8aaafc42052b233820180418095416.jpeg";

    public static void start(Context context, String url, String title, boolean isLive) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra(IntentKeys.URL, url);
        intent.putExtra(IntentKeys.IS_LIVE, isLive);
        intent.putExtra(IntentKeys.TITLE, title);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_player;
    }

    @Override
    protected void initView() {
        super.initView();
        mVideoView = findViewById(R.id.player);
        getLifecycle().addObserver(mVideoView);
        Intent intent = getIntent();
        if (intent != null) {
            StandardVideoController controller = new StandardVideoController(this);
            //Automatically enter/exit full screen according to screen orientation
            controller.setEnableOrientation(true);

            PrepareView prepareView = new PrepareView(this);//Ready to play interface
            ImageView thumb = prepareView.findViewById(R.id.thumb);//cover image
            Glide.with(this).load(THUMB).into(thumb);
            controller.addControlComponent(prepareView);

            controller.addControlComponent(new CompleteView(this));//Auto complete playback interface

            controller.addControlComponent(new ErrorView(this));//Error interface

            TitleView titleView = new TitleView(this);//title
            controller.addControlComponent(titleView);

            //According to whether to set different bottom control bars for live broadcast
            boolean isLive = intent.getBooleanExtra(IntentKeys.IS_LIVE, false);
            if (isLive) {
                controller.addControlComponent(new LiveControlView(this));//Live control strip
            } else {
                VodControlView vodControlView = new VodControlView(this);//On-demand control strip
                //Whether to display the bottom progress bar. Default Display
//                vodControlView.showBottomProgress(false);
                controller.addControlComponent(vodControlView);
            }

            GestureView gestureControlView = new GestureView(this);//Slide control view
            controller.addControlComponent(gestureControlView);
            //Decide whether you need to slide to adjust the progress according to whether it is a live broadcast
            controller.setCanChangePosition(!isLive);

            //Set title
            String title = intent.getStringExtra(IntentKeys.TITLE);
            titleView.setTitle(title);

            //注意：以上组件如果你想单独定制，我推荐你把源码复制一份出来，然后改成你想要的样子。
            //改完之后再通过addControlComponent添加上去
            //你也可以通过addControlComponent添加一些你自己的组件，具体实现方式参考现有组件的实现。
            //这个组件不一定是View，请发挥你的想象力😃

            //如果你不需要单独配置各个组件，可以直接调用此方法快速添加以上组件
//            controller.addDefaultControlComponent(title, isLive);

            //竖屏也开启手势操作，默认关闭
//            controller.setEnableInNormal(true);
            //滑动调节亮度，音量，进度，默认开启
//            controller.setGestureEnabled(false);
            //适配刘海屏，默认开启
//            controller.setAdaptCutout(false);

            //Display debugging information on the controller
            controller.addControlComponent(new DebugInfoView(this));
            //Display debugging information in LogCat
            controller.addControlComponent(new PlayerMonitor());

            //If you don’t want the UI, just don’t set the controller
            mVideoView.setVideoController(controller);

            mVideoView.setUrl(getIntent().getStringExtra(IntentKeys.URL));

            //保存播放进度
//            mVideoView.setProgressManager(new ProgressManagerImpl());
            //Play status monitoring
            mVideoView.addOnStateChangeListener(mOnStateChangeListener);

            //Temporarily switch the playback core, if you need to configure it globally, please configure it through VideoConfig, see MyApplication for details
            //Use IjkPlayer to decode
//            mVideoView.setPlayerFactory(IjkPlayerFactory.create());
            //使用ExoPlayer解码
//            mVideoView.setPlayerFactory(ExoMediaPlayerFactory.create());
            //使用MediaPlayer解码
//            mVideoView.setPlayerFactory(AndroidMediaPlayerFactory.create());

            mVideoView.start();
        }

        //Play other videos
        EditText etOtherVideo = findViewById(R.id.et_other_video);
        findViewById(R.id.btn_start_play).setOnClickListener(v -> {
            mVideoView.release();
            mVideoView.setUrl(etOtherVideo.getText().toString());
            mVideoView.start();
        });
    }

    private final VideoView.OnStateChangeListener mOnStateChangeListener = new VideoView.SimpleOnStateChangeListener() {
        @Override
        public void onPlayerStateChanged(int playerState) {
            switch (playerState) {
                case VideoView.PLAYER_NORMAL://小屏
                    break;
                case VideoView.PLAYER_FULL_SCREEN://全屏
                    break;
            }
        }

        @Override
        public void onPlayStateChanged(int playState) {
            switch (playState) {
                case VideoView.STATE_IDLE:
                    break;
                case VideoView.STATE_PREPARING:
                    //在STATE_PREPARING时设置setMute(true)可实现静音播放
//                    mVideoView.setMute(true);
                    break;
                case VideoView.STATE_PREPARED:
                    break;
                case VideoView.STATE_PLAYING:
                    //Need to get the video width and height at this time
                    int[] videoSize = mVideoView.getVideoSize();
                    if (videoSize != null) {
                        L.d("视频宽：" + videoSize[0]);
                        L.d("视频高：" + videoSize[1]);
                    }
                    break;
                case VideoView.STATE_PAUSED:
                    break;
                case VideoView.STATE_BUFFERING:
                    break;
                case VideoView.STATE_BUFFERED:
                    break;
                case VideoView.STATE_PLAYBACK_COMPLETED:
                    break;
                case VideoView.STATE_ERROR:
                    break;
            }
        }
    };

    private int i = 0;

    public void onButtonClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.scale_default:
                mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_DEFAULT);
                break;
            case R.id.scale_169:
                mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_16_9);
                break;
            case R.id.scale_43:
                mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_4_3);
                break;
            case R.id.scale_original:
                mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_ORIGINAL);
                break;
            case R.id.scale_match_parent:
                mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_MATCH_PARENT);
                break;
            case R.id.scale_center_crop:
                mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_CENTER_CROP);
                break;

            case R.id.speed_0_5:
                mVideoView.setSpeed(0.5f);
                break;
            case R.id.speed_0_75:
                mVideoView.setSpeed(0.75f);
                break;
            case R.id.speed_1_0:
                mVideoView.setSpeed(1.0f);
                break;
            case R.id.speed_1_5:
                mVideoView.setSpeed(1.5f);
                break;
            case R.id.speed_2_0:
                mVideoView.setSpeed(2.0f);
                break;

            case R.id.screen_shot:
                ImageView imageView = findViewById(R.id.iv_screen_shot);
                Bitmap bitmap = mVideoView.doScreenShot();
                imageView.setImageBitmap(bitmap);
                break;

            case R.id.mirror_rotate:
                mVideoView.setMirrorRotation(i % 2 == 0);
                i++;
                break;
            case R.id.btn_mute:
                mVideoView.setMute(true);
                break;
        }
    }
}
