/**
 * Copyright 2015 Anthony Restaino
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.hyphenate.easeui.common.permission

import android.Manifest
import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.annotation.StringDef
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.utils.StatusBarCompat.getStatusBarHeight
import com.hyphenate.easeui.menu.select.SelectUtils
import java.lang.ref.WeakReference

/**
 * A class to help you manage your permissions simply.
 */
class PermissionsManager private constructor() {
    private val mPendingRequests: MutableSet<String?> = HashSet(1)
    private val mPermissions: MutableSet<String?> = HashSet(1)
    private val mPendingActions: MutableList<WeakReference<PermissionsResultAction?>> = ArrayList(1)

    init {
        initializePermissionsMap()
    }

    /**
     * This method uses reflection to read all the permissions in the Manifest class.
     * This is necessary because some permissions do not exist on older versions of Android,
     * since they do not exist, they will be denied when you check whether you have permission
     * which is problematic since a new permission is often added where there was no previous
     * permission required. We initialize a Set of available permissions and check the set
     * when checking if we have permission since we want to know when we are denied a permission
     * because it doesn't exist yet.
     */
    @Synchronized
    private fun initializePermissionsMap() {
        val fields = permission::class.java.fields
        for (field in fields) {
            var name: String? = null
            try {
                name = field[""] as String
            } catch (e: IllegalAccessException) {
                Log.e(TAG, "Could not access field", e)
            }
            mPermissions.add(name)
        }
    }

    /**
     * This method retrieves all the permissions declared in the application's manifest.
     * It returns a non null array of permisions that can be declared.
     *
     * @param activity the Activity necessary to check what permissions we have.
     * @return a non null array of permissions that are declared in the application manifest.
     */
    @Synchronized
    private fun getManifestPermissions(activity: Activity): Array<String> {
        var packageInfo: PackageInfo? = null
        val list: MutableList<String> = ArrayList(1)
        try {
            Log.d(TAG, activity.packageName)
            packageInfo = activity.packageManager.getPackageInfo(
                activity.packageName,
                PackageManager.GET_PERMISSIONS
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "A problem occurred when retrieving permissions", e)
        }
        if (packageInfo != null) {
            val permissions = packageInfo.requestedPermissions
            if (permissions != null) {
                for (perm in permissions) {
                    Log.d(TAG, "Manifest contained permission: $perm")
                    list.add(perm)
                }
            }
        }
        return list.toTypedArray()
    }

    /**
     * This method adds the [PermissionsResultAction] to the current list
     * of pending actions that will be completed when the permissions are
     * received. The list of permissions passed to this method are registered
     * in the PermissionsResultAction object so that it will be notified of changes
     * made to these permissions.
     *
     * @param permissions the required permissions for the action to be executed.
     * @param action      the action to add to the current list of pending actions.
     */
    @Synchronized
    private fun addPendingAction(
        permissions: Array<String>,
        action: PermissionsResultAction?
    ) {
        if (action == null) {
            return
        }
        action.registerPermissions(permissions)
        mPendingActions.add(WeakReference(action))
    }

    /**
     * This method removes a pending action from the list of pending actions.
     * It is used for cases where the permission has already been granted, so
     * you immediately wish to remove the pending action from the queue and
     * execute the action.
     *
     * @param action the action to remove
     */
    @Synchronized
    private fun removePendingAction(action: PermissionsResultAction?) {
        val iterator = mPendingActions.iterator()
        while (iterator.hasNext()) {
            val weakRef = iterator.next()
            if (weakRef.get() === action || weakRef.get() == null) {
                iterator.remove()
            }
        }
    }

    /**
     * This static method can be used to check whether or not you have a specific permission.
     * It is basically a less verbose method of using [ActivityCompat.checkSelfPermission]
     * and will simply return a boolean whether or not you have the permission. If you pass
     * in a null Context object, it will return false as otherwise it cannot check the permission.
     * However, the Activity parameter is nullable so that you can pass in a reference that you
     * are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context    the Context necessary to check the permission
     * @param permission the permission to check
     * @return true if you have been granted the permission, false otherwise
     */
    @Suppress("unused")
    @Synchronized
    fun hasPermission(context: Context?, permission: String): Boolean {
        return context != null && (ActivityCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED || !mPermissions.contains(permission))
    }

