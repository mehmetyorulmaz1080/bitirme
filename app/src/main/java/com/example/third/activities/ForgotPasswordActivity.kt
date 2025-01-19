package com.example.third.activities

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.third.Utils
import com.example.third.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth

    private companion object{
        private const val TAG = "FORGOT PASSWORD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

        binding.submitBtn.setOnClickListener{
            validateData()
        }
    }

    private var email = ""

    private fun validateData(){
        email = binding.emailEt.text.toString().trim()

        Log.d(TAG, "validateData: email: $email")

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            binding.emailEt.error = "Invalid Email Pattern!"
            binding.emailEt.requestFocus()
        }
        else{
            ŞifreKurtarmaTalimatlarınıGönder()
        }
    }

    private fun ŞifreKurtarmaTalimatlarınıGönder(){
        Log.d(TAG,"ŞifreKurtarmaTalimatlarınıGönder: ")

        progressDialog.setMessage("şifre sıfırlama talimatlarını gönderiliyor $email")
        progressDialog.show()

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Utils.toast(this, "Şifre sıfırlama talimatları şu adrese gönderildi: $email")
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e(TAG, "ŞifreKurtarmaTalimatlarınıGönder: ", e)
                Utils.toast(this, "Nedeniyle gönderilemedi ${e.message}")
            }
    }
}
