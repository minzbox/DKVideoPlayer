package com.dueeeke.videoplayer.player

import android.content.res.AssetFileDescriptor
import android.view.Surface
import android.view.SurfaceHolder

/**
 * Abstract player, inherit this interface to extend your own player
 * Created by NghiaNV on 2017/12/21.
 */
abstract class AbstractPlayer {
    /**
     * Player event callback
     */
    @JvmField
    protected var mPlayerEventListener: PlayerEventListener? = null

    /**
     * Initialize the player instance
     */
    abstract fun initPlayer()

    /**
     * Set data source
     */
    abstract fun setDataSource(path: String?, headers: Map<String, String>?)

    /**
     * Set raw data source
     */
    abstract fun setDataSource(fd: AssetFileDescriptor?)

    /**
     * Start
     */
    abstract fun start()

    /**
     * Pause
     */
    abstract fun pause()

    /**
     * Stop
     */
    abstract fun stop()

    /**
     * Ready to start playing (asynchronous)
     */
    abstract fun prepareAsync()

    /**
     * Reset player
     */
    abstract fun reset()

    /**
     * Is it playing
     */
    abstract val isPlaying: Boolean

    /**
     * Adjust progress
     */
    abstract fun seekTo(time: Long)

    /**
     * Release player
     */
    abstract fun release()

    /**
     * Get the current playing position
     */
    abstract val currentPosition: Long

    /**
     * Get the total duration of the video
     */
    abstract val duration: Long

    /**
     * Get buffer percentage
     */
    abstract val bufferedPercentage: Int

    /**
     * Set the View for rendering the video, mainly used for TextureView
     */
    abstract fun setSurface(surface: Surface?)

    /**
     * Set the View for rendering the video, mainly used for SurfaceView
     */
    abstract fun setDisplay(holder: SurfaceHolder?)

    abstract fun setVolume(v1: Float, v2: Float)

    abstract fun setLooping(isLooping: Boolean)

    /**
     * Set other playback configuration
     */
    abstract fun setOptions()

    abstract var speed: Float

    /**
     * Get the current buffered internet speed
     */
    abstract val tcpSpeed: Long

    /**
     * Bind VideoView
     */
    fun setPlayerEventListener(playerEventListener: PlayerEventListener?) {
        mPlayerEventListener = playerEventListener
    }

    interface PlayerEventListener {
        fun onError()
        fun onCompletion()
        fun onInfo(what: Int, extra: Int)
        fun onPrepared()
        fun onVideoSizeChanged(width: Int, height: Int)
    }

    companion object {
        /**
         * Start to render the video screen
         */
        const val MEDIA_INFO_VIDEO_RENDERING_START = 3

        /**
         * Buffer start
         */
        const val MEDIA_INFO_BUFFERING_START = 701

        /**
         * Buffer end
         */
        const val MEDIA_INFO_BUFFERING_END = 702

        /**
         * Video rotation information
         */
        const val MEDIA_INFO_VIDEO_ROTATION_CHANGED = 10001
    }
}