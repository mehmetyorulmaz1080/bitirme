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
        progressDialog.setTitle("Lütfen bekleyin...")
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
            doğrulamaVerileri()
        }
    }

    private var email = ""
    private var password = ""

    private fun doğrulamaVerileri(){
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()

        Log.d(TAG, "doğrulamaVerileri: email: $email")
        Log.d(TAG, "doğrulamaVerileri: password: $password")

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            binding.passwordEt.error = "Geçersiz E-posta Formatı"
            binding.passwordEt.requestFocus()
        }
        else if (password.isEmpty()){
            binding.passwordEt.error = "Şifreyi Girin"
            binding.passwordEt.requestFocus()
        }
        else{
            kullanıcıOturumuAç()
        }
    }

    private fun kullanıcıOturumuAç(){
        Log.d(TAG, "kullanıcıOturumuAç: ")

        progressDialog.setMessage("Giriş Yap...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Kullanıcı Girişi: Giriş Yapıldı...")
                progressDialog.dismiss()

                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener() { e ->
                Log.e(TAG, "kullanıcıOturumuAç: " , e)
                progressDialog.dismiss()

                Utils.toast(this, "Nedeniyle oturum açılamıyor ${e.message}")
            }
    }
}