    /**
     * This static method can be used to check whether or not you have several specific permissions.
     * It is simpler than checking using [ActivityCompat.checkSelfPermission]
     * for each permission and will simply return a boolean whether or not you have all the permissions.
     * If you pass in a null Context object, it will return false as otherwise it cannot check the
     * permission. However, the Activity parameter is nullable so that you can pass in a reference
     * that you are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context     the Context necessary to check the permission
     * @param permissions the permissions to check
     * @return true if you have been granted all the permissions, false otherwise
     */
    @Suppress("unused")
    @Synchronized
    fun hasAllPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context == null) {
            return false
        }
        var hasAllPermissions = true
        for (perm in permissions) {
            hasAllPermissions = hasAllPermissions and hasPermission(context, perm)
        }
        return hasAllPermissions
    }

    /**
     * This method will request all the permissions declared in your application manifest
     * for the specified [PermissionsResultAction]. The purpose of this method is to enable
     * all permissions to be requested at one shot. The PermissionsResultAction is used to notify
     * you of the user allowing or denying each permission. The Activity and PermissionsResultAction
     * parameters are both annotated Nullable, but this method will not work if the Activity
     * is null. It is only annotated Nullable as a courtesy to prevent crashes in the case
     * that you call this from a Fragment where [Fragment.getActivity] could yield
     * null. Additionally, you will not receive any notification of permissions being granted
     * if you provide a null PermissionsResultAction.
     *
     * @param activity the Activity necessary to request and check permissions.
     * @param action   the PermissionsResultAction used to notify you of permissions being accepted.
     */
    @Suppress("unused")
    @Synchronized
    fun requestAllManifestPermissionsIfNecessary(
        activity: Activity?,
        action: PermissionsResultAction?
    ) {
        if (activity == null) {
            return
        }
        val perms = getManifestPermissions(activity)
        requestPermissionsIfNecessaryForResult(activity = activity, perms, action)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestCanDrawOverlays(activity: Activity?) {
        if (!Settings.canDrawOverlays(activity)) {
            //Request permission if not authorized
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:" + activity!!.packageName)
            activity.startActivityForResult(intent, 0)
        }
    }

    /**
     * This method should be used to execute a [PermissionsResultAction] for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Activity variable is nullable, but if it is null, the method will fail to execute.
     * This is only nullable as a courtesy for Fragments where getActivity() may yeild null
     * if the Fragment is not currently added to its parent Activity.
     *
     * @param activity    the activity necessary to request the permissions.
     * @param permissions the list of permissions to request for the [PermissionsResultAction].
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     */
    @Suppress("unused")
    @Synchronized
    fun requestPermissionsIfNecessaryForResult(
        activity: Activity?,
        permissions: Array<String>,
        action: PermissionsResultAction?
    ) {
        if (activity == null) {
            return
        }
        addPendingAction(permissions, action)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            doPermissionWorkBeforeAndroidM(activity, permissions, action)
        } else {
            val permList = getPermissionsListToRequest(activity, permissions, action)
            if (permList.isEmpty()) {
                //if there is no permission to request, there is no reason to keep the action int the list
                removePendingAction(action)
            } else {
                val permsToRequest = permList.toTypedArray()
                mPendingRequests.addAll(permList)
                ActivityCompat.requestPermissions(activity, permsToRequest, 1)
            }
        }
    }

    /**
     * This method should be used to execute a [PermissionsResultAction] for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Fragment variable is used, but if [Fragment.getActivity] returns null, this method
     * will fail to work as the activity reference is necessary to check for permissions.
     *
     * @param fragment    the fragment necessary to request the permissions.
     * @param permissions the list of permissions to request for the [PermissionsResultAction].
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     */
    @Suppress("unused")
    @Synchronized
    fun requestPermissionsIfNecessaryForResult(
        fragment: Fragment,
        permissions: Array<String>,
        action: PermissionsResultAction?
    ) {
        val activity = fragment.activity ?: return
        addPendingAction(permissions, action)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            doPermissionWorkBeforeAndroidM(activity, permissions, action)
        } else {
            val permList = getPermissionsListToRequest(activity, permissions, action)
            if (permList.isEmpty()) {
                //if there is no permission to request, there is no reason to keep the action int the list
                removePendingAction(action)
            } else {
                val permsToRequest = permList.toTypedArray()
                mPendingRequests.addAll(permList)
                fragment.requestPermissions(permsToRequest, 1)
            }
        }
    }

    /**
     * This method notifies the PermissionsManager that the permissions have change. If you are making
     * the permissions requests using an Activity, then this method should be called from the
     * Activity callback onRequestPermissionsResult() with the variables passed to that method. If
     * you are passing a Fragment to make the permissions request, then you should call this in
     * the [Fragment.onRequestPermissionsResult] method.
     * It will notify all the pending PermissionsResultAction objects currently
     * in the queue, and will remove the permissions request from the list of pending requests.
     *
     * @param permissions the permissions that have changed.
     * @param results     the values for each permission.
     */
    @Suppress("unused")
    @Synchronized
    fun notifyPermissionsChange(permissions: Array<String?>, results: IntArray) {
        var size = permissions.size
        if (results.size < size) {
            size = results.size
        }
        val iterator = mPendingActions.iterator()
        while (iterator.hasNext()) {
            val action = iterator.next().get()
            for (n in 0 until size) {
                if (action == null || action.onResult(permissions[n]!!, results[n])) {
                    iterator.remove()
                    break
                }
            }
        }
        for (n in 0 until size) {
            mPendingRequests.remove(permissions[n])
        }
    }

    /**
     * When request permissions on devices before Android M (Android 6.0, API Level 23)
     * Do the granted or denied work directly according to the permission status
     *
     * @param activity    the activity to check permissions
     * @param permissions the permissions names
     * @param action      the callback work object, containing what we what to do after
     * permission check
     */
    private fun doPermissionWorkBeforeAndroidM(
        activity: Activity,
        permissions: Array<String>,
        action: PermissionsResultAction?
    ) {
        for (perm in permissions) {
            if (action != null) {
                if (!mPermissions.contains(perm)) {
                    action.onResult(perm!!, Permissions.NOT_FOUND)
                } else if (ActivityCompat.checkSelfPermission(activity, perm!!)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    action.onResult(perm, Permissions.DENIED)
                } else {
                    action.onResult(perm, Permissions.GRANTED)
                }
            }
        }
    }

    /**
     * Filter the permissions list:
     * If a permission is not granted, add it to the result list
     * if a permission is granted, do the granted work, do not add it to the result list
     *
     * @param activity    the activity to check permissions
     * @param permissions all the permissions names
     * @param action      the callback work object, containing what we what to do after
     * permission check
     * @return a list of permissions names that are not granted yet
     */
    private fun getPermissionsListToRequest(
        activity: Activity,
        permissions: Array<String>,
        action: PermissionsResultAction?
    ): List<String?> {
        val permList: MutableList<String?> = ArrayList(permissions.size)
        for (perm in permissions) {
            if (!mPermissions.contains(perm)) {
                action?.onResult(perm!!, Permissions.NOT_FOUND)
            } else if (ActivityCompat.checkSelfPermission(
                    activity,
                    perm!!
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (!mPendingRequests.contains(perm)) {
                    permList.add(perm)
                }
            } else {
                action?.onResult(perm, Permissions.GRANTED)
            }
        }
        return permList
    }

    @StringDef(Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Permission

    fun showPermissionDescView(activity: Activity, @Permission permission: String) :View ?{
        if (activity==null||activity.isFinishing){
            return null
        }

        if (ActivityCompat.checkSelfPermission(activity, permission)== PackageManager.PERMISSION_GRANTED){
           return null
        }
        var id=0
        var title=0
        var content=0
        when(permission) {
            Manifest.permission.READ_MEDIA_IMAGES->{
                id = R.drawable.uikit_permission_media
                title = R.string.uikit_permission_media_title
                content = R.string.uikit_permission_media_content
            }
            Manifest.permission.READ_MEDIA_VIDEO   -> {
                id = R.drawable.uikit_permission_media
                title = R.string.uikit_permission_media_title
                content = R.string.uikit_permission_media_content
            }

            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                id = R.drawable.uikit_permission_file
                title = R.string.uikit_permission_sdcard_title
                content = R.string.uikit_permission_sdcard_content
            }

            Manifest.permission.CAMERA -> {
                id = R.drawable.uikit_permission_camera
                title = R.string.uikit_permission_camera_title
                content = R.string.uikit_permission_camera_content
            }

            Manifest.permission.RECORD_AUDIO -> {
                id = R.drawable.uikit_permission_mic
                title = R.string.uikit_permission_mic_title
                content = R.string.uikit_permission_mic_content
            }
        }
        var decorView = activity.window.decorView as FrameLayout
        var permissionDescView = LayoutInflater.from(activity)
            .inflate(R.layout.uikit_permission_desc_view, decorView, false)
        decorView.post {
            permissionDescView.findViewById<ImageView>(R.id.iv_icon).setImageResource(id)
            permissionDescView.findViewById<TextView>(R.id.tv_title).text=activity.getText(title)
            permissionDescView.findViewById<TextView>(R.id.tv_content).text=activity.getText(content)

            decorView.addView(permissionDescView)
            val layoutParams = permissionDescView.getLayoutParams() as FrameLayout.LayoutParams
            layoutParams.setMargins(
                layoutParams.leftMargin,
                getStatusBarHeight(activity.applicationContext)+SelectUtils.dp2px(10f),
                layoutParams.rightMargin,
                layoutParams.topMargin
            )
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        }
        return permissionDescView
    }

    fun hidePermissionDescView(permissionView: View?){
        permissionView?.let {
            it.post {
                it.parent?.let { parent ->
                    var parrent_viewgroup=parent as ViewGroup
                    parrent_viewgroup.removeView(permissionView)
                }
            }
        }
    }

    companion object {
        private val TAG = PermissionsManager::class.java.simpleName
        private var mInstance: PermissionsManager? = null

        fun getInstance(): PermissionsManager {
            if (mInstance == null) {
                mInstance = PermissionsManager()
            }
            return mInstance!!
        }
    }
}