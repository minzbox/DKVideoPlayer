package com.dueeeke.videoplayer.controller

import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.dueeeke.videoplayer.player.VideoView
import com.dueeeke.videoplayer.util.PlayerUtils.getScreenWidth
import com.dueeeke.videoplayer.util.PlayerUtils.isEdge
import com.dueeeke.videoplayer.util.PlayerUtils.scanForActivity
import kotlin.math.abs

/**
 * VideoController with gesture operation
 * Created by dueeeke on 2018/1/6.
 */
abstract class GestureVideoController : BaseVideoController, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, OnTouchListener {
    private var mGestureDetector: GestureDetector? = null
    private var mAudioManager: AudioManager? = null
    private var mIsGestureEnabled = true
    private var mStreamVolume = 0
    private var mBrightness = 0f
    private var mSeekPosition = 0
    private var mFirstTouch = false
    private var mChangePosition = false
    private var mChangeBrightness = false
    private var mChangeVolume = false
    private var mCanChangePosition = true
    private var mEnableInNormal = false
    private var mCanSlide = false
    private var mCurPlayState = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun initView() {
        super.initView()
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mGestureDetector = GestureDetector(context, this)
        setOnTouchListener(this)
    }

    /**
     * Set whether you can slide to adjust the progress, the default is
     */
    fun setCanChangePosition(canChangePosition: Boolean) {
        mCanChangePosition = canChangePosition
    }

    /**
     * Whether to start gesture control in portrait mode, it is closed by default
     */
    fun setEnableInNormal(enableInNormal: Boolean) {
        mEnableInNormal = enableInNormal
    }

    /**
     * Whether to turn on the gesture control, it is enabled by default.
     * After it is turned off, double-click to pause the playback and adjust the progress, volume,
     * and brightness of the gesture.
     */
    fun setGestureEnabled(gestureEnabled: Boolean) {
        mIsGestureEnabled = gestureEnabled
    }

    override fun setPlayerState(playerState: Int) {
        super.setPlayerState(playerState)
        if (playerState == VideoView.PLAYER_NORMAL) {
            mCanSlide = mEnableInNormal
        } else if (playerState == VideoView.PLAYER_FULL_SCREEN) {
            mCanSlide = true
        }
    }

    override fun setPlayState(playState: Int) {
        super.setPlayState(playState)
        mCurPlayState = playState
    }

    private val isInPlaybackState: Boolean
        get() = mControlWrapper != null && mCurPlayState != VideoView.STATE_ERROR && mCurPlayState != VideoView.STATE_IDLE && mCurPlayState != VideoView.STATE_PREPARING && mCurPlayState != VideoView.STATE_PREPARED && mCurPlayState != VideoView.STATE_START_ABORT && mCurPlayState != VideoView.STATE_PLAYBACK_COMPLETED

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return mGestureDetector!!.onTouchEvent(event)
    }

    /**
     * The moment the finger is pressed
     */
    override fun onDown(e: MotionEvent): Boolean {
        if (!isInPlaybackState //Not playing
                || !mIsGestureEnabled //Close up gesture
                || isEdge(context, e)) //At the edge of the screen
            return true
        mStreamVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val activity = scanForActivity(context)
        mBrightness = activity?.window?.attributes?.screenBrightness ?: 0f
        mFirstTouch = true
        mChangePosition = false
        mChangeBrightness = false
        mChangeVolume = false
        return true
    }

    /**
     * Click on
     */
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (isInPlaybackState) {
            mControlWrapper!!.toggleShowState()
        }
        return true
    }

    /**
     * Double click
     */
    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (!isLocked && isInPlaybackState) togglePlay()
        return true
    }

    /**
     * Swipe on the screen
     */
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!isInPlaybackState //Not playing
                || !mIsGestureEnabled //Close up gesture
                || !mCanSlide //Swipe gesture off
                || isLocked //Locked the screen
                || isEdge(context, e1)) //At the edge of the screen
            return true
        val deltaX = e1.x - e2.x
        val deltaY = e1.y - e2.y
        if (mFirstTouch) {
            mChangePosition = abs(distanceX) >= abs(distanceY)
            if (!mChangePosition) {
                //Half screen width
                val halfScreen = getScreenWidth(context, true) / 2
                if (e2.x > halfScreen) {
                    mChangeVolume = true
                } else {
                    mChangeBrightness = true
                }
            }
            if (mChangePosition) {
                //According to the user settings, whether the progress can be adjusted by sliding to determine whether the progress can be adjusted by sliding
                mChangePosition = mCanChangePosition
            }
            if (mChangePosition || mChangeBrightness || mChangeVolume) {
                for ((component) in mControlComponents) {
                    if (component is IGestureComponent) {
                        component.onStartSlide()
                    }
                }
            }
            mFirstTouch = false
        }
        when {
            mChangePosition -> {
                slideToChangePosition(deltaX)
            }
            mChangeBrightness -> {
                slideToChangeBrightness(deltaY)
            }
            mChangeVolume -> {
                slideToChangeVolume(deltaY)
            }
        }
        return true
    }

    private fun slideToChangePosition(deltaX: Float) {
        val mDeltaX = -deltaX
        val width = measuredWidth
        var duration = 0
        if (mControlWrapper != null) {
            duration = mControlWrapper!!.duration.toInt()
        }
        val currentPosition = mControlWrapper!!.currentPosition.toInt()
        var position = (mDeltaX / width * 120000 + currentPosition).toInt()
        if (position > duration) position = duration
        if (position < 0) position = 0
        for ((component) in mControlComponents) {
            if (component is IGestureComponent) {
                component.onPositionChange(position, currentPosition, duration)
            }
        }
        mSeekPosition = position
    }

    private fun slideToChangeBrightness(deltaY: Float) {
        val activity = scanForActivity(context) ?: return
        val window = activity.window
        val attributes = window.attributes
        val height = measuredHeight
        if (mBrightness == -1.0f) mBrightness = 0.5f
        var brightness = deltaY * 2 / height * 1.0f + mBrightness
        if (brightness < 0) {
            brightness = 0f
        }
        if (brightness > 1.0f) brightness = 1.0f
        val percent = (brightness * 100).toInt()
        attributes.screenBrightness = brightness
        window.attributes = attributes
        for ((component) in mControlComponents) {
            if (component is IGestureComponent) {
                component.onBrightnessChange(percent)
            }
        }
    }

    private fun slideToChangeVolume(deltaY: Float) {
        val streamMaxVolume = mAudioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val height = measuredHeight
        val deltaV = deltaY * 2 / height * streamMaxVolume
        var index = mStreamVolume + deltaV
        if (index > streamMaxVolume) index = streamMaxVolume.toFloat()
        if (index < 0) index = 0f
        val percent = (index / streamMaxVolume * 100).toInt()
        mAudioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, index.toInt(), 0)
        for ((component) in mControlComponents) {
            if (component is IGestureComponent) {
                component.onVolumeChange(percent)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //Event handling when sliding ends
        if (!mGestureDetector!!.onTouchEvent(event)) {
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopSlide()
                    if (mSeekPosition > 0) {
                        if (mControlWrapper != null) {
                            mControlWrapper!!.seekTo(mSeekPosition.toLong())
                        }
                        mSeekPosition = 0
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopSlide()
                    mSeekPosition = 0
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun stopSlide() {
        for ((component) in mControlComponents) {
            if (component is IGestureComponent) {
                component.onStopSlide()
            }
        }
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}
    override fun onShowPress(e: MotionEvent) {}
    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }
}