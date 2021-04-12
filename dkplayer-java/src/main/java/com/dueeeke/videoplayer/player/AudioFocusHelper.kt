package com.dueeeke.videoplayer.player

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Audio focus change monitoring
 */
class AudioFocusHelper(private val videoView: VideoView<*>?) : OnAudioFocusChangeListener {
    private val mHandler = Handler(Looper.getMainLooper())
    private val mAudioManager: AudioManager?
    private var mStartRequested = false
    private var mPausedForLoss = false
    private var mCurrentFocus = 0

    private val audioFocusRequest by lazy { buildFocusRequest() }

    @TargetApi(Build.VERSION_CODES.O)
    private fun buildFocusRequest(): AudioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(this)
                    .build()

    override fun onAudioFocusChange(focusChange: Int) {
        if (mCurrentFocus == focusChange) {
            return
        }

        //Because onAudioFocusChange may be called in a child thread,
        //So switch to the main thread to execute in this way
        mHandler.post { handleAudioFocusChange(focusChange) }
        mCurrentFocus = focusChange
    }

    private fun handleAudioFocusChange(focusChange: Int) {
//        val videoView = mWeakVideoView.get() ?: return
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                if (mStartRequested || mPausedForLoss) {
                    videoView?.start()
                    mStartRequested = false
                    mPausedForLoss = false
                }
                if (videoView?.isMute == false) //Restore volume
                    videoView.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (videoView?.isPlaying == true) {
                mPausedForLoss = true
                videoView.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (videoView?.isPlaying == true && !videoView.isMute) {
                videoView.setVolume(0.1f, 0.1f)
            }
        }
    }

    /**
     * Requests to obtain the audio focus
     */
    fun requestFocus() {
        if (mCurrentFocus == AudioManager.AUDIOFOCUS_GAIN) {
            return
        }
        if (mAudioManager == null) {
            return
        }
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.requestAudioFocus(audioFocusRequest)
        } else {
            mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
            mCurrentFocus = AudioManager.AUDIOFOCUS_GAIN
            return
        }
        mStartRequested = true
    }

    /**
     * Requests the system to drop the audio focus
     */
    fun abandonFocus() {
        if (mAudioManager == null) {
            return
        }
        mStartRequested = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            mAudioManager.abandonAudioFocus(this)
        }
    }

    init {
        mAudioManager = videoView?.context?.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}