package com.dueeeke.videoplayer.player

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.dueeeke.videoplayer.R
import com.dueeeke.videoplayer.controller.BaseVideoController
import com.dueeeke.videoplayer.controller.IMediaPlayerControl
import com.dueeeke.videoplayer.player.AbstractPlayer.PlayerEventListener
import com.dueeeke.videoplayer.player.VideoViewManager.Companion.config
import com.dueeeke.videoplayer.render.IRenderView
import com.dueeeke.videoplayer.render.RenderViewFactory
import com.dueeeke.videoplayer.util.L
import com.dueeeke.videoplayer.util.PlayerUtils
import java.io.IOException
import java.util.*

/**
 * Player
 * Created by NghiaNv on 2017/4/7.
 */
open class VideoView<P : AbstractPlayer?> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), IMediaPlayerControl, PlayerEventListener, LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        release()
    }

    companion object {
        const val SCREEN_SCALE_DEFAULT = 0
        const val SCREEN_SCALE_16_9 = 1
        const val SCREEN_SCALE_4_3 = 2
        const val SCREEN_SCALE_MATCH_PARENT = 3
        const val SCREEN_SCALE_ORIGINAL = 4
        const val SCREEN_SCALE_CENTER_CROP = 5

        //Various states of the player
        const val STATE_ERROR = -1
        const val STATE_IDLE = 0
        const val STATE_PREPARING = 1
        const val STATE_PREPARED = 2
        const val STATE_PLAYING = 3
        const val STATE_PAUSED = 4
        const val STATE_PLAYBACK_COMPLETED = 5
        const val STATE_BUFFERING = 6
        const val STATE_BUFFERED = 7
        const val STATE_START_ABORT = 8 //Start playing aborted
        const val PLAYER_NORMAL = 10 // Normal player
        const val PLAYER_FULL_SCREEN = 11
        const val PLAYER_TINY_SCREEN = 12
    }

    @JvmField
    protected var mMediaPlayer: P? = null
    private var mPlayerFactory: PlayerFactory<P>? //Factory class, used to instantiate the playback core

    @JvmField
    protected var mVideoController: BaseVideoController? = null //controller

    /**
     * The container that actually holds the player view
     */
    @JvmField
    protected var mPlayerContainer: FrameLayout? = null
    private var mRenderView: IRenderView? = null
    private var mRenderViewFactory: RenderViewFactory?
    private var mCurrentScreenScaleType: Int
    private var mVideoSize = intArrayOf(0, 0)
    private var mIsMute = false //Whether to mute

    //--------- data sources ---------//
    private var mUrl: String? = null //The address of the currently playing video
    private var mHeaders: Map<String, String>? = null  //The request header of the current video address
    private var mAssetFileDescriptor: AssetFileDescriptor? = null

    @JvmField
    protected var mCurrentPosition: Long = 0 //The position where the video is currently playing

    /**
     * Get the current playback status
     */
    var currentPlayState = STATE_IDLE  //Current player status

    /**
     * Get the status of the current player
     */
    var currentPlayerState = PLAYER_NORMAL

    private var mIsFullScreen = false
    private var mIsTinyScreen = false
    private var mTinyScreenSize = intArrayOf(0, 0)

    /**
     * The audio focus changes in the monitoring system, see [.setEnableAudioFocus]
     */
    private var mEnableAudioFocus: Boolean
    private var mAudioFocusHelper: AudioFocusHelper? = null

    /**
     * OnStateChangeListener collection, which saves all the listeners set by developers
     */
    private var mOnStateChangeListeners: MutableList<OnStateChangeListener>? = null

    /**
     * Progress manager, after setting, the player will record the playback progress so that the progress can be resumed next time.
     */
    private var mProgressManager: ProgressManager?

    /**
     * Loop option
     */
    private var mIsLooping: Boolean

    /**
     * [.mPlayerContainer] background color, default black
     */
    private val mPlayerBackgroundColor: Int

    init {

        //Read global configuration
        val config = config
        mEnableAudioFocus = config!!.mEnableAudioFocus
        mProgressManager = config.mProgressManager
        mPlayerFactory = config.mPlayerFactory as PlayerFactory<P>?
        mCurrentScreenScaleType = config.mScreenScaleType
        mRenderViewFactory = config.mRenderViewFactory

        //Read the configuration in xml and synthesize the global configuration
        val a = context.obtainStyledAttributes(attrs, R.styleable.VideoView)
        mEnableAudioFocus = a.getBoolean(R.styleable.VideoView_enableAudioFocus, mEnableAudioFocus)
        mIsLooping = a.getBoolean(R.styleable.VideoView_looping, false)
        mCurrentScreenScaleType = a.getInt(R.styleable.VideoView_screenScaleType, mCurrentScreenScaleType)
        mPlayerBackgroundColor = a.getColor(R.styleable.VideoView_playerBackgroundColor, Color.BLACK)
        a.recycle()
        initView()
    }

    /**
     * Initialize the player view
     */
    protected fun initView() {
        mPlayerContainer = FrameLayout(context)
        mPlayerContainer!!.setBackgroundColor(mPlayerBackgroundColor)
        val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        this.addView(mPlayerContainer, params)
    }

    /**
     * Set the background color of [.mPlayerContainer]
     */
    fun setPlayerBackgroundColor(color: Int) {
        mPlayerContainer!!.setBackgroundColor(color)
    }

    /**
     * Start playing, note: you must call [.release] to release the player after calling this method, otherwise it will cause a memory leak
     */
    override fun start() {
        var isStarted = false
        if (isInIdleState || isInStartAbortState) {
            isStarted = startPlay()
        } else if (isInPlaybackState) {
            startInPlaybackState()
            isStarted = true
        }
        if (isStarted) {
            mPlayerContainer!!.keepScreenOn = true
            if (mAudioFocusHelper != null) mAudioFocusHelper!!.requestFocus()
        }
    }

    /**
     * Play for the first time
     * @return Whether to start playing successfully
     */
    private fun startPlay(): Boolean {
        //Do not continue playing if you want to display mobile network prompts
        if (showNetWarning()) {
            //stop playing
            setPlayState(STATE_START_ABORT)
            return false
        }
        //Monitor audio focus changes
        if (mEnableAudioFocus) {
            mAudioFocusHelper = AudioFocusHelper(this)
        }
        //Read playback progress
        if (mProgressManager != null) {
            mCurrentPosition = mProgressManager!!.getSavedProgress(mUrl)
        }
        initPlayer()
        addDisplay()
        startPrepare(false)
        return true
    }

    /**
     * Whether to display the mobile network prompt, it can be configured in the Controller
     */
    private fun showNetWarning(): Boolean {
        //Do not detect the network when playing local data sources
        return if (isLocalDataSource) false else mVideoController != null && mVideoController!!.showNetWarning()
    }

    /**
     * Determine whether it is a local data source, including local files, Assets, raw
     */
    private val isLocalDataSource: Boolean
        get() {
            if (mAssetFileDescriptor != null) {
                return true
            } else if (!TextUtils.isEmpty(mUrl)) {
                val uri = Uri.parse(mUrl)
                return ContentResolver.SCHEME_ANDROID_RESOURCE == uri.scheme || ContentResolver.SCHEME_FILE == uri.scheme || "rawresource" == uri.scheme
            }
            return false
        }

    /**
     * Initialize player
     */
    protected open fun initPlayer() {
        mMediaPlayer = mPlayerFactory!!.createPlayer(context)
        mMediaPlayer!!.setPlayerEventListener(this)
        setInitOptions()
        mMediaPlayer!!.initPlayer()
        setOptions()
    }

    /**
     * Configuration items before initialization
     */
    protected open fun setInitOptions() {}

    /**
     * Configuration items after initialization
     */
    protected open fun setOptions() {
        mMediaPlayer!!.setLooping(mIsLooping)
    }

    /**
     * Initialize the video rendering View
     */
    private fun addDisplay() {
        if (mRenderView != null) {
            mPlayerContainer!!.removeView(mRenderView!!.view)
            mRenderView!!.release()
        }
        mRenderView = mRenderViewFactory!!.createRenderView(context)
        mMediaPlayer?.let { mRenderView?.attachToPlayer(it) }
        val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER)
        mPlayerContainer?.addView(mRenderView?.view, 0, params)
    }

    /**
     * Start preparing to play (direct play)
     */
    protected open fun startPrepare(reset: Boolean) {
        if (reset) {
            mMediaPlayer!!.reset()
            //Reset option, after media player reset, option will be invalid
            setOptions()
        }
        if (prepareDataSource()) {
            mMediaPlayer!!.prepareAsync()
            setPlayState(STATE_PREPARING)
            setPlayerState(if (isFullScreen) PLAYER_FULL_SCREEN else if (isTinyScreen) PLAYER_TINY_SCREEN else PLAYER_NORMAL)
        }
    }

    /**
     * Set playback data
     * @return Whether the playback data is set successfully
     */
    protected open fun prepareDataSource(): Boolean {
        if (mAssetFileDescriptor != null) {
            mMediaPlayer!!.setDataSource(mAssetFileDescriptor)
            return true
        } else if (!TextUtils.isEmpty(mUrl)) {
            mMediaPlayer!!.setDataSource(mUrl, mHeaders)
            return true
        }
        return false
    }

    /**
     * Start playing in the playback state
     */
    protected open fun startInPlaybackState() {
        mMediaPlayer!!.start()
        setPlayState(STATE_PLAYING)
    }

    /**
     * Pause playback
     */
    override fun pause() {
        if (isInPlaybackState
                && mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.pause()
            setPlayState(STATE_PAUSED)
            if (mAudioFocusHelper != null) {
                mAudioFocusHelper!!.abandonFocus()
            }
            mPlayerContainer!!.keepScreenOn = false
        }
    }

    /**
     * Resume playback
     */
    open fun resume() {
        if (isInPlaybackState
                && !mMediaPlayer!!.isPlaying) {
            mMediaPlayer!!.start()
            setPlayState(STATE_PLAYING)
            if (mAudioFocusHelper != null) {
                mAudioFocusHelper!!.requestFocus()
            }
            mPlayerContainer!!.keepScreenOn = true
        }
    }

    /**
     * Release player
     */
    open fun release() {
        if (!isInIdleState) {
            //Release player
            if (mMediaPlayer != null) {
                mMediaPlayer!!.release()
                mMediaPlayer = null
            }
            //Release renderView
            if (mRenderView != null) {
                mPlayerContainer!!.removeView(mRenderView!!.view)
                mRenderView!!.release()
                mRenderView = null
            }
            //Release Assets resources
            if (mAssetFileDescriptor != null) {
                try {
                    mAssetFileDescriptor!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            //Turn off AudioFocus monitoring
            if (mAudioFocusHelper != null) {
                mAudioFocusHelper!!.abandonFocus()
                mAudioFocusHelper = null
            }
            //Turn off the screen is always on
            mPlayerContainer!!.keepScreenOn = false
            //Save playback progress
            saveProgress()
            //Reset playback progress
            mCurrentPosition = 0
            //Switch state
            setPlayState(STATE_IDLE)
        }
    }

    /**
     * Save current position of playback
     */
    private fun saveProgress() {
        if (mProgressManager != null && mCurrentPosition > 0) {
            L.d("saveProgress: $mCurrentPosition")
            mProgressManager!!.saveProgress(mUrl, mCurrentPosition)
        }
    }

    /**
     * Is it playing
     */
    protected val isInPlaybackState: Boolean
        get() = mMediaPlayer != null && currentPlayState != STATE_ERROR && currentPlayState != STATE_IDLE && currentPlayState != STATE_PREPARING && currentPlayState != STATE_START_ABORT && currentPlayState != STATE_PLAYBACK_COMPLETED

    /**
     * Is it in the un-play state
     */
    private val isInIdleState: Boolean
        get() = currentPlayState == STATE_IDLE

    /**
     * Play paused state
     */
    private val isInStartAbortState: Boolean
        get() = currentPlayState == STATE_START_ABORT

    /**
     * Replay
     * @param resetPosition Whether to start playing from the beginning
     */
    override fun replay(resetPosition: Boolean) {
        if (resetPosition) {
            mCurrentPosition = 0
        }
        addDisplay()
        startPrepare(true)
        mPlayerContainer!!.keepScreenOn = true
    }

    override val duration: Long
        get() = if (isInPlaybackState) {
            mMediaPlayer!!.duration
        } else 0

    override val currentPosition: Long
        get() = if (isInPlaybackState) {
            mCurrentPosition = mMediaPlayer!!.currentPosition
            mCurrentPosition
        } else 0

    /**
     * Adjust playback progress
     */
    override fun seekTo(pos: Long) {
        if (isInPlaybackState) {
            mMediaPlayer!!.seekTo(pos)
        }
    }

    /**
     * Is it playing
     */
    override val isPlaying: Boolean
        get() = isInPlaybackState && mMediaPlayer!!.isPlaying

    /**
     * Get the current buffer percentage
     */
    override val bufferedPercentage: Int
        get() = mMediaPlayer?.bufferedPercentage ?: 0

    /**
     * Set mute
     */

    override var isMute: Boolean
        get() = mIsMute
        set(value) {
            if (mMediaPlayer != null) {
                mIsMute = value
                val volume = if (value) 0.0f else 1.0f
                mMediaPlayer?.setVolume(volume, volume)
            }
        }

    /**
     * Video playback error callback
     */
    override fun onError() {
        mPlayerContainer!!.keepScreenOn = false
        setPlayState(STATE_ERROR)
    }

    /**
     * Video playback complete callback
     */
    override fun onCompletion() {
        mPlayerContainer!!.keepScreenOn = false
        mCurrentPosition = 0
        if (mProgressManager != null) {
            //Play is complete, clear the progress
            mProgressManager!!.saveProgress(mUrl, 0)
        }
        setPlayState(STATE_PLAYBACK_COMPLETED)
    }

    override fun onInfo(what: Int, extra: Int) {
        when (what) {
            AbstractPlayer.MEDIA_INFO_BUFFERING_START -> setPlayState(STATE_BUFFERING)
            AbstractPlayer.MEDIA_INFO_BUFFERING_END -> setPlayState(STATE_BUFFERED)
            AbstractPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                setPlayState(STATE_PLAYING)
                if (mPlayerContainer!!.windowVisibility != VISIBLE) {
                    pause()
                }
            }
            AbstractPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> if (mRenderView != null) mRenderView!!.setVideoRotation(extra)
        }
    }

    /**
     * Call back when the video is buffered and ready to start playing
     */
    override fun onPrepared() {
        setPlayState(STATE_PREPARED)
        if (mCurrentPosition > 0) {
            seekTo(mCurrentPosition)
        }
    }

    /**
     * Get buffer speed
     */
    override val tcpSpeed: Long
        get() = mMediaPlayer?.tcpSpeed ?: 0

    /**
     * Set playback speed
     */
    override var speed: Float
        get() = if (isInPlaybackState) {
            mMediaPlayer!!.speed
        } else 1f
        set(value) {
            if (isInPlaybackState) {
                mMediaPlayer!!.speed = value
            }
        }

    /**
     * Set video address
     */
    fun setUrl(url: String?) {
        setUrl(url, null)
    }

    /**
     * Set the video address containing the request header information
     * @param url video address
     * @param headers request header
     */
    open fun setUrl(url: String?, headers: Map<String, String>?) {
        mAssetFileDescriptor = null
        mUrl = url
        mHeaders = headers
    }

    /**
     * Used to play video files in assets
     */
    fun setAssetFileDescriptor(fd: AssetFileDescriptor?) {
        mUrl = null
        mAssetFileDescriptor = fd
    }

    /**
     * Seek to the preset position at the beginning of playback
     */
    open fun skipPositionWhenPlay(position: Int) {
        mCurrentPosition = position.toLong()
    }

    /**
     * Set the volume between 0.0f-1.0f
     * @param v1 left channel volume
     * @param v2 right channel volume
     */
    fun setVolume(v1: Float, v2: Float) {
        mMediaPlayer?.setVolume(v1, v2)
    }

    /**
     * Set the progress manager to save the playback progress
     */
    fun setProgressManager(progressManager: ProgressManager?) {
        mProgressManager = progressManager
    }

    /**
     * Loop playback, not loop playback by default
     */
    fun setLooping(looping: Boolean) {
        mIsLooping = looping
        mMediaPlayer?.setLooping(looping)
    }

    /**
     * Whether to enable AudioFocus monitoring, enabled by default, used to monitor whether the audio focus is acquired in other places, and if it is acquired in other places
     * Audio focus, this player will respond accordingly, see [AudioFocusHelper] for specific implementation
     */
    fun setEnableAudioFocus(enableAudioFocus: Boolean) {
        mEnableAudioFocus = enableAudioFocus
    }

    /**
     * Custom play core, inherit [PlayerFactory] to realize your own play core
     */
    fun setPlayerFactory(playerFactory: PlayerFactory<P>?) {
        requireNotNull(playerFactory) { "PlayerFactory can not be null!" }
        mPlayerFactory = playerFactory
    }

    /**
     * Custom RenderView, inherit [RenderViewFactory] to implement your own RenderView
     */
    fun setRenderViewFactory(renderViewFactory: RenderViewFactory?) {
        requireNotNull(renderViewFactory) { "RenderViewFactory can not be null!" }
        mRenderViewFactory = renderViewFactory
    }

    /**
     * Enter full screen
     */
    override fun startFullScreen() {
        if (mIsFullScreen) return
        val decorView = decorView ?: return
        mIsFullScreen = true

        //Hide NavigationBar and StatusBar
//        hideSysBar()
        PlayerUtils.hideSystemUI(activity)
        //Remove the player view from the current FrameLayout
        removeView(mPlayerContainer)
        //Add the player view to DecorView to achieve full screen
        decorView.addView(mPlayerContainer)
        setPlayerState(PLAYER_FULL_SCREEN)
    }

    private fun hideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.setDecorFitsSystemWindows(false)
        } else {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && mIsFullScreen) {
            //Keep full screen when regaining focus
//            hideSysBar()
            PlayerUtils.hideSystemUI(activity)
        }
    }

    /**
     * Exit fullscreen
     */
    override fun stopFullScreen() {
        if (!mIsFullScreen) return
        val decorView = decorView ?: return
        mIsFullScreen = false

        //Display NavigationBar and StatusBar
//        showSysBar()
        PlayerUtils.showSystemUI(activity)
        //Remove the player view from DecorView and add it to the current FrameLayout to exit the full screen
        decorView.removeView(mPlayerContainer)
        this.addView(mPlayerContainer)
        setPlayerState(PLAYER_NORMAL)
    }

    private fun showSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.window?.setDecorFitsSystemWindows(true)
        } else {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * Get decorView
     */
    private val decorView: ViewGroup?
        get() {
            val activity = activity ?: return null
            return activity.window.decorView as ViewGroup
        }

    /**
     * Get the content view in the activity, its id is android.R.id.content
     */
    private val contentView: ViewGroup?
        get() {
            val activity = activity ?: return null
            return activity.findViewById(android.R.id.content)
        }

    /**
     * Get Activity, first get Activity through Controller
     */
    protected val activity: Activity?
        get() {
            var activity: Activity?
            if (mVideoController != null) {
                activity = PlayerUtils.scanForActivity(mVideoController!!.context)
                if (activity == null) {
                    activity = PlayerUtils.scanForActivity(context)
                }
            } else {
                activity = PlayerUtils.scanForActivity(context)
            }
            return activity
        }

    /**
     * Determine whether it is in full screen state
     */
    override val isFullScreen: Boolean
        get() = mIsFullScreen

    /**
     * Turn on small screen
     */
    override fun startTinyScreen() {
        if (mIsTinyScreen) return
        val contentView = contentView ?: return
        removeView(mPlayerContainer)
        var width = mTinyScreenSize[0]
        if (width <= 0) {
            width = PlayerUtils.getScreenWidth(context, false) / 2
        }
        var height = mTinyScreenSize[1]
        if (height <= 0) {
            height = width * 9 / 16
        }
        val params = LayoutParams(width, height)
        params.gravity = Gravity.BOTTOM or Gravity.END
        contentView.addView(mPlayerContainer, params)
        mIsTinyScreen = true
        setPlayerState(PLAYER_TINY_SCREEN)
    }

    /**
     * Exit small screen
     */
    override fun stopTinyScreen() {
        if (!mIsTinyScreen) return
        val contentView = contentView ?: return
        contentView.removeView(mPlayerContainer)
        val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        this.addView(mPlayerContainer, params)
        mIsTinyScreen = false
        setPlayerState(PLAYER_NORMAL)
    }

    override val isTinyScreen: Boolean
        get() = mIsTinyScreen

    override fun onVideoSizeChanged(width: Int, height: Int) {
        mVideoSize[0] = width
        mVideoSize[1] = height
        if (mRenderView != null) {
            mRenderView!!.setScaleType(mCurrentScreenScaleType)
            mRenderView!!.setVideoSize(width, height)
        }
    }

    /**
     * Set the controller, pass null to remove the controller
     */
    fun setVideoController(mediaController: BaseVideoController?) {
        mPlayerContainer!!.removeView(mVideoController)
        mVideoController = mediaController
        if (mediaController != null) {
            mediaController.setMediaPlayer(this)
            val params = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            mPlayerContainer!!.addView(mVideoController, params)
        }
    }

    /**
     * Set video ratio
     */
    override fun setScreenScaleType(screenScaleType: Int) {
        mCurrentScreenScaleType = screenScaleType
        if (mRenderView != null) {
            mRenderView!!.setScaleType(screenScaleType)
        }
    }

    /**
     * Set mirror rotation, SurfaceView is not currently supported
     */
    override fun setMirrorRotation(enable: Boolean) {
        mRenderView?.view?.scaleX = if (enable) -1f else 1f
    }

    /**
     * Screenshot, SurfaceView is not currently supported
     */
    override fun doScreenShot(): Bitmap? {
        return if (mRenderView != null) {
            mRenderView!!.doScreenShot()
        } else null
    }

    /**
     * Get the width and height of the video, where width: mVideoSize[0], height: mVideoSize[1]
     */
    override val videoSize: IntArray?
        get() = mVideoSize

    /**
     * Rotate video screen
     * @param rotation angle
     */
    override fun setRotation(rotation: Float) {
        if (mRenderView != null) {
            mRenderView!!.setVideoRotation(rotation.toInt())
        }
    }

    /**
     * Set the width and height of the small screen
     *
     * @param tinyScreenSize where tinyScreenSize[0] is wide and tinyScreenSize[1] is high
     */
    fun setTinyScreenSize(tinyScreenSize: IntArray) {
        mTinyScreenSize = tinyScreenSize
    }

    /**
     * Set the playback state to the Controller, which is used to control the ui display of the Controller
     */
    protected fun setPlayState(playState: Int) {
        currentPlayState = playState
        if (mVideoController != null) {
            mVideoController!!.setPlayState(playState)
        }
        if (mOnStateChangeListeners != null) {
            for (l in PlayerUtils.getSnapshot(mOnStateChangeListeners!!)) {
                l.onPlayStateChanged(playState)
            }
        }
    }

    /**
     * Set the player state to the Controller, including full-screen state and non-full-screen state
     */
    private fun setPlayerState(playerState: Int) {
        currentPlayerState = playerState
        if (mVideoController != null) {
            mVideoController!!.setPlayerState(playerState)
        }
        if (mOnStateChangeListeners != null) {
            for (l in PlayerUtils.getSnapshot(mOnStateChangeListeners!!)) {
                l.onPlayerStateChanged(playerState)
            }
        }
    }

    /**
     * Play state change listener
     */
    interface OnStateChangeListener {
        fun onPlayerStateChanged(playerState: Int)
        fun onPlayStateChanged(playState: Int)
    }

    /**
     * Null implementation of OnStateChangeListener. Only need to rewrite the required method when using
     */
    open class SimpleOnStateChangeListener : OnStateChangeListener {
        override fun onPlayerStateChanged(playerState: Int) {}
        override fun onPlayStateChanged(playState: Int) {}
    }

    /**
     * Add a playback status listener, which will be called when the playback status changes.
     */
    fun addOnStateChangeListener(listener: OnStateChangeListener) {
        if (mOnStateChangeListeners == null) {
            mOnStateChangeListeners = ArrayList()
        }
        mOnStateChangeListeners!!.add(listener)
    }

    /**
     * Remove a certain playback status monitor
     */
    fun removeOnStateChangeListener(listener: OnStateChangeListener) {
        if (mOnStateChangeListeners != null) {
            mOnStateChangeListeners!!.remove(listener)
        }
    }

    /**
     * Set up a playback status listener, which will be called when the playback status changes,
     * If you want to set up multiple listeners at the same time, [.addOnStateChangeListener] is recommended.
     */
    fun setOnStateChangeListener(listener: OnStateChangeListener) {
        if (mOnStateChangeListeners == null) {
            mOnStateChangeListeners = ArrayList()
        } else {
            mOnStateChangeListeners!!.clear()
        }
        mOnStateChangeListeners!!.add(listener)
    }

    /**
     * Remove all playback status monitors
     */
    fun clearOnStateChangeListeners() {
        if (mOnStateChangeListeners != null) {
            mOnStateChangeListeners!!.clear()
        }
    }

    /**
     * Change the return key logic for activity
     */
    fun onBackPressed(): Boolean {
        return mVideoController != null && mVideoController!!.onBackPressed()
    }

    override fun onSaveInstanceState(): Parcelable? {
        L.d("onSaveInstanceState: $mCurrentPosition")
        //After the activity is cut to the background, it may be recycled by the system, so the progress is saved here
        saveProgress()
        return super.onSaveInstanceState()
    }
}