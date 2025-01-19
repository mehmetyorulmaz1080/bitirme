package com.example.third.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.third.R
import com.example.third.Utils
import com.example.third.databinding.ActivityLoginOptionsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginOptionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginOptionsBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private companion object{
        private const val TAG = "LOGIN_OPTIONS_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)



        binding.loginEmailBtn.setOnClickListener {
            startActivity(Intent(this, LoginEmailActivity::class.java))
        }

        binding.loginGoogleBtn.setOnClickListener {
            googleGirişiniBaşlat()
        }

        binding.loginPhoneBtn.setOnClickListener{
            val intent = Intent(this, LoginPhoneActivity::class.java)
            startActivity(intent)
        }

    }

    private fun googleGirişiniBaşlat(){
        Log.d(TAG, "googleGirişiniBaşlat:")

        val googleSignInIntent = mGoogleSignInClient.signInIntent
        googleOturumAç.launch(googleSignInIntent)
    }

    private val googleOturumAç = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        Log.d(TAG, "googleOturumAç: ")

        if (result.resultCode == RESULT_OK){

            val data = result.data

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "googleOturumAç: Account ID: ${account.id}")

                googleHesabıileFirebaseKimlikDoğrulaması(account.idToken)
            } catch (e: Exception) {
                Log.e(TAG, "googleOturumAç:", e)
                Utils.toast(this, "Error: ${e.message}")
            }

        }
        else{
            Utils.toast(this, "Cancelled...")
        }
    }
    private fun googleHesabıileFirebaseKimlikDoğrulaması(idToken: String?) {
        Log.d(TAG, "googleHesabıileFirebaseKimlikDoğrulaması: idToken: $idToken")

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->

                if (authResult.additionalUserInfo!!.isNewUser){
                    Log.d(TAG, "googleHesabıileFirebaseKimlikDoğrulaması: Yeni Kullanıcı, Hesap oluşturuldu...")

                    kullanıcıBilgisiVeritabanınıGüncelle()
                }
                else{
                    Log.d(TAG, "googleHesabıileFirebaseKimlikDoğrulaması: Mevcut Kullanıcı, Giriş Yapıldı...")
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "googleHesabıileFirebaseKimlikDoğrulaması: ", e)
                Utils.toast(this, "${e.message}")
            }
    }

    private fun kullanıcıBilgisiVeritabanınıGüncelle(){
        Log.d(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: ")

        progressDialog.setMessage("Kullanıcı Bilgilerini Kaydetme")
        progressDialog.show()

        val timestamp = Utils.getTimestamp()
        val registeredUserEmail = firebaseAuth.currentUser?.email
        val registeredUserUid = firebaseAuth.uid
        val name = firebaseAuth.currentUser?.displayName

        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = "$name"
        hashMap["phoneCode"] = ""
        hashMap["phoneNumber"] = ""
        hashMap["profileImageUrl"] = ""
        hashMap["dob"] = ""
        hashMap["userType"] = "Google"
        hashMap["typingTo"] = ""
        hashMap["timestamp"] = timestamp
        hashMap["onlineStatus"] = true
        hashMap["email"] = registeredUserEmail
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
                progressDialog.dismiss()
                Log.e(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: ", e)
                Utils.toast(this, "Nedeniyle kullanıcı bilgileri kaydedilemedi ${e.message}")
            }

    }

}
