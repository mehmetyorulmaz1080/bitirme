package com.example.third.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import com.example.third.Utils
import com.example.third.databinding.ActivityLoginEmailBinding
import com.google.firebase.auth.FirebaseAuth

class LoginEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginEmailBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private companion object{
        private const val TAG = "LOGIN_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

        binding.noAccountTv.setOnClickListener{
            startActivity(Intent(this, RegisterEmailActivity::class.java))
        }
        binding.forgotPasswordTv.setOnClickListener{
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.loginBtn.setOnClickListener{
            validateData()
        }
    }

    private var email = ""
    private var password = ""

    private fun validateData(){
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()

        Log.d(TAG, "validateData: email: $email")
        Log.d(TAG, "validateData: password: $password")

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            binding.passwordEt.error = "Invalid Email Format"
            binding.passwordEt.requestFocus()
        }
        else if (password.isEmpty()){
            binding.passwordEt.error = "Enter Password"
            binding.passwordEt.requestFocus()
        }
        else{
            loginUser()
        }
    }

    private fun loginUser(){
        Log.d(TAG, "loginUSer: ")

        progressDialog.setMessage("Logging In...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Login User: Logged In...")
                progressDialog.dismiss()

                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener() { e ->
                Log.e(TAG, "LoginUser: " , e)
                progressDialog.dismiss()

                Utils.toast(this, "Unable to login to due to ${e.message}")
            }
    }
}