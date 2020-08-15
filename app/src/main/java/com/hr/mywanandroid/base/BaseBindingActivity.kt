package com.hr.mywanandroid.base

import androidx.databinding.ViewDataBinding

abstract class BaseBindingActivity<T: ViewDataBinding> :BaseActivity() {
    override var layoutId = 0

    abstract  var binding: T
}