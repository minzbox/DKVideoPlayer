package com.dueeeke.videocontroller.component

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.dueeeke.videocontroller.R
import com.dueeeke.videoplayer.controller.ControlWrapper
import com.dueeeke.videoplayer.controller.IControlComponent
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.player.VideoViewManager.Companion.instance
import java.util.*

/**
 * Ready to play interface
 */
class PrepareView : FrameLayout, IControlComponent {
    private var mControlWrapper: ControlWrapper? = null
    private var mThumb: ImageView? = null
    private var mStartPlay: ImageView? = null
    private var mLoading: ProgressBar? = null
    private var mNetWarning: FrameLayout? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Settings Click on this interface to start playing
     */
    fun setClickStart() {
        setOnClickListener { mControlWrapper!!.start() }
    }

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override val view: View
        get() = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {}
    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            VideoView.STATE_PREPARING -> {
                bringToFront()
                visibility = VISIBLE
                mStartPlay!!.visibility = GONE
                mNetWarning!!.visibility = GONE
                mLoading!!.visibility = VISIBLE
            }
            VideoView.STATE_PLAYING, VideoView.STATE_PAUSED, VideoView.STATE_ERROR, VideoView.STATE_BUFFERING, VideoView.STATE_BUFFERED, VideoView.STATE_PLAYBACK_COMPLETED -> visibility = GONE
            VideoView.STATE_IDLE -> {
                visibility = VISIBLE
                bringToFront()
                mLoading!!.visibility = GONE
                mNetWarning!!.visibility = GONE
                mStartPlay!!.visibility = VISIBLE
                mThumb!!.visibility = VISIBLE
            }
            VideoView.STATE_START_ABORT -> {
                visibility = VISIBLE
                mNetWarning!!.visibility = VISIBLE
                mNetWarning!!.bringToFront()
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {}
    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {}

    init {
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_prepare_view, this, true)
        mThumb = findViewById(R.id.thumb)
        mStartPlay = findViewById(R.id.start_play)
        mLoading = findViewById(R.id.loading)
        mNetWarning = findViewById(R.id.net_warning_layout)
        findViewById<View>(R.id.status_btn).setOnClickListener {
            mNetWarning?.visibility = GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Objects.requireNonNull(instance())!!.setPlayOnMobileNetwork(true)
            }
            mControlWrapper!!.start()
        }
    }
}