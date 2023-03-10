package com.azur.howfar.howfarchat

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.azur.howfar.R
import com.azur.howfar.activity.BaseActivity
import com.azur.howfar.activity.LoginActivityActivity
import com.azur.howfar.activity.SplashActivityLike
import com.azur.howfar.databinding.ActivityChatLandingBinding
import com.azur.howfar.howfarchat.chat.ActivitySearchChat
import com.azur.howfar.howfarchat.groupChat.ActivityNewGroup
import com.azur.howfar.howfarchat.groupChat.Contact
import com.azur.howfar.howfarchat.groupChat.FragmentGroupChats
import com.azur.howfar.howfarchat.settings.SettingsFragment
import com.azur.howfar.howfarchat.status.ActivityCreateStatus
import com.azur.howfar.howfarchat.status.FragmentStatus
import com.azur.howfar.howfarchat.status.StatusType
import com.azur.howfar.howfarwallet.ActivityFingerPrint
import com.azur.howfar.livedata.ValueEventLiveData
import com.azur.howfar.models.BannerData
import com.azur.howfar.models.EventListenerType.onDataChange
import com.azur.howfar.models.FingerprintRoute.HOW_FAR_PAY
import com.azur.howfar.models.UserProfile
import com.azur.howfar.services.MsgBroadcast
import com.azur.howfar.user.EditProfileActivity
import com.azur.howfar.user.wallet.MyWalletActivity
import com.azur.howfar.utils.CallUtils
import com.azur.howfar.utils.HFCoinUtils
import com.azur.howfar.utils.Util
import com.azur.howfar.viewmodel.BooleanViewModel
import com.azur.howfar.viewmodel.UserProfileViewmodel
import com.azur.howfar.workManger.HowFarAnalyticsTypes
import com.azur.howfar.workManger.OpenAppWorkManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatLanding : BaseActivity(), View.OnClickListener {
    private val binding by lazy { ActivityChatLandingBinding.inflate(layoutInflater) }
    private lateinit var chatLandingTabAdapter: ChatLandingTabAdapter
    private val fragmentChats = FragmentChats()
    private val fragmentGroupChats = FragmentGroupChats()
    private val fragmentStatus = FragmentStatus()
    private var contacts: ArrayList<Contact> = arrayListOf()
    private val fragmentCall = FragmentCall()
    private var balanceSwitch = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private var callSoundPool: SoundPool? = null
    private var callSound = 1
    private val booleanViewModel by viewModels<BooleanViewModel>()
    private val userProfileViewModel by viewModels<UserProfileViewmodel>()
    private var timeRef = FirebaseDatabase.getInstance().reference
    private val myAuth = FirebaseAuth.getInstance().currentUser!!.uid
    private lateinit var pref: SharedPreferences
    private var contactJson = ""
    private lateinit var callUtils: CallUtils
    private var permissionDialog: AlertDialog? = null
    private val workManager = WorkManager.getInstance(this)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions[Manifest.permission.READ_CONTACTS] == true -> {
                scope.launch {
                    contacts = Util.getAllSavedContacts(this@ChatLanding).first
                    runOnUiThread {
                        contactJson = Gson().toJson(contacts)
                        pref.edit().putString(getString(R.string.contacts_key), contactJson).apply()
                    }
                }
            }
            permissions[Manifest.permission.READ_CONTACTS] == false -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
                    callUtils.permissionRationale(message = "HowFar needs CONTACTS permission to deliver best notification experience\nGrant app permission")
                }
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startAlarm() {
        val fiveMin = 1 * 60 * 1000L
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MsgBroadcast::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, fiveMin, pendingIntent)
    }

    private fun askPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> {
                scope.launch {
                    contacts = Util.getAllSavedContacts(this@ChatLanding).first
                    runOnUiThread {
                        contactJson = Gson().toJson(contacts)
                        pref.edit().putString(getString(R.string.contacts_key), contactJson).apply()
                    }
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                val message = "HowFar needs CONTACTS permission to deliver best notification experience\nGrant app permission"
                val permissionBuilder = AlertDialog.Builder(this)
                permissionBuilder.setTitle("App permission")
                permissionBuilder.setMessage(message)
                permissionBuilder.setPositiveButton("Set") { dialog, _ ->
                    callUtils.openAppSettings()
                    dialog.cancel()
                }.setNegativeButton("No") { dialog, _ ->
                    dialog.cancel()
                }.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                permissionDialog = permissionBuilder.create()
                permissionDialog!!.show()
            }
            else -> permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
        pref = getSharedPreferences(getString(R.string.ALL_PREFERENCE), Context.MODE_PRIVATE)
        val theme = pref.getInt(getString(R.string.THEME_SHARED_PREFERENCE), R.style.Theme_HowFar)
        setTheme(theme)
        timeRef = FirebaseDatabase.getInstance().reference.child("time").child(myAuth)
        setContentView(binding.root)
        setSupportActionBar(binding.chattingLandingToolbar)
        callUtils = CallUtils(this, this)
        activeUserAnalytics()
        bannerDisplay()
        binding.navDayNight.setOnClickListener(this)
        //startAlarm()
        initSoundPool()
        binding.chatLandingAppIcon.setOnClickListener {
            if (!binding.chatLandingDrawer.isDrawerOpen(GravityCompat.START)) {
                binding.chatLandingDrawer.openDrawer(GravityCompat.START)
            }
        }

        binding.eyeSwitch.setOnClickListener(this)
        binding.navPayBils.setOnClickListener(this)
        binding.navPay.setOnClickListener(this)
        binding.navTv.setOnClickListener(this)
        binding.navTaxi.setOnClickListener(this)
        binding.navData.setOnClickListener(this)
        binding.navCoin.setOnClickListener(this)
        binding.navData.setOnClickListener(this)
        binding.navGames.setOnClickListener(this)
        binding.navLike.setOnClickListener(this)
        binding.navMarket.setOnClickListener(this)

        binding.chatChat.setOnClickListener(this)
        binding.chatLike.setOnClickListener(this)
        binding.chatPay.setOnClickListener(this)
        binding.chatSetting.setOnClickListener(this)
        binding.mainFab.setOnClickListener(this)
        binding.navPics.setOnClickListener(this)

        binding.wallet.setOnClickListener(this)
        chatLandingTabAdapter = ChatLandingTabAdapter(this)
        val tabsText = arrayListOf("Chats", "Channel", "Story", "Calls")
        chatLandingTabAdapter.dataset = arrayListOf(fragmentChats, fragmentGroupChats, fragmentStatus, fragmentCall)

        binding.chattingLandingViewPager.offscreenPageLimit = 3
        binding.chattingLandingViewPager.adapter = chatLandingTabAdapter
        TabLayoutMediator(binding.chattingLandingTabsLayout, binding.chattingLandingViewPager) { tabs, position -> tabs.text = tabsText[position] }.attach()

        val historyRef = FirebaseDatabase.getInstance().reference.child(TRANSFER_HISTORY).child(myAuth)
        ValueEventLiveData(historyRef).observe(this) {
            when (it.second) {
                onDataChange -> {
                    val available = HFCoinUtils.checkBalance(it.first)
                    binding.balanceHfc.text = available.toString()
                }
            }
        }

        val usersRef = FirebaseDatabase.getInstance().reference.child(USER_DETAILS).child(FirebaseAuth.getInstance().currentUser!!.uid)
        ValueEventLiveData(usersRef).observe(this) {
            when (it.second) {
                onDataChange -> {
                    val user = it.first.getValue(UserProfile::class.java)!!
                    userProfileViewModel.setUserProfile(user)
                    binding.navUsername.text = user.name
                    if (!isDestroyed) Glide.with(this).load(user.image).error(R.drawable.ic_avatar).centerCrop().into(binding.navPics)
                }
            }
        }
        binding.chattingLandingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                booleanViewModel.setSwitch(true)
            }

            override fun onPageSelected(position: Int) = Unit
            override fun onPageScrollStateChanged(state: Int) = Unit
        })
        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            runOnUiThread {
                booleanViewModel.setStopLoadingShimmer(true)
            }
        }
        binding.coinCashSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.cashRoot.visibility = View.GONE
            binding.coinRoot.visibility = View.GONE
            if (isChecked) binding.cashRoot.visibility = View.VISIBLE else binding.coinRoot.visibility = View.VISIBLE
        }
        binding.chattingLandingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (binding.chattingLandingViewPager.scrollState == ViewPager2.SCROLL_STATE_IDLE && binding.chattingLandingViewPager.currentItem == 2) {
                    binding.mainFab.setImageResource(R.drawable.plus)
                } else if (binding.chattingLandingViewPager.currentItem != 2) {
                    binding.mainFab.setImageResource(R.drawable.plus)
                }
            }
        })
    }

    private fun bannerDisplay() {
        val BANNER_PROMOTION = "BANNER_PROMOTION"
        val bannerRef = FirebaseDatabase.getInstance().reference.child(BANNER_PROMOTION).child(myAuth)
        ValueEventLiveData(bannerRef).observe(this) {
            when (it.second) {
                onDataChange -> {
                    val bannerData = it.first.getValue(BannerData::class.java)!!
                    when (bannerData.seen) {
                        false -> {
                            val view = layoutInflater.inflate(R.layout.banner_message, binding.root, false)
                            val param = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            view.layoutParams = param
                            binding.root.addView(view)
                            val msgText: TextView = view.findViewById(R.id.banner_message)
                            val bannerImage: ImageView = view.findViewById(R.id.banner_image)
                            when (bannerData.image) {
                                "" -> {
                                    msgText.minLines = 12
                                    bannerImage.visibility = View.GONE
                                }
                                else -> {
                                    msgText.minLines = 1
                                    bannerImage.visibility = View.VISIBLE
                                    Glide.with(this).load(bannerData.image).centerCrop().into(bannerImage)
                                }
                            }
                            val bannerCancel: ImageView = view.findViewById(R.id.banner_cancel)
                            msgText.text = bannerData.message
                            bannerCancel.setOnClickListener { binding.root.removeView(view) }
                            bannerRef.removeValue()
                        }
                    }
                }
            }
        }
    }

    private fun activeUserAnalytics() {
        val workRequest = OneTimeWorkRequestBuilder<OpenAppWorkManager>().addTag("analytics")
            .setInputData(workDataOf("action" to HowFarAnalyticsTypes.OPEN_APP))
            .build()
        workManager.enqueue(workRequest)
    }

    override fun onPause() {
        if (permissionDialog != null) permissionDialog!!.dismiss()
        super.onPause()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        callSoundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        callSound = callSoundPool!!.load(this, R.raw.cell_phone_vibrate, 1)
    }

    override fun onResume() {
        super.onResume()
        //binding.cashRoot.visibility = View.VISIBLE
        //binding.coinRoot.visibility = View.GONE
        //binding.coinCashSwitch.isChecked = false

        pref.edit().putInt(getString(R.string.in_chat_phone_key), 0).apply()
        FirebaseAuth.getInstance().addAuthStateListener { p0 ->
            if (p0.currentUser == null) {
                val intent = Intent(this@ChatLanding, LoginActivityActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
            }
        }
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> binding.navDayNight.setImageResource(R.drawable.night_mode_toggle)
            Configuration.UI_MODE_NIGHT_NO -> binding.navDayNight.setImageResource(R.drawable.day_mode_toggle)
            Configuration.UI_MODE_NIGHT_UNDEFINED -> binding.navDayNight.setImageResource(R.drawable.day_mode_toggle)
        }
    }

    inner class ChatLandingTabAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        lateinit var dataset: ArrayList<Fragment>
        override fun getItemCount(): Int = dataset.size
        override fun createFragment(position: Int): Fragment {
            return dataset[position]
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.wallet -> {
                startActivity(Intent(this, MyWalletActivity::class.java))
                overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
                drawer()
            }
            R.id.eye_switch -> {
                balanceSwitch = !balanceSwitch
                if (balanceSwitch) {
                    binding.eyeSwitch.rotation = 180F
                    binding.moneyRoot.visibility = View.GONE
                    binding.chattingLandingFadedImage.updateLayoutParams {
                        width = 180
                        height = 90
                    }
                    binding.card.updateLayoutParams {
                        height = 300
                    }
                } else {
                    binding.eyeSwitch.rotation = 360F
                    binding.moneyRoot.visibility = View.VISIBLE
                    binding.chattingLandingFadedImage.updateLayoutParams {
                        width = 180
                        height = 160
                    }
                    binding.card.updateLayoutParams {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
            R.id.nav_like -> {
                startActivity(Intent(this, SplashActivityLike::class.java))
                overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
                drawer()
            }
            R.id.nav_pics -> {
                startActivity(Intent(this, EditProfileActivity::class.java))
                drawer()
            }
            R.id.nav_pay -> {
                startActivity(Intent(this, ActivityFingerPrint::class.java).putExtra("data", HOW_FAR_PAY))
                overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
                drawer()
            }
            R.id.nav_coin -> {
                startActivity(Intent(this, MyWalletActivity::class.java))
                overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
                drawer()
            }
            R.id.nav_data -> {
                showSnackBar(binding.root, "Coming soon")
                drawer()
            }
            R.id.nav_pay_bils -> {
                showSnackBar(binding.root, "Coming soon")
                drawer()
            }
            R.id.nav_tv -> {
                showSnackBar(binding.root, "Coming soon")
                drawer()
            }
            R.id.nav_games -> {
                showSnackBar(binding.root, "Coming soon")
                drawer()
            }
            R.id.nav_market -> {
                showSnackBar(binding.root, "Coming soon")
                drawer()
            }
            R.id.nav_taxi -> {
                showSnackBar(binding.root, "Coming soon")
                drawer()
            }
            R.id.nav_day_night -> {
                var isNight = false
                when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        isNight = true
                        binding.navDayNight.setImageResource(R.drawable.day_mode_toggle)
                    }
                    Configuration.UI_MODE_NIGHT_NO -> {
                        isNight = false
                        binding.navDayNight.setImageResource(R.drawable.night_mode_toggle)
                    }
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                        isNight = false
                        binding.navDayNight.setImageResource(R.drawable.night_mode_toggle)
                    }
                }
                AppCompatDelegate.setDefaultNightMode(if (!isNight) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            }
            R.id.chat_chat -> {
            }
            R.id.chat_like -> {
                startActivity(Intent(this, SplashActivityLike::class.java))
                overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
            }
            R.id.chat_pay -> {
                startActivity(Intent(this, ActivityFingerPrint::class.java).putExtra("data", HOW_FAR_PAY))
                overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
            }
            R.id.chat_setting -> {
                supportFragmentManager.beginTransaction().addToBackStack("settings")
                    .setCustomAnimations(R.anim.enter_right_to_left, R.anim.exit_right_to_left, R.anim.enter_left_to_right, R.anim.exit_left_to_right)
                    .replace(R.id.chat_landing_root, SettingsFragment()).commit()
            }
            R.id.main_fab -> {
                when (binding.chattingLandingViewPager.currentItem) {
                    0 -> {
                        val intent = Intent(this, ActivitySearchChat::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                    1 -> {
                        val intent = Intent(this, ActivityNewGroup::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.enter_right_to_left, R.anim.exit_right_to_left)
                    }
                    2 -> {
                        val intent = Intent(this, ActivityCreateStatus::class.java)
                        intent.putExtra("type", StatusType.IMAGE)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun drawer() {
        if (binding.chatLandingDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.chatLandingDrawer.closeDrawer(GravityCompat.START)
        }
    }

    companion object {
        const val TRANSFER_HISTORY = "user_coins_transfer"
        const val USER_DETAILS = "user_details"
        const val CHAT_REFERENCE = "chat_reference"
        const val BANNER_PROMOTION = "BANNER_PROMOTION"
    }
}