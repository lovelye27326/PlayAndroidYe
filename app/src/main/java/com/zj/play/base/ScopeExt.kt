package com.zj.play.base

import com.zj.core.util.LogUtil
import com.zj.model.model.BaseModel
import com.zj.model.model.Login
import com.zj.model.model.isSuccess
import com.zj.network.action.RequestAction
import com.zj.network.exception.HandleException
import com.zj.network.service.LoginService
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * desc: 协程http 请求扩展
 */

fun <T> CoroutineScope.netRequest(block: RequestAction<T>.() -> Unit) {
    val action = RequestAction<T>().apply(block)
    this.launch {
        try {
            action.start?.invoke()
            val result = action.request?.invoke()
            if (result.isSuccess()) {
                action.success?.invoke(result!!.data)
            } else {
                if (result != null) {
                    action.error?.invoke(result.errorMsg + "|" + result.errorCode)
                } else {
                    action.error?.invoke("|")
                }
            }
        } catch (ex: Exception) {
            // 可以做一些定制化的返回错误提示
            action.error?.invoke(HandleException.handleResponseError(ex))
        } finally {
            action.finish?.invoke()
        }
    }
}


/**
 * 当前协程未指定调度线程，恢复挂起的数据仍在当前线程中
 */
fun <T> CoroutineScope.http(
    request: (suspend () -> BaseModel<T>),
    response: (T?) -> Unit,
    error: (String) -> Unit = {},
    showToast: Boolean = true
): Job {
    return this.launch {
        try {
            val result = request()
            if (result.errorCode == 0) {
                response(result.data)
            } else {
                showToast(showToast, result.errorMsg)
                error(result.errorMsg)
            }
        } catch (e: Exception) {
            showToast(showToast, e.message)
            error(e.message ?: "异常")
        }

    }
}


/**
 * 当前协程指定调度线程，恢复挂起的数据仍在当前指定的线程中
 */
fun <T> CoroutineScope.http2(
    request: (suspend () -> BaseModel<T>),
    response: (T?) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    error: (String) -> Unit = {},
    showToast: Boolean = true
): Job {
    return this.launch(dispatcher) {
        try {
            val result = request()
            if (result.errorCode == 0) {
                response(result.data)
            } else {
                showToast(showToast, result.errorMsg)
                error(result.errorMsg)
            }
        } catch (e: Exception) {
            showToast(showToast, e.message)
            error(e.message ?: "异常")
        }

    }
}

private const val TAG = "ScopeExt"

private fun showToast(isShow: Boolean, msg: String?) {
    LogUtil.e(TAG, "showToast: isShow:$isShow   msg:$msg")
}


@ActivityRetainedScoped
class UserUseCase @Inject constructor(
    val getLoginProjects: GetLoginProjects,
    val getRegisterProjects: GetRegisterProjects
)

@ActivityRetainedScoped
class GetLoginProjects @Inject constructor(private val service: LoginService) {
    suspend operator fun invoke(username: String, password: String): BaseModel<Login> {
        return service.getLogin(username, password)
    }
}

@ActivityRetainedScoped
class GetRegisterProjects @Inject constructor(private val service: LoginService) {
    suspend operator fun invoke(
        username: String,
        password: String,
        surePassword: String
    ): BaseModel<Login> {
        return service.getRegister(username, password, surePassword)
    }
}