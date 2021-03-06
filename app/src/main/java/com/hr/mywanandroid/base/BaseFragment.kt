package com.hr.mywanandroid.base

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.classic.common.MultipleStatusView
import com.hr.mywanandroid.R
import java.util.ArrayList
import java.util.HashMap

abstract class BaseFragment:Fragment() {

    protected abstract var layoutId:Int

    protected abstract fun initData()

    //多视图
    protected var multipleStatusView: MultipleStatusView? = null

    abstract fun subscribeUi()

    private lateinit var permissionListener: Listeners.PermissionListener
    private lateinit var deniedPermissions: HashMap<String, Array<String>>
    private var showPermissionDialogOnDenied: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        multipleStatusView?.setOnClickListener {
            when (multipleStatusView?.viewStatus) {
                MultipleStatusView.STATUS_ERROR, MultipleStatusView.STATUS_EMPTY -> onRetry()
            }
        }
    }

    open fun onRetry() {

    }

    //处理数据
    open fun<T> handleData(liveData: LiveData<RequestState<T>>, action:(T) -> Unit) =
        liveData.observe(this, Observer {
            if(it.isLoading()) {
//                RequestState状态为loading, 显示loading视图
                multipleStatusView?.showLoading()
            }else if(it.isSuccess()) {
                if(it.data != null) {
                    //显示内容视图
                    multipleStatusView?.showContent()
                    action(it.data)
                }else {
                    //显示空视图
                    multipleStatusView?.showEmpty()
                }
            }else if(it.isError()) {
                multipleStatusView?.showError()
            }
        })


    protected fun checkPermissions(permissions: Array<String>, permissionsCN: Array<String>, reasons: Array<String>, listener: Listeners.PermissionListener) {
        permissionListener = listener
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            permissionListener.onGranted()
        } else {
            deniedPermissions = HashMap()
            permissions.forEachIndexed { index, s ->
                if (ContextCompat.checkSelfPermission(activity as Context, s) != PermissionChecker.PERMISSION_GRANTED) {
                    deniedPermissions[s] = arrayOf(permissionsCN[index], reasons[index])
                }
            }
            if (deniedPermissions.isEmpty()) {
                permissionListener.onGranted()
            } else {
                val permissionsSb = StringBuilder()
                val reasonsSb = StringBuilder()
                deniedPermissions.forEach {
                    if (shouldShowRequestPermissionRationale(it.key)) {
                        //用户拒绝过权限该方法就会返回 true，选择不再提示后返回 false
                        permissionsSb.append(it.value[0]).append(",")
                        reasonsSb.append(it.value[1]).append("\n")
                    }
                }
                if (permissionsSb.isNotBlank() && reasonsSb.isNotBlank()) {
                    showPermissionDialogOnDenied = false
                    permissionsSb.deleteCharAt(permissionsSb.length - 1)
                    reasonsSb.deleteCharAt(reasonsSb.length - 1)
                    showPermissionDeniedDialog(permissionsSb.toString(), reasonsSb.toString()) {
                        requestPermissions(deniedPermissions.keys.toTypedArray(), Constants.PERMISSION_CODE)
                        permissionListener.onShowReason()
                    }
                } else {
                    requestPermissions(deniedPermissions.keys.toTypedArray(), Constants.PERMISSION_CODE)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        when (requestCode) {
            Constants.PERMISSION_CODE -> if (grantResults.isNotEmpty()) {
                val deniedAgainPermissions = ArrayList<String>()
                for (i in grantResults.indices) {
                    val permission = permissions[i]
                    val grantResult = grantResults[i]
                    if (grantResult != PermissionChecker.PERMISSION_GRANTED) {
                        deniedAgainPermissions.add(permission)
                    }
                }
                if (deniedAgainPermissions.isEmpty()) {
                    permissionListener.onGranted()
                } else {
                    deniedAgainPermissions.forEach { now ->
                        deniedPermissions.forEach { old ->
                            if (now == old.key) {
                                if (showPermissionDialogOnDenied) {
                                    showPermissionDeniedDialog(old.value[0], old.value[1]) { permissionListener.onDenied(deniedAgainPermissions) }
                                } else {
                                    showPermissionDialogOnDenied = true
                                    permissionListener.onDenied(deniedAgainPermissions)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showPermissionDeniedDialog(p: String, reason: String, action: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("缺少【$p】权限")
            .setMessage(reason)
            .setCancelable(false)
            .setPositiveButton(
                R.string.i_know
            ) { _, _ -> action() }
            .show()
    }

}