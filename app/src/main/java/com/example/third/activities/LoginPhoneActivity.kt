package com.example.third.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.third.Utils
import com.example.third.databinding.ActivityLoginPhoneBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class LoginPhoneActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPhoneBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth
    private var forceRefreshingToken: ForceResendingToken? = null
    private lateinit var mCallbacks: OnVerificationStateChangedCallbacks
    private var mVerificationId: String? = null

    private companion object{
        private const val TAG = "PHONE_LOGIN_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.phoneInputRl.visibility = View.VISIBLE
        binding.otpInputRl.visibility = View.GONE

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Lütfen Bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()

        telefonGirişGeriAramalar()

        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.sendOtpBtn.setOnClickListener {
            doğrulamaVerileri()
        }

        binding.verifyOtpBtn.setOnClickListener {
            val otp = binding.otpEt.text.toString().trim()
            Log.d(TAG, "onCreate: otp: $otp")
            if(otp.isEmpty()){
                binding.otpEt.error = "Kod girin"
                binding.otpEt.requestFocus()
            }else if(otp.length < 6){
                binding.otpEt.error = "Kod uzunluğu 6 karakter uzunluğunda olmalıdır"
                binding.otpEt.requestFocus()
            }else{
                telefonNumarasınıKodlaDoğrula(mVerificationId, otp)
            }


        }

        binding.resendOtpTv.setOnClickListener {

            doğrulamaKodunuYenidenGönder(forceRefreshingToken)

        }

    }

    private var phoneCode = ""
    private var phoneNumber = ""
    private var phoneNumberWithCode = ""

    private fun doğrulamaVerileri(){

        phoneCode = binding.phoneCodeTil.selectedCountryCodeWithPlus
        phoneNumber = binding.phoneNumberEt.text.toString().trim()
        phoneNumberWithCode = phoneCode + phoneNumber

        Log.d(TAG, "doğrulamaVerileri: phoneCode: $phoneCode")
        Log.d(TAG, "doğrulamaVerileri: phoneNumber: $phoneNumber")
        Log.d(TAG, "doğrulamaVerileri: phoneNumberWithCode: $phoneNumberWithCode")

        if(phoneNumber.isEmpty()){
           binding.phoneNumberEt.error = "Telefon numaranızı girin"
           binding.phoneNumberEt.requestFocus()
        }else{
            telefonNumarasıDoğrulamayıBaşlat()
        }
    }

    private fun telefonNumarasıDoğrulamayıBaşlat(){
        Log.d(TAG, "telefonNumarasıDoğrulamayıBaşlat: ")
        progressDialog.setMessage("Kod gönderiliyor $phoneNumberWithCode")
        progressDialog.show()
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumberWithCode)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun telefonGirişGeriAramalar(){
        Log.d(TAG, "telefonGirişGeriAramalar: ")

        mCallbacks = object: OnVerificationStateChangedCallbacks(){

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: ")

                telefonKimlikDoğrulamaBilgileriyleOturumAçın(credential)

            }

            

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ", e)

                progressDialog.dismiss()

                Utils.toast(this@LoginPhoneActivity, "${e.message}")

            }

            override fun onCodeSent(verificationId: String, token: ForceResendingToken) {
                Log.d(TAG, "onCodeSent: $verificationId")
                mVerificationId = verificationId
                forceRefreshingToken = token

                progressDialog.dismiss()

                binding.phoneInputRl.visibility = View.GONE
                binding.otpInputRl.visibility = View.VISIBLE

                Utils.toast(this@LoginPhoneActivity, "Kod şuraya gönderiliyor:$phoneNumberWithCode")

                binding.loginLabelTv.text = "Lütfen gönderdiğimiz doğrulama kodunu yazın $phoneNumberWithCode"
            }

            override fun onCodeAutoRetrievalTimeOut(p0: String) {
                super.onCodeAutoRetrievalTimeOut(p0)
            }
        }
    }

    private fun telefonNumarasınıKodlaDoğrula(verificationId: String?, otp: String) {
        Log.d(TAG, "telefonNumarasınıKodlaDoğrula: verificationId: $verificationId")
        Log.d(TAG, "telefonNumarasınıKodlaDoğrula: otp: $otp")

        progressDialog.setMessage("Verifying OTP")
        progressDialog.show()

        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        telefonKimlikDoğrulamaBilgileriyleOturumAçın(credential)
    }

    private fun doğrulamaKodunuYenidenGönder(token: ForceResendingToken?){
        Log.d(TAG, "doğrulamaKodunuYenidenGönder: ")

        progressDialog.setMessage("Kod yeniden gönderiliyor $phoneNumberWithCode")
        progressDialog.show()
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumberWithCode)
            .setTimeout(60L,TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }

    private fun telefonKimlikDoğrulamaBilgileriyleOturumAçın(credential: PhoneAuthCredential) {

        Log.d(TAG, "telefonKimlikDoğrulamaBilgileriyleOturumAçın")

        progressDialog.setMessage("Logging In")
        progressDialog.show()

        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                Log.d(TAG, "telefonKimlikDoğrulamaBilgileriyleOturumAçın: Başarı")
                if(authResult.additionalUserInfo!!.isNewUser){
                    Log.d(TAG, "telefonKimlikDoğrulamaBilgileriyleOturumAçın: Yeni kullanıcı, Hesap Oluşturuldu")
                    kullanıcıBilgisiVeritabanınıGüncelle()
                }else{
                    Log.d(TAG, "telefonKimlikDoğrulamaBilgileriyleOturumAçın: Mevcut kullanıcı, Giriş yapıldı")
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }

                }
            .addOnFailureListener { e ->
                Log.e(TAG, "telefonKimlikDoğrulamaBilgileriyleOturumAçın: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Şu nedenden dolayı giriş yapılamadı: ${e.message}")
            }
    }

    private fun kullanıcıBilgisiVeritabanınıGüncelle(){
        Log.d(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: ")
        progressDialog.setMessage("Kullanıc bilgileri kaydediliyor...")
        progressDialog.show()

        val timestamp = Utils.getTimestamp()
        val registeredUserUid = firebaseAuth.uid

        val hashMap = HashMap<String, Any?>()

        hashMap["name"] = ""
        hashMap["phoneCode"] = "$phoneCode"
        hashMap["phoneNumber"] = "$phoneNumber"
        hashMap["profileImageUrl"] = ""
        hashMap["dob"] = ""
        hashMap["userType"] = "Phone"
        hashMap["typingTo"] = ""
        hashMap["timestamp"] = timestamp
        hashMap["onlineStatus"] = true
        hashMap["email"] = ""
        hashMap["uid"] = registeredUserUid

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(registeredUserUid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: Kullanıcı bilgileri kaydedildi...")
                progressDialog.dismiss()

                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Şu nedenden dolayı giriş yapılamadı: ${e.message}")
            }


    }
}