package com.example.third.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.third.Utils
import com.example.third.databinding.ActivityRegisterEmailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private companion object{
        private const val TAG = "REGISTER_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

        binding.haveAccountTv.setOnClickListener{
            onBackPressed()
        }

        binding.registerBtn.setOnClickListener{
            validateData()
        }

    }

    private var email = ""
    private var password = ""
    private var cPassword = ""

    private fun validateData(){

        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        cPassword = binding.cPasswordEt.text.toString().trim()

        Log.d(TAG, "validateData: email : $email")
        Log.d(TAG, "validateData: password : $password")
        Log.d(TAG, "validateData: confirm password : $cPassword")

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            binding.emailEt.error = "Invalid Email Password"
            binding.passwordEt.requestFocus()
        }
        else if (password.isEmpty()){

            binding.emailEt.error = "Enter Password"
            binding.passwordEt.requestFocus()
        }
        else if (cPassword.isEmpty()){

            binding.emailEt.error = "Enter Confirm Password"
            binding.passwordEt.requestFocus()
        }
        else if (password != cPassword){

            binding.emailEt.error = "Password doesn't match"
            binding.passwordEt.requestFocus()
        }
        else{
            registerUser()
        }
    }

    private fun registerUser(){
        Log.d(TAG, "registerUser: ")

        progressDialog.setMessage("Creating Account")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "registerUser: Register Success")
                updateUserInfo()
            }
            .addOnFailureListener{ e ->
                Log.e(TAG, "registerUser: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to create account due to ${e.message}")
            }
    }

    private fun updateUserInfo(){
        Log.d(TAG, "updateUserInfo")

        progressDialog.setMessage("Saving User Info")

        val timestamp = Utils.getTimestamp()
        val registeredUserEmail = firebaseAuth.currentUser!!.email
        val  registeredUserUid = firebaseAuth.uid

        val hashMap = HashMap<String, Any>()
        hashMap["name"] = ""
        hashMap["phoneCode"] = ""
        hashMap["phoneNumber"] = ""
        hashMap["profileImageUrl"] = ""
        hashMap["dob"] = ""
        hashMap["userType"] = "Email"
        hashMap["typingTo"] = ""
        hashMap["timestamp"] = timestamp
        hashMap["onlineStatus"] = true
        hashMap["email"] = "$registeredUserEmail"
        hashMap["uid"] = "$registeredUserUid"

        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(registeredUserUid!!)
            .setValue(hashMap)
            .addOnSuccessListener{

                Log.d(TAG, "UpdateUserInfo: User registered...")
                progressDialog.dismiss()

                startActivity((Intent(this, MainActivity::class.java)))
                finishAffinity()
            }
            .addOnFailureListener{ e ->

                Log.e(TAG, "updateUserInfo: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to save user info due to ${e.message}")

            }
    }

}