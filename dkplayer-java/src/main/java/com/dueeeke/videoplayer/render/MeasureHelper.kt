package com.dueeeke.videoplayer.render

import android.view.View
import com.dueeeke.videoplayer.player.VideoView

class MeasureHelper {
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var mCurrentScreenScale = 0
    private var mVideoRotationDegree = 0
    fun setVideoRotation(videoRotationDegree: Int) {
        mVideoRotationDegree = videoRotationDegree
    }

    fun setVideoSize(width: Int, height: Int) {
        mVideoWidth = width
        mVideoHeight = height
    }

    fun setScreenScale(screenScale: Int) {
        mCurrentScreenScale = screenScale
    }

    /**
     * Note: The width and height of the VideoView must be fixed, otherwise the following algorithm is not valid
     */
    fun doMeasure(widthMS: Int, heightMS: Int): IntArray {
        var widthMeasureSpec = widthMS
        var heightMeasureSpec = heightMS
        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) { // Processing rotation information during soft decoding, exchanging width and height
            widthMeasureSpec += heightMeasureSpec
            heightMeasureSpec = widthMeasureSpec - heightMeasureSpec
            widthMeasureSpec -= heightMeasureSpec
        }
        var width = View.MeasureSpec.getSize(widthMeasureSpec)
        var height = View.MeasureSpec.getSize(heightMeasureSpec)
        if (mVideoHeight == 0 || mVideoWidth == 0) {
            return intArrayOf(width, height)
        }
        when (mCurrentScreenScale) {
            VideoView.SCREEN_SCALE_DEFAULT -> if (mVideoWidth * height < width * mVideoHeight) {
                width = height * mVideoWidth / mVideoHeight
            } else if (mVideoWidth * height > width * mVideoHeight) {
                height = width * mVideoHeight / mVideoWidth
            }
            VideoView.SCREEN_SCALE_ORIGINAL -> {
                width = mVideoWidth
                height = mVideoHeight
            }
            VideoView.SCREEN_SCALE_16_9 -> if (height > width / 16 * 9) {
                height = width / 16 * 9
            } else {
                width = height / 9 * 16
            }
            VideoView.SCREEN_SCALE_4_3 -> if (height > width / 4 * 3) {
                height = width / 4 * 3
            } else {
                width = height / 3 * 4
            }
            VideoView.SCREEN_SCALE_MATCH_PARENT -> {
                width = widthMeasureSpec
                height = heightMeasureSpec
            }
            VideoView.SCREEN_SCALE_CENTER_CROP -> if (mVideoWidth * height > width * mVideoHeight) {
                width = height * mVideoWidth / mVideoHeight
            } else {
                height = width * mVideoHeight / mVideoWidth
            }
            else -> if (mVideoWidth * height < width * mVideoHeight) {
                width = height * mVideoWidth / mVideoHeight
            } else if (mVideoWidth * height > width * mVideoHeight) {
                height = width * mVideoHeight / mVideoWidth
            }
        }
        return intArrayOf(width, height)
    }
}