package com.dueeeke.videocontroller

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.AttrRes
import com.dueeeke.videocontroller.component.*
import com.dueeeke.videoplayer.controller.GestureVideoController
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.util.PlayerUtils.dp2px

/**
 * Live/on-demand controller
 * * Note: This controller is for reference only, if you want to customize the ui, you can directly inherit the implementation of GestureVideoController or BaseVideoController
 * * Your own controller
 * Created by dueeeke on 2017/4/7.
 */
open class StandardVideoController @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) : GestureVideoController(context, attrs, defStyleAttr), View.OnClickListener {
    private var mLockButton: ImageView? = null
    private var mLoadingProgress: ProgressBar? = null
    override val layoutId: Int
        get() = R.layout.dkplayer_layout_standard_controller

    override fun initView() {
        super.initView()
        mLockButton = findViewById(R.id.lock)
        mLockButton?.setOnClickListener(this)
        mLoadingProgress = findViewById(R.id.loading)
    }

    /**
     * Quickly add various components
     * @param title title
     * @param isLive is it a live broadcast
     */
    fun addDefaultControlComponent(title: String?, isLive: Boolean) {
        val completeView = CompleteView(context)
        val errorView = ErrorView(context)
        val prepareView = PrepareView(context)
        prepareView.setClickStart()
        val titleView = TitleView(context)
        titleView.setTitle(title)
        addControlComponent(completeView, errorView, prepareView, titleView)
        if (isLive) {
            addControlComponent(LiveControlView(context))
        } else {
            addControlComponent(VodControlView(context))
        }
        addControlComponent(GestureView(context))
        setCanChangePosition(!isLive)
    }

    override fun onClick(v: View) {
        val i = v.id
        if (i == R.id.lock) {
            if (mControlWrapper != null) {
                mControlWrapper!!.toggleLockState()
            }
        }
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        if (isLocked) {
            mLockButton!!.isSelected = true
            Toast.makeText(context, R.string.dkplayer_locked, Toast.LENGTH_SHORT).show()
        } else {
            mLockButton!!.isSelected = false
            Toast.makeText(context, R.string.dkplayer_unlocked, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (mControlWrapper != null && mControlWrapper!!.isFullScreen) {
            if (isVisible) {
                if (mLockButton!!.visibility == GONE) {
                    mLockButton!!.visibility = VISIBLE
                    if (anim != null) {
                        mLockButton!!.startAnimation(anim)
                    }
                }
            } else {
                mLockButton!!.visibility = GONE
                if (anim != null) {
                    mLockButton!!.startAnimation(anim)
                }
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        super.onPlayerStateChanged(playerState)
        when (playerState) {
            VideoView.PLAYER_NORMAL -> {
                layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
                mLockButton!!.visibility = GONE
            }
            VideoView.PLAYER_FULL_SCREEN -> if (isShowing) {
                mLockButton!!.visibility = VISIBLE
            } else {
                mLockButton!!.visibility = GONE
            }
        }
        if (mActivity != null && hasCutout()) {
            val orientation = mActivity!!.requestedOrientation
            val dp24 = dp2px(context, 24f)
            val cutoutHeight = cutoutHeight
            when (orientation) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                    val lblp = mLockButton!!.layoutParams as LayoutParams
                    lblp.setMargins(dp24, 0, dp24, 0)
                }
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                    val layoutParams = mLockButton!!.layoutParams as LayoutParams
                    layoutParams.setMargins(dp24 + cutoutHeight, 0, dp24 + cutoutHeight, 0)
                }
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                    val layoutParams = mLockButton!!.layoutParams as LayoutParams
                    layoutParams.setMargins(dp24, 0, dp24, 0)
                }
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        super.onPlayStateChanged(playState)
        when (playState) {
            VideoView.STATE_IDLE -> {
                mLockButton!!.isSelected = false
                mLoadingProgress!!.visibility = GONE
            }
            VideoView.STATE_PLAYING, VideoView.STATE_PAUSED, VideoView.STATE_PREPARED, VideoView.STATE_ERROR, VideoView.STATE_BUFFERED -> mLoadingProgress!!.visibility = GONE
            VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> mLoadingProgress!!.visibility = VISIBLE
            VideoView.STATE_PLAYBACK_COMPLETED -> {
                mLoadingProgress!!.visibility = GONE
                mLockButton!!.visibility = GONE
                mLockButton!!.isSelected = false
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (isLocked) {
            show()
            Toast.makeText(context, R.string.dkplayer_lock_tip, Toast.LENGTH_SHORT).show()
            return true
        }
        return if (mControlWrapper != null && mControlWrapper!!.isFullScreen) {
            stopFullScreen()
        } else super.onBackPressed()
    }
}