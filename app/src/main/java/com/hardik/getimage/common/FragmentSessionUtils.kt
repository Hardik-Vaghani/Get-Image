package com.hardik.getimage.common

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.hardik.getimage.MainActivity
import com.hardik.getimage.R

class FragmentSessionUtils private constructor() {
    companion object {
        private val TAG = "BASE_TAG" + FragmentSessionUtils::class.simpleName

        @Volatile
        private var instance: FragmentSessionUtils? = null

        // Method to get the singleton instance
        fun getInstance(): FragmentSessionUtils {
            return instance ?: synchronized(this) {
                instance ?: FragmentSessionUtils().also { instance = it }
            }
        }
    }

    init {
        Log.i(TAG, "FragmentSessionUtils")
    }

    fun handleBackPressed(activity: Activity) {
        Log.i(TAG, "handleBackPressed: ")
        if (activity is MainActivity) {
            val fragmentManager = activity.supportFragmentManager
            val stackCount = fragmentManager.backStackEntryCount

            Log.i(TAG, "handleBackPressed: stackCount: $stackCount")
            if (stackCount >= 1) {
                Log.i(TAG, "handleBackPressed: go back")
                fragmentManager.popBackStack()
            } else {
                Log.i(TAG, "handleBackPressed: finish activity")
                fragmentManager.popBackStackImmediate()
                activity.finish()
            }
        }
    }

    fun switchFragment(
        fragmentManager: FragmentManager,
        fragment: Fragment,
        addToBackStack: Boolean = false,
    ) {
        // Obtain FragmentManager
        if (fragmentManager.isDestroyed) {
            return
        }
        // Get tag name from fragment
        val tag = fragment::class.simpleName
        Log.i(TAG, "switchFragment: $tag")

        // Save the current fragment state
        val navHostContainer = R.id.fragment_container
        val currentFragment = fragmentManager.findFragmentById(navHostContainer)
        var fragmentState: Bundle? = null
        currentFragment?.let {
            fragmentState = Bundle()
            it.onSaveInstanceState(fragmentState!!)
        }

        // Begin FragmentTransaction
        val fragmentTransaction = fragmentManager.beginTransaction()
        val existingFragment = fragmentManager.findFragmentByTag(tag)

//        val settingFragment = fragmentManager.findFragmentByTag(SettingsFragment::class.java.simpleName)
//        if (fragmentManager.findFragmentById(R.id.nav_host_fragment) == settingFragment){
//            fragmentManager.popBackStack(SettingsFragment::class.java.simpleName,FragmentManager.POP_BACK_STACK_INCLUSIVE)
//        }

        fragmentTransaction.replace(navHostContainer, fragment, tag)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag)
        }


        // Commit the transaction
        fragmentTransaction.commitAllowingStateLoss()
        Log.i(TAG, "switchFragment: countOfStackAdd: ${fragmentManager.backStackEntryCount}")

        // Restore the state of the replaced fragment
        var callbacks: FragmentManager.FragmentLifecycleCallbacks? = null
        fragmentState?.let { finalFragmentState ->
            val finalCallbacks = callbacks
            callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    savedInstanceState: Bundle?
                ) {
                    super.onFragmentCreated(fm, f, savedInstanceState)
                    if (f === fragment && f.view != null) {
                        f.view?.post {
                            f.onViewStateRestored(finalFragmentState)
                            finalCallbacks?.let {
                                fm.unregisterFragmentLifecycleCallbacks(it)
                            }
                        }
                    }
                }
            }
            fragmentManager.registerFragmentLifecycleCallbacks(callbacks!!, false)
        }
    }
}
