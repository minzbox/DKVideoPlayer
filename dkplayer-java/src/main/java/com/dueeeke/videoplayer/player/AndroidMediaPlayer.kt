package com.dueeeke.videoplayer.player

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder

/**
 * The MediaPlayer of the packaging system is not recommended.
 * The compatibility of the system's MediaPlayer is poor.
 * It is recommended to use IjkPlayer or ExoPlayer
 */
class AndroidMediaPlayer(context: Context) : AbstractPlayer() {
    private var mMediaPlayer: MediaPlayer? = null
    override var bufferedPercentage = 0
        private set
    private var mAppContext: Context = context.applicationContext
    private var mIsPreparing = false
    override fun initPlayer() {
        mMediaPlayer = MediaPlayer()
        setOptions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaPlayer!!.setAudioAttributes(AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
        } else {
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
        mMediaPlayer!!.setOnErrorListener(onErrorListener)
        mMediaPlayer!!.setOnCompletionListener(onCompletionListener)
        mMediaPlayer!!.setOnInfoListener(onInfoListener)
        mMediaPlayer!!.setOnBufferingUpdateListener(onBufferingUpdateListener)
        mMediaPlayer!!.setOnPreparedListener(onPreparedListener)
        mMediaPlayer!!.setOnVideoSizeChangedListener(onVideoSizeChangedListener)
    }

    override fun setDataSource(path: String?, headers: Map<String, String>?) {
        try {
            mMediaPlayer!!.setDataSource(mAppContext, Uri.parse(path), headers)
        } catch (e: Exception) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun setDataSource(fd: AssetFileDescriptor?) {
        try {
            mMediaPlayer!!.setDataSource(fd!!.fileDescriptor, fd.startOffset, fd.length)
        } catch (e: Exception) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun start() {
        try {
            mMediaPlayer!!.start()
        } catch (e: IllegalStateException) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun pause() {
        try {
            mMediaPlayer!!.pause()
        } catch (e: IllegalStateException) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun stop() {
        try {
            mMediaPlayer!!.stop()
        } catch (e: IllegalStateException) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun prepareAsync() {
        try {
            mIsPreparing = true
            mMediaPlayer!!.prepareAsync()
        } catch (e: IllegalStateException) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun reset() {
        mMediaPlayer!!.reset()
        mMediaPlayer!!.setSurface(null)
        mMediaPlayer!!.setDisplay(null)
        mMediaPlayer!!.setVolume(1f, 1f)
    }

    override val isPlaying: Boolean
        get() = mMediaPlayer!!.isPlaying

    override fun seekTo(time: Long) {
        try {
            mMediaPlayer!!.seekTo(time.toInt())
        } catch (e: IllegalStateException) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun release() {
        mMediaPlayer!!.setOnErrorListener(null)
        mMediaPlayer!!.setOnCompletionListener(null)
        mMediaPlayer!!.setOnInfoListener(null)
        mMediaPlayer!!.setOnBufferingUpdateListener(null)
        mMediaPlayer!!.setOnPreparedListener(null)
        mMediaPlayer!!.setOnVideoSizeChangedListener(null)
        object : Thread() {
            override fun run() {
                try {
                    mMediaPlayer!!.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    override val currentPosition: Long
        get() = mMediaPlayer!!.currentPosition.toLong()
    override val duration: Long
        get() = mMediaPlayer!!.duration.toLong()

    override fun setSurface(surface: Surface?) {
        try {
            mMediaPlayer!!.setSurface(surface)
        } catch (e: Exception) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        try {
            mMediaPlayer!!.setDisplay(holder)
        } catch (e: Exception) {
            mPlayerEventListener!!.onError()
        }
    }

    override fun setVolume(v1: Float, v2: Float) {
        mMediaPlayer!!.setVolume(v1, v2)
    }

    override fun setLooping(isLooping: Boolean) {
        mMediaPlayer!!.isLooping = isLooping
    }

    override fun setOptions() {}

    // only support above Android M
    // only support above Android M
    override var speed: Float
        get() {
            // only support above Android M
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    return mMediaPlayer!!.playbackParams.speed
                } catch (e: Exception) {
                    mPlayerEventListener!!.onError()
                }
            }
            return 1f
        }
        set(speed) {
            // only support above Android M
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mMediaPlayer!!.playbackParams = mMediaPlayer!!.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    mPlayerEventListener!!.onError()
                }
            }
        }

    // no support
    override val tcpSpeed: Long
        get() = 0 // no support
    private val onErrorListener = MediaPlayer.OnErrorListener { _, _, _ ->
        mPlayerEventListener!!.onError()
        true
    }
    private val onCompletionListener = MediaPlayer.OnCompletionListener { mPlayerEventListener!!.onCompletion() }
    private val onInfoListener = MediaPlayer.OnInfoListener { _, what, extra -> //Solve the problem of multiple callbacks of MEDIA_INFO_VIDEO_RENDERING_START
        if (what == MEDIA_INFO_VIDEO_RENDERING_START) {
            if (mIsPreparing) {
                mPlayerEventListener!!.onInfo(what, extra)
                mIsPreparing = false
            }
        } else {
            mPlayerEventListener!!.onInfo(what, extra)
        }
        true
    }
    private val onBufferingUpdateListener: MediaPlayer.OnBufferingUpdateListener = MediaPlayer.OnBufferingUpdateListener { _, percent -> bufferedPercentage = percent }
    private val onPreparedListener = MediaPlayer.OnPreparedListener {
        mPlayerEventListener!!.onPrepared()
        start()
    }
    private val onVideoSizeChangedListener = MediaPlayer.OnVideoSizeChangedListener { mp, width, height ->
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        if (videoWidth != 0 && videoHeight != 0) {
            mPlayerEventListener!!.onVideoSizeChanged(videoWidth, videoHeight)
        }
    }

}