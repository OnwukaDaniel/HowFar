package com.azur.howfar.bottomsheets

import android.app.Activity.RESULT_OK
import android.content.Context
import com.azur.howfar.activity.LoginActivityActivity.LoginType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.azur.howfar.R
import android.content.DialogInterface
import android.content.Intent
import android.provider.MediaStore
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.azur.howfar.databinding.BottomSheetGenderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BottomSheetGender(
    loginType: LoginType,
    context: Context?,
    activity: AppCompatActivity?,
    onReportedListner: OnGenderSelectListner
) {
    private val bottomSheetDialog: BottomSheetDialog
    private var selected = ""
    var submitButtonEnable = false
    val sheetDilogBinding: BottomSheetGenderBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.bottom_sheet_gender,
        null,
        false
    )
    private val scope = CoroutineScope(Dispatchers.IO)
    private val acceptedImageTypes: ArrayList<String> = arrayListOf("jpg", "png", "jpeg")

    interface OnGenderSelectListner {
        fun onSelect(g: String?, name: String?)
    }

    private val pickDisplayImageLauncher = activity!!.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
        scope.launch {
            try {
                if (it.data!!.data == null) return@launch
                if (it.resultCode == RESULT_OK) {
                    val dataUri = it.data!!.data
                    val contentResolver = context!!.contentResolver
                    val mime = MimeTypeMap.getSingleton()
                    if (mime.getExtensionFromMimeType(contentResolver?.getType(dataUri!!))!! in acceptedImageTypes) {
                    } else Snackbar.make(sheetDilogBinding.root, "Unsupported image type. Supported types are '.jpg', '.png' ", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
            }
        }
    })

    init {
        bottomSheetDialog = BottomSheetDialog(context!!, R.style.CustomBottomSheetDialogTheme)
        bottomSheetDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<View>(androidx.navigation.ui.R.id.design_bottom_sheet) as FrameLayout?
            BottomSheetBehavior.from(bottomSheet!!)
                .setState(BottomSheetBehavior.STATE_EXPANDED)
        }
        bottomSheetDialog.setContentView(sheetDilogBinding.root)
        bottomSheetDialog.show()
        sheetDilogBinding.male.background =
            ContextCompat.getDrawable(context, R.drawable.bg_stok_round)
        sheetDilogBinding.female.background =
            ContextCompat.getDrawable(context, R.drawable.bg_stok_round)
        sheetDilogBinding.male.setOnClickListener { v: View? ->
            selected = "MALE"
            sheetDilogBinding.male.background =
                ContextCompat.getDrawable(context, R.drawable.bg_stok_round_pink)
            sheetDilogBinding.female.background =
                ContextCompat.getDrawable(context, R.drawable.bg_stok_round)
        }
        sheetDilogBinding.female.setOnClickListener { v: View? ->
            selected = "FEMALE"
            sheetDilogBinding.female.background =
                ContextCompat.getDrawable(context, R.drawable.bg_stok_round_pink)
            sheetDilogBinding.male.background =
                ContextCompat.getDrawable(context, R.drawable.bg_stok_round)
        }
        if (loginType == LoginType.QUICK) {
            sheetDilogBinding.lytname.visibility = View.VISIBLE
        } else {
            sheetDilogBinding.lytname.visibility = View.GONE
        }
        sheetDilogBinding.btnSubmit.setOnClickListener { v: View? ->
            val name = sheetDilogBinding.etName.text.toString()
            if (loginType == LoginType.QUICK && name.isEmpty()) {
                Toast.makeText(context, "Enter Name First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selected.isEmpty()) {
                Toast.makeText(context, "Select Gender First", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onReportedListner.onSelect(selected, name)
            bottomSheetDialog.dismiss()
        }
        sheetDilogBinding.addImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            pickDisplayImageLauncher.launch(intent)
        }
    }
}