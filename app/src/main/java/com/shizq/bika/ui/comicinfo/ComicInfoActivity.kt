package com.shizq.bika.ui.comicinfo

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.size
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shizq.bika.BR
import com.shizq.bika.R
import com.shizq.bika.adapter.ChapterAdapter
import com.shizq.bika.adapter.RecommendAdapter
import com.shizq.bika.base.BaseActivity
import com.shizq.bika.databinding.ActivityComicinfoBinding
import com.shizq.bika.databinding.ViewChapterFooterViewBinding
import com.shizq.bika.db.History
import com.shizq.bika.ui.comiclist.ComicListActivity
import com.shizq.bika.ui.comment.CommentsActivity
import com.shizq.bika.ui.image.ImageActivity
import com.shizq.bika.ui.reader.ReaderActivity
import com.shizq.bika.utils.*
import com.shizq.bika.widget.SpacesItemDecoration
import com.shizq.bika.widget.UserViewDialog

/**
 * 漫画详情
 */

class ComicInfoActivity : BaseActivity<ActivityComicinfoBinding, ComicInfoViewModel>() {
    var fileserver: String = ""
    var imageurl: String = ""

    private lateinit var mAdapterChaper: ChapterAdapter
    private lateinit var chaperFooterBinding: ViewChapterFooterViewBinding
    private lateinit var mAdapterRecommend: RecommendAdapter

