package com.dueeeke.videoplayer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.dueeeke.videoplayer.util.PlayerUtils.scanForActivity

/**
 * Notch tool
 */
object CutoutUtil {
    /**
     * Whether it is a notch model that allows full-screen interface to display content to the notch area (corresponding to the configuration in AndroidManifest)
     */
    @JvmStatic
    fun allowDisplayToCutout(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // The 9.0 system full-screen interface will retain the black border by default, and it is not allowed to display the content to the bangs area
            val window = activity.window
            val windowInsets = window.decorView.rootWindowInsets ?: return false
            val displayCutout = windowInsets.displayCutout ?: return false
            val boundingRect = displayCutout.boundingRects
            boundingRect.size > 0
        } else {
            (hasCutoutHuawei(activity)
                    || hasCutoutOPPO(activity)
                    || hasCutoutVIVO(activity)
                    || hasCutoutXIAOMI(activity))
        }
    }

    /**
     * Whether it is a Huawei notch model
     */
    private fun hasCutoutHuawei(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)) {
            false
        } else try {
            val cl = activity.classLoader
            val hwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil")
            if (hwNotchSizeUtil != null) {
                val get = hwNotchSizeUtil.getMethod("hasNotchInScreen")
                return get.invoke(hwNotchSizeUtil) as Boolean
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Is it an oppo notch model
     */
    private fun hasCutoutOPPO(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("oppo", ignoreCase = true)) {
            false
        } else activity.packageManager.hasSystemFeature("com.oppo.feature.screen.heteromorphism")
    }

    /**
     * Is it a vivo Liu Haiping model
     */
    @SuppressLint("PrivateApi")
    private fun hasCutoutVIVO(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("vivo", ignoreCase = true)) {
            false
        } else try {
            val cl = activity.classLoader
            val ftFeatureUtil = cl.loadClass("android.util.FtFeature")
            if (ftFeatureUtil != null) {
                val get = ftFeatureUtil.getMethod("isFeatureSupport", Int::class.javaPrimitiveType)
                return get.invoke(ftFeatureUtil, 0x00000020) as Boolean
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Whether it is a Xiaomi Liu Haiping model
     */
    @SuppressLint("PrivateApi")
    private fun hasCutoutXIAOMI(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)) {
            false
        } else try {
            val cl = activity.classLoader
            val systemProperties = cl.loadClass("android.os.SystemProperties")
            val paramTypes = arrayOfNulls<Class<*>?>(2)
            paramTypes[0] = String::class.java
            paramTypes[1] = Int::class.javaPrimitiveType
            val getInt = systemProperties.getMethod("getInt", *paramTypes)
            //参数
            val params = arrayOfNulls<Any>(2)
            params[0] = "ro.miui.notch"
            params[1] = 0
            val hasCutout = getInt.invoke(systemProperties, *params) as Int
            hasCutout == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Adapt to Liu Haiping, for systems above Android P
     */
    @JvmStatic
    fun adaptCutoutAboveAndroidP(context: Context?, isAdapt: Boolean) {
        val activity = scanForActivity(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = activity.window.attributes
            if (isAdapt) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            activity.window.attributes = lp
        }
    }
}