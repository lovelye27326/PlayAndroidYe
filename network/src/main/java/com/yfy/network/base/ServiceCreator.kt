package com.yfy.network.base

import cn.coderpig.cp_network_capture.interceptor.CaptureInterceptor
import com.yfy.core.util.DataStoreUtils
import com.yfy.core.util.LogUtil
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * object单例 ServiceCreator
 *
 */
object ServiceCreator {

    const val BASE_URL = "https://www.wanandroid.com/"
    private const val SAVE_USER_LOGIN_KEY = "user/login"
    private const val SAVE_USER_REGISTER_KEY = "user/register"
    private const val SET_COOKIE_KEY = "set-cookie"
    private const val COOKIE_NAME = "Cookie"
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 10L


    val okHttpClient: OkHttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClient().newBuilder().apply {
            connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            if (LogUtil.DEBUG_MODE) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                addLogPopWin(this)
            }
            // get response cookie
            addInterceptor {
                val request = it.request()
                val response = it.proceed(request)
                val requestUrl = request.url().toString()
                val domain = request.url().host()
                // set-cookie maybe has multi, login to save cookie
                if ((requestUrl.contains(SAVE_USER_LOGIN_KEY) || requestUrl.contains(
                        SAVE_USER_REGISTER_KEY
                    ))
                    && response.headers(SET_COOKIE_KEY).isNotEmpty()
                ) {
                    val cookies = response.headers(SET_COOKIE_KEY)
                    val cookie = encodeCookie(cookies)
                    saveCookie(requestUrl, domain, cookie)
                }
                response
            }
            addInterceptor {
                val request = it.request()
                val builder = request.newBuilder()
                val domain = request.url().host()
                // get domain cookie
                if (domain.isNotEmpty()) {
                    val spDomain: String = DataStoreUtils.readStringData(domain, "")
                    val cookie: String = spDomain.ifEmpty { "" }
                    if (cookie.isNotEmpty()) {
                        builder.addHeader(COOKIE_NAME, cookie)
                    }
                }
                it.proceed(builder.build())
            }
        }.build()

    }

    private fun create(): Retrofit {

        return RetrofitBuild(
            url = BASE_URL,
            client = okHttpClient,
            gsonFactory = GsonConverterFactory.create()
        ).retrofit
    }

    /**
     * get ServiceApi
     */
    fun <T> create(service: Class<T>): T = create().create(service)

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    private fun saveCookie(url: String?, domain: String?, cookies: String) {
        url ?: return
        DataStoreUtils.putSyncData(url, cookies)
        domain ?: return
        DataStoreUtils.putSyncData(domain, cookies)
    }


    /**
     * 添加日志助手
     */
    private fun addLogPopWin(builder: OkHttpClient.Builder) {
        val interceptor = CaptureInterceptor()
        builder.addInterceptor(interceptor)

        //在主模块依赖时使用反射获取日志弹窗抓包拦截器类实例初始化
//        var captureInterceptor: Interceptor? = null
//        try {
//            val clazz =
//                Class.forName("cn.coderpig.cp_network_capture.interceptor.CaptureInterceptor")
//            val constructor = clazz.getDeclaredConstructor()
//            captureInterceptor = constructor.newInstance() as Interceptor
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        // 抓包拦截器不为null，添加拦截器
//        captureInterceptor?.let { builder.addInterceptor(it) }
    }

}


class RetrofitBuild(
    url: String, client: OkHttpClient,
    gsonFactory: GsonConverterFactory
) {
    val retrofit: Retrofit = Retrofit.Builder().apply {
        baseUrl(url)
        client(client)
        addConverterFactory(gsonFactory)

    }.build()
}

/**
 * save cookie string
 */
fun encodeCookie(cookies: List<String>): String {
    val sb = StringBuilder()
    val set = HashSet<String>()
    cookies
        .map { cookie ->
            cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }
        .forEach { it ->
            it.filterNot { set.contains(it) }.forEach { set.add(it) }
        }

    val ite = set.iterator()
    while (ite.hasNext()) {
        val cookie = ite.next()
        sb.append(cookie).append(";")
    }

    val last = sb.lastIndexOf(";")
    if (sb.length - 1 == last) {
        sb.deleteCharAt(last)
    }

    return sb.toString()
}