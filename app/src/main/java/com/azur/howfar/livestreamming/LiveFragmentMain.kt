package com.azur.howfar.livestreamming

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.azur.howfar.R
import com.azur.howfar.adapter.DotAdapter
import com.azur.howfar.databinding.FragmentLiveBinding
import com.azur.howfar.home.adapter.BannerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class LiveFragmentMain : Fragment() {
    private lateinit var binding: FragmentLiveBinding
    private lateinit var tabAdapter: TabAdapter
    var bannerAdapter = BannerAdapter()
    private val liveListFragment = LiveListFragment()
    private val livePopularFragment = LivePopularFragment()
    val handler = Handler(Looper.getMainLooper())
    val runnable: Runnable = object : Runnable {
        var pos = 0
        var flag = true
        override fun run() {
            if (pos == bannerAdapter.itemCount - 1) flag = false else if (pos == 0) flag = true
            if (flag) pos++ else pos--
            binding.rvBanner.smoothScrollToPosition(pos)
            handler.postDelayed(this, 2000)
        }
    }

    inner class TabAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        lateinit var dataset: ArrayList<Fragment>
        override fun getItemCount(): Int = dataset.size
        override fun createFragment(position: Int): Fragment {
            return dataset[position]
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLiveBinding.inflate(inflater, container, false)
        initView()
        setupLogicAutoSlider()
        return binding.root
    }

    private fun setupLogicAutoSlider() {
        val dotAdapter = DotAdapter(bannerAdapter.itemCount, R.color.pink)
        binding.rvBanner.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvBanner.adapter = bannerAdapter
        binding.rvDots.adapter = dotAdapter
        binding.rvBanner.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val myLayoutManager = binding.rvBanner.layoutManager as LinearLayoutManager?
                val scrollPosition = myLayoutManager!!.findFirstVisibleItemPosition()
                dotAdapter.changeDot(scrollPosition)
            }
        })
        handler.postDelayed(runnable, 2000)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }

    private fun initView() {
        tabAdapter = TabAdapter(requireActivity())
        binding.tablayout1.setBackgroundColor(Color.TRANSPARENT)

        val tabsText = arrayListOf("All", "Popular", "Following")
        tabAdapter.dataset = arrayListOf(liveListFragment, livePopularFragment, LiveListFollowingFragment())
        binding.viewPager.adapter = tabAdapter
        TabLayoutMediator(binding.tablayout1, binding.viewPager) { tabs, position -> tabs.text = tabsText[position] }.attach()
        /*binding.tablayout1.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val v = tab.customView
                if (v != null) {
                    val tv = v.findViewById<View>(R.id.tvTab) as TextView
                    tv.setTextColor(ContextCompat.getColor(activity!!, R.color.white))
                    tv.backgroundTintList = ContextCompat.getColorStateList(activity!!, R.color.pink)
                    tv.textSize = 16f
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                //ll
                val v = tab.customView
                if (v != null) {
                    val tv = v.findViewById<View>(R.id.tvTab) as TextView
                    tv.setTextColor(ContextCompat.getColor(activity!!, R.color.text_gray))
                    tv.backgroundTintList = ContextCompat.getColorStateList(activity!!, R.color.grayinsta)
                    tv.textSize = 14f
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
        setTab(arrayOf("All", "Popular", "Following"))*/
    }

    private fun setTab(country: Array<String>) {
        binding.tablayout1.tabGravity = TabLayout.GRAVITY_FILL
        binding.tablayout1.removeAllTabs()
        for (i in country.indices) {
            binding.tablayout1.addTab(binding.tablayout1.newTab().setCustomView(createCustomView(i, country[i])))
        }
        val tabLayout = binding.tablayout1
        val test = tabLayout.getChildAt(0) as ViewGroup //tabs is your Tablayout
        val tabLen = test.childCount
        for (i in 0 until tabLen) {
            val v = test.getChildAt(i)
            v.setPadding(10, 0, 10, 0)
        }
    }

    private fun createCustomView(i: Int, s: String): View {
        val v = LayoutInflater.from(activity).inflate(R.layout.custom_tabhorizontol, null)
        val tv = v.findViewById<View>(R.id.tvTab) as TextView
        tv.text = s
        if (i == 0) {
            tv.setTextColor(ContextCompat.getColor(requireActivity(), R.color.white))
        } else {
            tv.setTextColor(ContextCompat.getColor(requireActivity(), R.color.text_gray))
            tv.backgroundTintList = ContextCompat.getColorStateList(requireActivity(), R.color.grayinsta)
        }
        return v
    }
}