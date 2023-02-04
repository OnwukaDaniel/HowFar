package com.azur.howfar.activity

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.azur.howfar.R
import com.azur.howfar.databinding.ActivityLoginActivityBinding
import com.azur.howfar.databinding.BottomSheetGenderBinding
import com.azur.howfar.databinding.TextFragmentBinding
import com.azur.howfar.utils.CanHubImage
import com.azur.howfar.utils.ImageCompressor
import com.azur.howfar.viewmodel.SignUpViewModel
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.synnapps.carouselview.ImageListener
import java.io.ByteArrayInputStream

class LoginActivityActivity : AppCompatActivity(), View.OnClickListener {
    private val binding by lazy { ActivityLoginActivityBinding.inflate(layoutInflater) }
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var selected = ""
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sheetDialogBinding: BottomSheetGenderBinding
    private val images: ArrayList<Int> = arrayListOf(
        R.drawable.person_image2,
        R.drawable.person_image5,
        R.drawable.background_image_girl,
        R.drawable.known,
        R.drawable.major,
        R.drawable.major2,
        R.drawable.image1,
        R.drawable.image2,
        R.drawable.image3,
    )
    private var imageStream: Pair<ByteArrayInputStream, ByteArray>? = null
    private val signUpViewModel: SignUpViewModel by viewModels()
    private var textFragments = arrayListOf<Fragment>()
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var canHubImage: CanHubImage
    private val runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 4000)
            val pos = if (binding.textViewPager.currentItem < textFragments.size) binding.textViewPager.currentItem + 1 else 1
            binding.textViewPager.setCurrentItem(pos, true)
        }
    }

    init {
        images.shuffle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        canHubImage = CanHubImage(this)
        bottomSheetDialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme)
        sheetDialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.bottom_sheet_gender, null, false)

        val imageListener = ImageListener { position, imageView -> imageView.setImageResource(images[position]) }
        binding.carouselView.setImageListener(imageListener)
        binding.carouselView.pageCount = images.size
        sheetDialogBinding.male.setOnClickListener(this)
        sheetDialogBinding.female.setOnClickListener(this)
        sheetDialogBinding.btnSubmit.setOnClickListener(this)
        sheetDialogBinding.addImage.setOnClickListener(this)
        bottomSheetDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        bottomSheetDialog.setContentView(sheetDialogBinding.root)
        sheetDialogBinding.male.background = ContextCompat.getDrawable(this, R.drawable.bg_stok_round)
        sheetDialogBinding.female.background = ContextCompat.getDrawable(this, R.drawable.bg_stok_round)
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<View>(androidx.navigation.ui.R.id.design_bottom_sheet) as FrameLayout?
            BottomSheetBehavior.from(bottomSheet!!).setState(BottomSheetBehavior.STATE_EXPANDED)
        }

        val texts = arrayListOf(
            "Find new \nfriends nearby" to "With millions of users all over the world, we gives you the ability to connect with people no matter " +
                    "where you are.",
            "Connect with\nfriends nearby" to "with millions of users all over the world, we give you the ability to connect with people no matter where you " +
                    "are.",
            "Entertainment\nat its peak" to "with millions of users all over the world, we give you the ability to connect with people no matter where you " +
                    "are.",
            "Enjoy Comfort \nin  luxury ride" to "with millions of users all over the world, we give you the ability to connect with people no matter where you " +
                    "are.",
        )
        viewPagerAdapter = ViewPagerAdapter(this)
        for (pair in texts) {
            val textFrag = TextFragment()
            val bundle = Bundle()
            val json = Gson().toJson(pair)
            bundle.putString("json", json)
            textFrag.arguments = bundle
            textFragments.add(textFrag)
        }
        viewPagerAdapter.textFragments = textFragments
        binding.textViewPager.adapter = viewPagerAdapter
        handler.postDelayed(runnable, 1000)
    }

    inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        var textFragments: ArrayList<Fragment> = arrayListOf()
        override fun getItemCount() = textFragments.size
        override fun createFragment(position: Int) = textFragments[position]
    }

    override fun onResume() {
        super.onResume()
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        handler.removeCallbacks(runnable)
    }

    fun onClickPhone(view: View?) {
        bottomSheetDialog.dismiss()
        bottomSheetDialog.show()
    }

    fun onClickLoginPhone(view: View?) {
        supportFragmentManager.beginTransaction().addToBackStack("phone").replace(R.id.login_root, FragmentSignInPhone()).commit()
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent!!
            //val uriFilePath = result.getUriFilePath(this) // optional usage
            try {
                val pair: Pair<ByteArrayInputStream, ByteArray> = ImageCompressor.compressImage(uriContent, this, null)
                imageStream = pair
                Glide.with(this).load(pair.second).circleCrop().into(sheetDialogBinding.addImage)
            } catch (e: Exception) {
                println("Exception *********************************************************** ${e.printStackTrace()}")
            }
        } else {
            val exception = result.error
            exception!!.printStackTrace()
        }
    }

    enum class LoginType {
        QUICK
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnSubmit -> {
                val name = sheetDialogBinding.etName.text.toString()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter Name First", Toast.LENGTH_SHORT).show()
                    return
                }
                if (selected.isEmpty()) {
                    Toast.makeText(this, "Select Gender First", Toast.LENGTH_SHORT).show()
                    return
                }
                if (imageStream == null) {
                    Toast.makeText(this, "Select Image First", Toast.LENGTH_SHORT).show()
                    return
                }
                val three = ThreeSignUpData(name = name, gender = selected, stream = imageStream!!)
                signUpViewModel.setThreeInput(three)
                supportFragmentManager.beginTransaction().addToBackStack("phone").replace(R.id.login_root, FragmentVerifyPhone()).commit()
                bottomSheetDialog.dismiss()
            }
            R.id.male -> {
                selected = "MALE"
                sheetDialogBinding.male.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_stok_round_pink)
                sheetDialogBinding.female.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_stok_round)
            }
            R.id.female -> {
                selected = "FEMALE"
                sheetDialogBinding.female.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_stok_round_pink)
                sheetDialogBinding.male.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_stok_round)
            }
            R.id.add_image -> canHubImage.openCanHub(cropImage)
        }
    }
}

class TextFragment : Fragment() {
    private lateinit var binding: TextFragmentBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = TextFragmentBinding.inflate(inflater, container, false)
        val json = requireArguments().getString("json")
        val pair = Gson().fromJson(json, Pair::class.java)
        binding.textView1.text = pair.first.toString()
        binding.textView2.text = pair.second.toString()
        return binding.root
    }
}

data class ThreeSignUpData(
    var name: String = "",
    var stream: Pair<ByteArrayInputStream, ByteArray>,
    var gender: String = "",
    var uri: String = "",
)