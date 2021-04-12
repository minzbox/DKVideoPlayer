package com.dueeeke.videoplayer.controller

import android.content.Context
import android.view.OrientationEventListener

/**
 * Device direction monitoring
 */
class OrientationHelper(context: Context?) : OrientationEventListener(context) {
    private var mLastTime: Long = 0
    private var mOnOrientationChangeListener: OnOrientationChangeListener? = null
    override fun onOrientationChanged(orientation: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - mLastTime < 300) return  //Check every 300 milliseconds
        if (mOnOrientationChangeListener != null) {
            mOnOrientationChangeListener!!.onOrientationChanged(orientation)
        }
        mLastTime = currentTime
    }

    interface OnOrientationChangeListener {
        fun onOrientationChanged(orientation: Int)
    }

    fun setOnOrientationChangeListener(onOrientationChangeListener: OnOrientationChangeListener?) {
        mOnOrientationChangeListener = onOrientationChangeListener
    }
}