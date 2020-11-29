package com.zj.play.home.search

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.KeyboardUtils
import com.zj.core.util.showToast
import com.zj.core.view.base.BaseActivity
import com.zj.model.room.PlayDatabase
import com.zj.model.room.dao.HotKeyDao
import com.zj.model.room.entity.HotKey
import com.zj.play.R
import com.zj.play.home.search.article.ArticleListActivity
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.launch

class SearchActivity : BaseActivity(), View.OnClickListener, TextView.OnEditorActionListener {

    override fun getLayoutId(): Int = R.layout.activity_search

    private lateinit var hotKeyDao: HotKeyDao
    private val viewModel by lazy { ViewModelProvider(this).get(SearchViewModel::class.java) }

    override fun onResume() {
        super.onResume()
        KeyboardUtils.showSoftInput(searchTxtKeyword)
    }

    override fun initData() {
        viewModel.getDataList(true)
        hotKeyDao = PlayDatabase.getDatabase(this).hotKeyDao()
        setDataStatus(viewModel.dataLiveData) {
            if (it.isNotEmpty() && viewModel.dataList.isEmpty()) {
                viewModel.dataList.clear()
                viewModel.dataList.addAll(it)
            }
            addFlowView()
        }
    }

    override fun isSearchPage(): Boolean {
        return true
    }

    override fun onPause() {
        super.onPause()
        KeyboardUtils.hideSoftInput(searchTxtKeyword)
    }

    private fun addFlowView() {
        //往容器内添加TextView数据
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(10, 5, 10, 15);
        if (searchFlowLayout != null) {
            searchFlowLayout.removeAllViews()
        }
        for (i in 0 until viewModel.dataList.size) {
            val item = View.inflate(this, R.layout.layout_search_item, null)
            val tv = item.findViewById<TextView>(R.id.searchTvName)
            val delete = item.findViewById<LinearLayout>(R.id.searchLlDelete)
            val name = viewModel.dataList[i].name
            if (viewModel.dataList[i].order > 0) {
                delete.visibility = View.GONE
            }
            tv.text = name
            tv.setOnClickListener {
                ArticleListActivity.actionStart(this, name)
            }
            delete.setOnClickListener {
                lifecycleScope.launch {
                    hotKeyDao.delete(viewModel.dataList[i])
                }
                searchFlowLayout.removeView(item)
            }
            searchFlowLayout.addView(item, layoutParams)
        }
    }


    override fun initView() {
        searchImgBack.setOnClickListener(this)
        searchTxtRight.setOnClickListener(this)
        searchTxtKeyword.setOnEditorActionListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.searchImgBack -> {
                finish()
            }
            R.id.searchTxtRight -> {
                toSearch()
                Log.e("ZHUJIANG", "toSearch: searchTxtRight")
            }
        }
    }

    private fun toSearch() {
        Log.e("ZHUJIANG", "toSearch: ")
        val keyword = searchTxtKeyword.text.toString()
        if (TextUtils.isEmpty(keyword)) {
            showToast(getString(R.string.keyword_not_null))
            return
        }

        lifecycleScope.launch {
            hotKeyDao.insert(HotKey(id = -1, name = keyword))
        }
        ArticleListActivity.actionStart(this, keyword)
    }

    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        //以下方法防止两次发送请求
        return if (actionId == EditorInfo.IME_ACTION_SEND ||
            event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
        ) {
            when (event!!.action) {
                KeyEvent.ACTION_UP -> {
                    //发送请求
                    toSearch()
                    true
                }
                else -> true
            }
        } else false


    }

}