    private lateinit var userViewDialog: UserViewDialog

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_comicinfo
    }

    override fun initVariableId(): Int {
        return BR.viewModel
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {
        ViewCompat.setTransitionName(binding.comicinfoImage, "image")
        ViewCompat.setTransitionName(binding.comicinfoTitle, "title")
        //接收传递的信息
        fileserver = intent.getStringExtra("fileserver").toString()
        imageurl = intent.getStringExtra("imageurl").toString()
        viewModel.bookId = intent.getStringExtra("id")
        viewModel.title = intent.getStringExtra("title")
        viewModel.author = intent.getStringExtra("author")
        viewModel.totalViews = intent.getStringExtra("totalViews")

        //点击事件
        binding.clickListener = ClickListener()

        //toolbar
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.appbarlayout.addLiftOnScrollListener { elevation, backgroundColor ->
            //判断appbar滑动状态 显示隐藏标题
            if (elevation > 0) {
                binding.toolbar.title = viewModel.title
            } else {
                binding.toolbar.title = ""
            }
        }

        userViewDialog = UserViewDialog(this)

        //漫画章节
        mAdapterChaper = ChapterAdapter()
        binding.comicInfoChapterList.isNestedScrollingEnabled = false
        binding.comicInfoChapterList.layoutManager = LinearLayoutManager(this)
        binding.comicInfoChapterList.adapter = mAdapterChaper
        chaperFooterBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this), R.layout.view_chapter_footer_view,
            binding.comicInfoChapterList.parent as ViewGroup, false
        )
        binding.comicInfoChapterList.addFooterView(chaperFooterBinding.root)
        chaperFooterBinding.chapterFooterLayout.isEnabled = false
        //漫画推荐
        val lm = LinearLayoutManager(this)
        lm.orientation = LinearLayoutManager.HORIZONTAL
        binding.comicinfoRecommend.layoutManager = lm
        mAdapterRecommend = RecommendAdapter(this)
        binding.comicinfoRecommend.adapter = mAdapterRecommend

        //加载数据
        GlideApp.with(this)
            .load(GlideUrlNewKey(fileserver, imageurl))
            .placeholder(R.drawable.placeholder_transparent)
            .into(binding.comicinfoImage)
        binding.comicinfoAuthor.text = viewModel.author
        binding.comicinfoTotalViews.text = "指数：${viewModel.totalViews}"

        //网络请求
        if (binding.comicinfoTagslist.size < 1) {
            //防止重复加载 判断标签数是否为0
            binding.comicinfoProgressbar.visibility = View.VISIBLE
            viewModel.getInfo()
            viewModel.chapterPage = 0
            mAdapterChaper.clear()
            viewModel.getChapter()
            viewModel.getRecommend()
        }


    }

    //toolbar菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu_comicinfo, menu)
        return true
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
//                finishAfterTransition()
                finish()
            }
            R.id.action_bookmark -> {//Toolbar 收藏键
                binding.comicinfoProgressbar.visibility = View.VISIBLE
                viewModel.getFavourite()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    inner class ClickListener {
        fun Image() {
            //封面图片
            val intent = Intent(this@ComicInfoActivity, ImageActivity::class.java)
            intent.putExtra("fileserver", fileserver)
            intent.putExtra("imageurl", imageurl)
            val options = ActivityOptions.makeSceneTransitionAnimation(this@ComicInfoActivity, binding.comicinfoImage, "image")
            startActivity(intent, options.toBundle())
        }

        fun Author() {
            //作者
            val intent = Intent(this@ComicInfoActivity, ComicListActivity::class.java)
            intent.putExtra("tag", "author")
            intent.putExtra("title", binding.comicinfoAuthor.text.toString())
            intent.putExtra("value", binding.comicinfoAuthor.text.toString())
            startActivity(intent)
        }

        fun Translator() {
            //翻译人
            val intent = Intent(this@ComicInfoActivity, ComicListActivity::class.java)
            intent.putExtra("tag", "translate")
            intent.putExtra("title", binding.comicinfoTranslator.text.toString())
            intent.putExtra("value", binding.comicinfoTranslator.text.toString())
            startActivity(intent)
        }


        fun Comment() {//评论
            val intentComments = Intent(this@ComicInfoActivity, CommentsActivity::class.java)
            intentComments.putExtra("id", viewModel.bookId)
            intentComments.putExtra("comics_games", "comics")
            startActivity(intentComments)
        }

        fun Creator() {
            //搜索上传者
            val intent = Intent(this@ComicInfoActivity, ComicListActivity::class.java)
            intent.putExtra("tag", "knight")
            intent.putExtra("title", binding.comicinfoCreatorName.text.toString())
            intent.putExtra("value", viewModel.creatorId)
            startActivity(intent)
        }

        fun Like() {
            //喜欢
            binding.comicinfoProgressbar.visibility = View.VISIBLE
            viewModel.getLike()
        }

        fun CreatorLayout() {
            //上传者信息
            if (viewModel.creator != null) {
                userViewDialog.showUserDialog(viewModel.creator!!)
            }
        }

        fun Read() {
            //开始阅读 默认从第一话开始 以后加历史记录
            val intent = Intent(this@ComicInfoActivity, ReaderActivity::class.java)
            intent.putExtra("bookId", viewModel.bookId)
            intent.putExtra("order", 1)//查看的第几个章节
            intent.putExtra("chapterTotal", viewModel.chapterTotal)//总共多少章节
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun initViewObservable() {
        //漫画详情
        viewModel.liveData_info.observe(this) {
            binding.comicinfoProgressbar.visibility = View.GONE
            if (it.code == 200) {
                if (binding.comicinfoTagslist.size < 1) {
                    //漫画封面图片
                    if (imageurl == "") {
                        fileserver = it.data.comic.thumb.fileServer
                        imageurl = it.data.comic.thumb.path
                        GlideApp.with(this)
                            .load(
                                GlideUrlNewKey(
                                    it.data.comic.thumb.fileServer,
                                    it.data.comic.thumb.path
                                )
                            )
                            .placeholder(R.drawable.placeholder_transparent)
                            .into(binding.comicinfoImage)
                    }


                    //汉化组
                    if (!it.data.comic.chineseTeam.isNullOrEmpty()) {
                        binding.comicinfoTranslator.visibility = View.VISIBLE
                        binding.comicinfoTranslator.text = it.data.comic.chineseTeam
                    } else {
                        binding.comicinfoTranslator.visibility = View.GONE
                    }

                    //浏览量
                    binding.comicinfoTotalViews.text = "指数：${it.data.comic.viewsCount}"

                    //喜欢数
                    binding.comicinfoLikeText.text = "${it.data.comic.likesCount}人喜欢"

                    //是否喜欢
                    binding.comicinfoLikeImage.setImageResource(if (it.data.comic.isLiked) R.drawable.ic_favorite_24 else R.drawable.ic_favorite_border_24)

                    //评论数 是否关闭评论
                    if (it.data.comic.allowComment) {
                        binding.comicinfoComment.isClickable = true
                        binding.comicinfoCommentText.text = "${it.data.comic.commentsCount}条评论"
                    } else {
                        binding.comicinfoComment.isClickable = false
                        binding.comicinfoCommentText.text = "禁止评论"
                    }

                    //章节页数
                    if (it.data.comic.finished) {
                        binding.comicinfoChapter.text = "${it.data.comic.epsCount}章(完结)"
                    } else {
                        binding.comicinfoChapter.text = "${it.data.comic.epsCount}章"
                    }
                    binding.comicinfoPage.text = "${it.data.comic.pagesCount}页"

                    //是否收藏
                    if (binding.toolbar.menu.findItem(R.id.action_bookmark) != null) {
                        binding.toolbar.menu.findItem(R.id.action_bookmark)
                            .setIcon(if (it.data.comic.isFavourite) R.drawable.ic_bookmark_check else R.drawable.ic_bookmark_add)
                    }

                    //描述
                    if (!it.data.comic.description.isNullOrEmpty()) {
                        binding.comicinfoDescription.text = it.data.comic.description.trim()
                    }

                    //漫画分类
                    for (i in it.data.comic.categories.toSortedSet().toList()) {
                        val chip = Chip(this@ComicInfoActivity)
                        chip.text = i
                        chip.setEnsureMinTouchTargetSize(false)//去除视图的顶部和底部的额外空间
                        binding.comicinfoTagslist.addView(chip)
                        chip.setOnClickListener {
                            val intent =
                                Intent(this@ComicInfoActivity, ComicListActivity::class.java)
                            intent.putExtra("tag", "categories")
                            intent.putExtra("title", i)
                            intent.putExtra("value", i)
                            startActivity(intent)
                        }
                    }

                    //漫画标签
                    if (it.data.comic.tags.isNotEmpty()) {
                        binding.comicinfoTagsLayout.visibility = View.VISIBLE
                        for (i in it.data.comic.tags.toSortedSet().toList()) {
                            val chip = Chip(this@ComicInfoActivity)
                            chip.text = i
                            chip.setEnsureMinTouchTargetSize(false)//去除视图的顶部和底部的额外空间
                            binding.comicinfoTagslist1.addView(chip)
                            chip.setOnClickListener {
                                val intent =
                                    Intent(this@ComicInfoActivity, ComicListActivity::class.java)
                                intent.putExtra("tag", "tags")
                                intent.putExtra("title", i)
                                intent.putExtra("value", i)
                                startActivity(intent)
                            }
                        }
                    }

                    //上传者 头像
                    if (null != it.data.comic._creator.avatar) {
                        GlideApp.with(this)
                            .load(
                                GlideUrlNewKey(
                                    it.data.comic._creator.avatar.fileServer,
                                    it.data.comic._creator.avatar.path
                                )
                            )
                            .placeholder(R.drawable.placeholder_avatar_2)
                            .into(binding.comicinfoCreatorAvatar)
                    } else {
                        binding.comicinfoCreatorAvatar.setImageResource(R.drawable.placeholder_avatar)
                    }

                    //记录历史
                    val historyList = viewModel.getHistory()
                    if (historyList.isNotEmpty()) {
                        val history = History(
                            System.currentTimeMillis(),
                            historyList[0].title,
                            historyList[0].fileServer,
                            historyList[0].path,
                            historyList[0].comic_or_game,
                            historyList[0].author,
                            historyList[0].comic_or_game_id,
                            historyList[0].sort,
                            historyList[0].epsCount,
                            historyList[0].pagesCount,
                            historyList[0].finished,
                            historyList[0].likeCount,
                            historyList[0].ep,
                            historyList[0].page
                        )
                        history.id = historyList[0].id
                        //这个进行更新 //更新好象要主键
                        viewModel.updateHistory(history)//更新搜索记录

                    } else {
                        val history = History(
                            System.currentTimeMillis(),
                            viewModel.title.toString(),
                            fileserver,
                            imageurl,
                            "comic",
                            viewModel.author.toString(),
                            viewModel.bookId.toString(),
                            "",
                            "",
                            "",
                            false,
                            "",
                            "",
                            ""
                        )
                        //这个进行更新 //更新好象要主键
                        viewModel.insertHistory(history)//添加搜索记录
                    }
                }
            } else if (it.code == 400 && it.error == "1014") {
                MaterialAlertDialogBuilder(this)
                    .setTitle("本子审核中！")
                    .setPositiveButton("退出") { _, _ -> finish() }
                    .show()

            } else {
                //加载失败
                MaterialAlertDialogBuilder(this)
                    .setTitle("网络错误")
                    .setMessage("网络错误code=${it.code} error=${it.error} message=${it.message}")
                    .setPositiveButton("重试") { _, _ ->
                        binding.comicinfoProgressbar.visibility = View.VISIBLE
                        viewModel.getInfo()
                    }
                    .setNegativeButton("退出") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }

        //漫画章节
        viewModel.liveData_chapter.observe(this) {
            if (it.code == 200) {
                binding.comicinfoBtnRead.show()
                if (it.data.eps.page == 1) {//防止重复添加
                    mAdapterChaper.clear()
                    mAdapterChaper.addData(it.data.eps.docs)
                } else {
                    mAdapterChaper.addData(it.data.eps.docs)
                }

                if (it.data.eps.pages == it.data.eps.page) {
                    //总页数等于当前页数 不显示加载布局
                    chaperFooterBinding.chapterFooterLayout.isEnabled = false
                    chaperFooterBinding.chapterFooterText.setText(R.string.footer_end)
                } else {
                    chaperFooterBinding.chapterFooterLayout.isEnabled = true
                    chaperFooterBinding.chapterFooterText.setText(R.string.footer_more)
                }

            } else {
                chaperFooterBinding.chapterFooterLayout.isEnabled = true
                chaperFooterBinding.chapterFooterText.setText(R.string.footer_error)

            }
        }


        //漫画推荐
        viewModel.liveData_recommend.observe(this) {
            if (it.code == 200) {
                if (mAdapterRecommend.itemCount < 1) {
                    binding.comicinfoRecommend.addItemDecoration(
                        SpacesItemDecoration(
                            SpacesItemDecoration.px2dp(20F),
                            it.data.comics
                        )
                    )
                    mAdapterRecommend.addNewData(it.data.comics)
                }
            }
        }

        //漫画喜欢
        viewModel.liveData_like.observe(this) {
            binding.comicinfoProgressbar.visibility = View.GONE
            if (it.code == 200) {
                if (it.data.action == "like") {
                    binding.comicinfoLikeImage.setImageResource(R.drawable.ic_favorite_24)
                } else {
                    binding.comicinfoLikeImage.setImageResource(R.drawable.ic_favorite_border_24)
                }
            } else {
                //加载失败
                Toast.makeText(
                    this,
                    "网络错误，点击爱心失败code=${it.code} error=${it.error} message=${it.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

        //漫画收藏
        viewModel.liveData_favourite.observe(this) {
            binding.comicinfoProgressbar.visibility = View.GONE
            if (it.code == 200) {
                if (binding.toolbar.menu.findItem(R.id.action_bookmark) != null) {
                    if (it.data.action == "favourite") {
                        binding.toolbar.menu.findItem(R.id.action_bookmark)
                            .setIcon(R.drawable.ic_bookmark_check)
                    } else {
                        binding.toolbar.menu.findItem(R.id.action_bookmark)
                            .setIcon(R.drawable.ic_bookmark_add)
                    }
                }

            } else {
                //加载失败
                Toast.makeText(
                    this,
                    "网络错误，收藏失败code=${it.code} error=${it.error} message=${it.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }


        binding.comicInfoChapterList.setOnItemClickListener { v, position ->
            val intent = Intent(this@ComicInfoActivity, ReaderActivity::class.java)
            intent.putExtra("bookId", viewModel.bookId)
            intent.putExtra("order", mAdapterChaper.data[position].order)//查看的第几个章节
            intent.putExtra("chapterTotal", viewModel.chapterTotal)//总共多少章节
            startActivity(intent)
        }
        chaperFooterBinding.chapterFooterLayout.setOnClickListener {
            chaperFooterBinding.chapterFooterLayout.isEnabled = false
            chaperFooterBinding.chapterFooterText.setText(R.string.footer_loading)
            viewModel.getChapter()
        }

    }
}