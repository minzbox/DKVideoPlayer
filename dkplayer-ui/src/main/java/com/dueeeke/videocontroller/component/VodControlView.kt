package com.dueeeke.videocontroller.component

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.dueeeke.videocontroller.R
import com.dueeeke.videoplayer.controller.ControlWrapper
import com.dueeeke.videoplayer.controller.IControlComponent
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.util.PlayerUtils.scanForActivity
import com.dueeeke.videoplayer.util.PlayerUtils.stringForTime

/**
 * On-demand control bar at the bottom
 */
open class VodControlView : FrameLayout, IControlComponent, View.OnClickListener, OnSeekBarChangeListener {
    @JvmField
    protected var mControlWrapper: ControlWrapper? = null
    private var mTotalTime: TextView? = null
    private var mCurrTime: TextView? = null
    private var mFullScreen: ImageView? = null
    private var mBottomContainer: LinearLayout? = null
    private var mVideoProgress: SeekBar? = null
    private var mBottomProgress: ProgressBar? = null
    private var mPlayButton: ImageView? = null
    private var mIsDragging = false
    private var mIsShowBottomProgress = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    protected open val layoutId: Int
        get() = R.layout.dkplayer_layout_vod_control_view

    /**
     * Whether to display the bottom progress bar, the default display
     */
    fun showBottomProgress(isShow: Boolean) {
        mIsShowBottomProgress = isShow
    }

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override val view: View
        get() = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            mBottomContainer?.visibility = VISIBLE
            if (anim != null) {
                mBottomContainer?.startAnimation(anim)
            }
            if (mIsShowBottomProgress) {
                mBottomProgress?.visibility = GONE
            }
        } else {
            mBottomContainer?.visibility = GONE
            if (anim != null) {
                mBottomContainer?.startAnimation(anim)
            }
            if (mIsShowBottomProgress) {
                mBottomProgress!!.visibility = VISIBLE
                val animation = AlphaAnimation(0f, 1f)
                animation.duration = 300
                mBottomProgress?.startAnimation(animation)
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED -> {
                visibility = GONE
                mBottomProgress?.progress = 0
                mBottomProgress?.secondaryProgress = 0
                mVideoProgress?.progress = 0
                mVideoProgress?.secondaryProgress = 0
            }
            VideoView.STATE_START_ABORT, VideoView.STATE_PREPARING, VideoView.STATE_PREPARED, VideoView.STATE_ERROR -> visibility = GONE
            VideoView.STATE_PLAYING -> {
                mPlayButton?.isSelected = true
                if (mIsShowBottomProgress) {
                    if (mControlWrapper?.isShowing == true) {
                        mBottomProgress?.visibility = GONE
                        mBottomContainer?.visibility = VISIBLE
                    } else {
                        mBottomContainer?.visibility = GONE
                        mBottomProgress?.visibility = VISIBLE
                    }
                } else {
                    mBottomContainer?.visibility = GONE
                }
                visibility = VISIBLE
                //Start refreshing progress
                mControlWrapper?.startProgress()
            }
            VideoView.STATE_PAUSED -> mPlayButton?.isSelected = false
            VideoView.STATE_BUFFERING, VideoView.STATE_BUFFERED -> mPlayButton?.isSelected = mControlWrapper?.isPlaying == true
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        when (playerState) {
            VideoView.PLAYER_NORMAL -> mFullScreen?.isSelected = false
            VideoView.PLAYER_FULL_SCREEN -> mFullScreen?.isSelected = true
        }
        val activity = scanForActivity(context)
        if (activity != null && mControlWrapper?.hasCutout() == true) {
            val orientation = activity.requestedOrientation
            val cutoutHeight = mControlWrapper?.cutoutHeight
            when (orientation) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                    mBottomContainer!!.setPadding(0, 0, 0, 0)
                    mBottomProgress!!.setPadding(0, 0, 0, 0)
                }
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                    cutoutHeight?.let {
                        mBottomContainer?.setPadding(it, 0, 0, 0)
                        mBottomProgress?.setPadding(it, 0, 0, 0)
                    }
                }
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                    cutoutHeight?.let {
                        mBottomContainer?.setPadding(0, 0, it, 0)
                        mBottomProgress?.setPadding(0, 0, it, 0)
                    }
                }
            }
        }
    }

    override fun setProgress(duration: Int, position: Int) {
        if (mIsDragging) {
            return
        }
        if (mVideoProgress != null) {
            if (duration > 0) {
                mVideoProgress!!.isEnabled = true
                val pos = (position * 1.0 / duration * mVideoProgress?.max!!).toInt()
                mVideoProgress!!.progress = pos
                mBottomProgress!!.progress = pos
            } else {
                mVideoProgress!!.isEnabled = false
            }
            val percent = mControlWrapper!!.bufferedPercentage
            if (percent >= 95) { //Solve the problem that the buffer progress cannot be 100%
                mVideoProgress!!.secondaryProgress = mVideoProgress!!.max
                mBottomProgress!!.secondaryProgress = mBottomProgress!!.max
            } else {
                mVideoProgress!!.secondaryProgress = percent * 10
                mBottomProgress!!.secondaryProgress = percent * 10
            }
        }
        mTotalTime?.text = stringForTime(duration)
        mCurrTime?.text = stringForTime(position)
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        onVisibilityChanged(!isLocked, null)
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.fullscreen) {
            toggleFullScreen()
        } else if (id == R.id.iv_play) {
            mControlWrapper!!.togglePlay()
        }
    }

    /**
     * 横竖屏切换
     */
    private fun toggleFullScreen() {
        val activity = scanForActivity(context)
        mControlWrapper!!.toggleFullScreen(activity)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        mIsDragging = true
        mControlWrapper!!.stopProgress()
        mControlWrapper!!.stopFadeOut()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        val duration = mControlWrapper!!.duration
        val newPosition = duration * seekBar.progress / mVideoProgress!!.max
        mControlWrapper!!.seekTo(newPosition)
        mIsDragging = false
        mControlWrapper!!.startProgress()
        mControlWrapper!!.startFadeOut()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (!fromUser) {
            return
        }
        val duration = mControlWrapper!!.duration
        val newPosition = duration * progress / mVideoProgress!!.max
        if (mCurrTime != null) mCurrTime!!.text = stringForTime(newPosition.toInt())
    }

    val initView by lazy {
        visibility = GONE
        LayoutInflater.from(context).inflate(layoutId, this, true)
        mFullScreen = findViewById(R.id.fullscreen)
        mFullScreen?.setOnClickListener(this)
        mBottomContainer = findViewById(R.id.bottom_container)
        mVideoProgress = findViewById(R.id.seekBar)
        mVideoProgress?.setOnSeekBarChangeListener(this)
        mTotalTime = findViewById(R.id.total_time)
        mCurrTime = findViewById(R.id.curr_time)
        mPlayButton = findViewById(R.id.iv_play)
        mPlayButton?.setOnClickListener(this)
        mBottomProgress = findViewById(R.id.bottom_progress)

        //Android 5.1 The SeekBar height of the following systems needs to be set to WRAP_CONTENT
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mVideoProgress?.layoutParams!!.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    init {
        initView
    }
}