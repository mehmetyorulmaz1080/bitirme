package com.example.third.activities

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.third.Utils
import com.example.third.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseUser: FirebaseUser

    private companion object{
        private const val TAG = "CHANGE_PASSWORD_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

        binding.submitBtn.setOnClickListener{
            doğrulamaVerileri()
        }

    }

    private var currentPassword = ""
    private var newPassword = ""
    private var confirmNewPassword = ""

    private fun doğrulamaVerileri(){

        currentPassword = binding.currentPasswordEt.text.toString().trim()
        newPassword = binding.newPasswordEt.text.toString().trim()
        confirmNewPassword = binding.confirmNewPasswordEt.text.toString().trim()

        Log.d(TAG, "doğrulamaVerileri: Mevcut Şifre: $currentPassword")
        Log.d(TAG, "doğrulamaVerileri: Yeni Şifre: $newPassword")
        Log.d(TAG, "doğrulamaVerileri: Yeni Şifreyi onayla: $confirmNewPassword")

        if(currentPassword.isEmpty()){
            binding.currentPasswordEt.error = "Mevcut şifreyi girin!"
            binding.currentPasswordEt.requestFocus()
        }
        else if(newPassword.isEmpty()){
            binding.newPasswordEt.error = "Yeni şifreyi girin!"
            binding.newPasswordEt.requestFocus()
            }
        else if(confirmNewPassword.isEmpty()){
            binding.confirmNewPasswordEt.error = "Yeni şifreyi girin ve onaylayın!"
            binding.confirmNewPasswordEt.requestFocus()

        }
        else if(newPassword != confirmNewPassword) {
            binding.confirmNewPasswordEt.error = "Şifre eşleşmiyor!"
            binding.confirmNewPasswordEt.requestFocus()
        }
        else{
            güncellemeŞifresiİçinKullanıcınınKimliğiniDoğrula()
        }

    }

    private fun güncellemeŞifresiİçinKullanıcınınKimliğiniDoğrula(){

        progressDialog.setMessage("Kullanıcının kimliği doğrulanıyor...")
        progressDialog.show()

        val authCredential = EmailAuthProvider.getCredential(firebaseUser.email.toString(), currentPassword)
        firebaseUser.reauthenticate(authCredential)
            .addOnSuccessListener {
                Log.d(TAG, "güncellemeŞifresiİçinKullanıcınınKimliğiniDoğrula: Authenticated...")
                güncellemeŞifre()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "güncellemeŞifresiİçinKullanıcınınKimliğiniDoğrula: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Nedeniyle kimlik doğrulaması yapılamadı ${e.message}")
            }
    }

    private fun güncellemeŞifre(){
        progressDialog.setMessage("Şifre güncelleniyor...")
        progressDialog.show()

        firebaseUser.updatePassword(newPassword)
            .addOnSuccessListener {
                Log.d(TAG, "güncellemeŞifre: Şifre güncellendi...")
                progressDialog.dismiss()
                Utils.toast(this, "Şifre güncellendi...")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "güncellemeŞifre: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Nedeniyle güncellenemedi ${e.message}")
            }

    }
}