package us.synergize_apps.oliapp.ui.fragments.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.activity_user_profile.*
import kotlinx.android.synthetic.main.activity_user_profile.iv_user_photo
import us.synergize_apps.oliapp.R
import us.synergize_apps.oliapp.models.User
import us.synergize_apps.oliapp.ui.activities.BaseActivity
import us.synergize_apps.oliapp.ui.activities.MainActivity
import us.synergize_apps.oliapp.ui.activities.firestore.FireStoreClass
import us.synergize_apps.oliapp.utils.Constants
import us.synergize_apps.oliapp.utils.GlideLoader
import java.io.IOException


class UserProfileActivity : BaseActivity(), View.OnClickListener {

    private lateinit var oliUserDetails: User
    private var oliSelectedImageFileUri: Uri? = null
    private var oliProfileImageURL: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        if (intent.hasExtra(Constants.USER_EXTENDED_INFO)) {
            oliUserDetails = intent.getParcelableExtra(Constants.USER_EXTENDED_INFO)!!
        }

        til_first_name.setText(oliUserDetails.firstName)
        til_last_name.setText(oliUserDetails.lastName)
        til_email.setText(oliUserDetails.email)

        if (oliUserDetails.profileComplete == 0) {
            profileTitle.text = resources.getString(R.string.title_complete_profile)
            til_first_name.isEnabled = false
            til_last_name.isEnabled = false
        }else{
            setupActionBar()
            profileTitle.text = resources.getString(R.string.title_edit_profile)
            GlideLoader(this@UserProfileActivity).loadUserPicture(oliUserDetails.image,
                    iv_user_photo)

            if (oliUserDetails.mobile != 0L){
                til_mobile_number.setText(oliUserDetails.mobile.toString())
            }
        }

        iv_user_photo.setOnClickListener(this@UserProfileActivity)
        btn_submit.setOnClickListener(this@UserProfileActivity)
    }



    override fun onClick(view: View?) {
        if (view != null) {
            when (view.id) {
                R.id.iv_user_photo -> {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        Constants.showImageChoose(this)
                    } else {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            Constants.READ_STORAGE_PERMISSION_CODE
                        )
                    }
                }

                R.id.btn_submit ->{
                    if (validateUserProfileDetails()){
                        showProgressDialog(resources.getString(R.string.please_wait))

                        if (oliSelectedImageFileUri != null) {
                            FireStoreClass().uploadImageToCloud(
                                this@UserProfileActivity,
                                oliSelectedImageFileUri!!, Constants.USER_PROFILE_IMAGE
                            )
                        }else{
                            updateUserProfileDetails()
                        }



                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChoose(this)
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(
                        this,
                        resources.getString(R.string.read_storage_permission_denied),
                        Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.PICK_IMAGE_REQUEST_CODE) {
                if (data != null) {
                    try {
                        oliSelectedImageFileUri = data.data!!

                        //iv_user_photo.setImageURI(Uri.parse(selectedImageFileUri.toString()))
                        GlideLoader(this@UserProfileActivity).loadUserPicture(
                                oliSelectedImageFileUri!!,
                                iv_user_photo)
                    }catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                                this@UserProfileActivity,
                                resources.getString(R.string.image_select_fail),
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("Request Cancelled", "Image select cancelled.")
        }
    }

    private fun setupActionBar() {

        setSupportActionBar(profileToolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }

        profileToolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun validateUserProfileDetails(): Boolean {
        return when {

            TextUtils.isEmpty(til_mobile_number.text.toString().trim { it <= ' ' }) -> {
                showErrorSnackBar(resources.getString(R.string.err_msg_enter_mobile_number), true)
                false
            }
            else -> {
                true
            }
        }
    }

    private fun  updateUserProfileDetails(){
        val userHashMap = HashMap<String, Any>()

        val firstName = til_first_name.text.toString().trim { it <= ' ' }
        if (firstName != oliUserDetails.firstName){
            userHashMap[Constants.FIRST_NAME] = firstName
        }
        val lastName = til_last_name.text.toString().trim { it <= ' ' }
        if (lastName != oliUserDetails.lastName){
            userHashMap[Constants.LAST_NAME] = lastName
        }
        val email = til_email.text.toString().trim { it <= ' ' }
        if (lastName != oliUserDetails.email){
            userHashMap[Constants.EMAIL] = email
        }

        val mobileNumber = til_mobile_number.text.toString().trim { it <= ' ' }
        if (mobileNumber.isNotEmpty() && mobileNumber != oliUserDetails.mobile.toString()){
            userHashMap[Constants.MOBILE] = mobileNumber.toLong()
        }
        if (oliProfileImageURL.isNotEmpty()){
            userHashMap[Constants.IMAGE] = oliProfileImageURL
        }
        if (oliUserDetails.profileComplete == 0) {
            userHashMap[Constants.PROFILE_COMPLETE] = 1
        }
        FireStoreClass().updateUserProfileData(this@UserProfileActivity, userHashMap)
    }

    fun userProfileUpdateSuccess() {
        hideProgressDialog()
        Toast.makeText(
                this,
                resources.getString(R.string.profile_update_success),
                Toast.LENGTH_SHORT
        ).show()

        startActivity(Intent(this@UserProfileActivity, DashboardActivity::class.java))
        finish()
    }

    fun imageUploadSuccess(imageURL: String) {
        oliProfileImageURL = imageURL
        updateUserProfileDetails()
    }
}