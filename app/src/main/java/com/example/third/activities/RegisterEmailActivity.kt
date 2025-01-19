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
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

        binding.haveAccountTv.setOnClickListener{
            onBackPressed()
        }

        binding.registerBtn.setOnClickListener{
            doğrulamaVerileri()
        }

    }

    private var email = ""
    private var password = ""
    private var cPassword = ""

    private fun doğrulamaVerileri(){

        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        cPassword = binding.cPasswordEt.text.toString().trim()

        Log.d(TAG, "doğrulamaVerileri: email : $email")
        Log.d(TAG, "doğrulamaVerileri: password : $password")
        Log.d(TAG, "doğrulamaVerileri: confirm password : $cPassword")

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            binding.emailEt.error = "Invalid Email Password"
            binding.passwordEt.requestFocus()
        }
        else if (password.isEmpty()){

            binding.emailEt.error = "Şifre girin"
            binding.passwordEt.requestFocus()
        }
        else if (cPassword.isEmpty()){

            binding.emailEt.error = "Mevcut şifrenizi girin"
            binding.passwordEt.requestFocus()
        }
        else if (password != cPassword){

            binding.emailEt.error = "Şifreler eşleşmiyor"
            binding.passwordEt.requestFocus()
        }
        else{
            kullanıcıyıKaydet()
        }
    }

    private fun kullanıcıyıKaydet(){
        Log.d(TAG, "kullanıcıyıKaydet: ")

        progressDialog.setMessage("Hesap Oluşturma")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "kullanıcıyıKaydet: Kayıt olma Başarılı ")
                güncellemeKullanıcıBilgileri()
            }
            .addOnFailureListener{ e ->
                Log.e(TAG, "kullanıcıyıKaydet: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Şu nedenlerden dolayı kayıt olamadı: ${e.message}")
            }
    }

    private fun güncellemeKullanıcıBilgileri(){
        Log.d(TAG, "güncellemeKullanıcıBilgileri")

        progressDialog.setMessage("Kullanıcı Bilgileri Kaydediliyor...")

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

                Log.d(TAG, "güncellemeKullanıcıBilgileri: Kullanıcı bilgileri kaydedildi...")
                progressDialog.dismiss()

                startActivity((Intent(this, MainActivity::class.java)))
                finishAffinity()
            }
            .addOnFailureListener{ e ->

                Log.e(TAG, "güncellemeKullanıcıBilgileri: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Şu nedenlerden dolayı kullanıcı bilgileri kaydedilemedi: ${e.message}")

            }
    }

}