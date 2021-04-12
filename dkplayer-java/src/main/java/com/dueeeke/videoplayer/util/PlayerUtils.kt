package com.dueeeke.videoplayer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Insets
import android.graphics.Point
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Size
import android.util.TypedValue
import android.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Player related tools
 * Created by NghiaNV on 2017/4/10.
 */
@Suppress("DEPRECATION")
object PlayerUtils {
    /**
     * Get the height of the status bar
     */
    @JvmStatic
    fun getStatusBarHeight(context: Context): Double {
        var statusBarHeight = 0
        //Get the ID of the status_bar_height resource
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            //Get the size value of the response according to the resource ID
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight.toDouble()
    }

    /**
     * Get the height of the status bar in portrait mode
     */
    @JvmStatic
    fun getStatusBarHeightPortrait(context: Context): Double {
        var statusBarHeight = 0
        //Get the ID of the status_bar_height_portrait resource
        val resourceId = context.resources.getIdentifier("status_bar_height_portrait", "dimen", "android")
        if (resourceId > 0) {
            //Get the size value of the response according to the resource ID
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight.toDouble()
    }

    /**
     * Get the height of NavigationBar
     */
    private fun getNavigationBarHeight(context: Context): Int {
        if (!hasNavigationBar(context)) {
            return 0
        }
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height",
                "dimen", "android")
        //Get the height of NavigationBar
        return resources.getDimensionPixelSize(resourceId)
    }

    /**
     * Whether there is a NavigationBar
     */
    @Suppress("DEPRECATION")
    private fun hasNavigationBar(context: Context): Boolean {
        // Gets all excluding insets
        val metrics = scanForActivity(context)?.windowManager?.currentWindowMetrics ?: return false
        val windowInsets = metrics.windowInsets
        val insets: Insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()
                or WindowInsets.Type.displayCutout())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val insetsWidth: Int = insets.right + insets.left
            val insetsHeight: Int = insets.top + insets.bottom
            // Legacy size that Display#getSize reports
            val bounds: Rect = metrics.bounds
            val legacySize = Size(bounds.width() - insetsWidth,
                    bounds.height() - insetsHeight)
            return metrics.bounds.width() != legacySize.width || metrics.bounds.height() != legacySize.height
        } else return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val display = getWindowManager(context).defaultDisplay
            val size = Point()
            val realSize = Point()
            display.getSize(size)
            display.getRealSize(realSize)
            realSize.x != size.x || realSize.y != size.y
        } else {
            val menu = ViewConfiguration.get(context).hasPermanentMenuKey()
            val back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
            !(menu || back)
        }
    }

    /**
     * Hide navigation bar
     */
    fun hideSystemUI(activity: Activity?) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                activity?.window?.setDecorFitsSystemWindows(false)
                activity?.window?.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() and WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            }
            else -> {
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            }
        }
    }

    fun showSystemUI(activity: Activity?) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                activity?.window?.setDecorFitsSystemWindows(true)
                activity?.window?.insetsController?.show(WindowInsets.Type.statusBars() and WindowInsets.Type.navigationBars())
            }
            else -> {
                activity?.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
    }

    /**
     * Get screen width
     */
    @JvmStatic
    fun getScreenWidth(context: Context, isIncludeNav: Boolean): Int {
        return if (isIncludeNav) {
            context.resources.displayMetrics.widthPixels + getNavigationBarHeight(context)
        } else {
            context.resources.displayMetrics.widthPixels
        }
    }

    /**
     * Get screen height
     */
    private fun getScreenHeight(context: Context, isIncludeNav: Boolean): Int {
        return if (isIncludeNav) {
            context.resources.displayMetrics.heightPixels + getNavigationBarHeight(context)
        } else {
            context.resources.displayMetrics.heightPixels
        }
    }

    /**
     * Get Activity
     */
    @JvmStatic
    fun scanForActivity(context: Context?): Activity? {
        if (context == null) return null
        if (context is Activity) {
            return context
        } else if (context is ContextWrapper) {
            return scanForActivity(context.baseContext)
        }
        return null
    }

    /**
     * Convert dp to pixel
     */
    @JvmStatic
    fun dp2px(context: Context, dpValue: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.resources.displayMetrics).toInt()
    }

    /**
     * Convert sp to pixel
     */
    @JvmStatic
    fun sp2px(context: Context, dpValue: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dpValue, context.resources.displayMetrics).toInt()
    }

    /**
     * If WindowManager has not been created yet, create a new WindowManager and return. Otherwise, it returns the currently created WindowManager.
     */
    @JvmStatic
    fun getWindowManager(context: Context): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * Edge detection
     */
    @JvmStatic
    fun isEdge(context: Context, e: MotionEvent): Boolean {
        val edgeSize = dp2px(context, 40f)
        return e.rawX < edgeSize || e.rawX > getScreenWidth(context, true) - edgeSize || e.rawY < edgeSize || e.rawY > getScreenHeight(context, true) - edgeSize
    }

    private const val NO_NETWORK = 0
    private const val NETWORK_CLOSED = 1
    private const val NETWORK_ETHERNET = 2
    private const val NETWORK_WIFI = 3
    const val NETWORK_MOBILE = 4
    private const val NETWORK_UNKNOWN = -1

    /**
     * Determine the current network type
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun getNetworkType(context: Context): Int {
        val connectMgr = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectMgr.activeNetwork
            val actNw = connectMgr.getNetworkCapabilities(nw)
            return if (actNw != null) {
                when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NETWORK_WIFI
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NETWORK_MOBILE
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NETWORK_ETHERNET
                    else -> NETWORK_UNKNOWN
                }
            } else {
                NO_NETWORK
            }
        } else {
            val networkInfo = connectMgr.activeNetworkInfo ?: return NO_NETWORK
            if (!networkInfo.isConnected) {
                return NETWORK_CLOSED
            }
            when (networkInfo.type) {
                ConnectivityManager.TYPE_ETHERNET -> {
                    return NETWORK_ETHERNET
                }
                ConnectivityManager.TYPE_WIFI -> {
                    return NETWORK_WIFI
                }
                ConnectivityManager.TYPE_MOBILE -> {
                    when (networkInfo.subtype) {
                        TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_NR -> return NETWORK_MOBILE
                    }
                }
            }
            return NETWORK_UNKNOWN
        }
    }

    /**
     * Obtain Application through reflection
     *
     */
    @get:Deprecated("Not in use, Google may block and change the interface later")
    @get:SuppressLint("PrivateApi")
    val application: Application?
        get() {
            try {
                return Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null) as Application
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    /**
     * Get current system time
     */
    @JvmStatic
    val currentSystemTime: String
        get() {
            val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = Date()
            return simpleDateFormat.format(date)
        }

    /**
     * Format time
     */
    @JvmStatic
    fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Get a snapshot of the collection
     */
    fun <T> getSnapshot(other: Collection<T>): List<T> {
        val result: MutableList<T> = ArrayList(other.size)
        for (item in other) {
            if (item != null) {
                result.add(item)
            }
        }
        return result
    }
}