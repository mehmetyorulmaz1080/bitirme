package com.example.third.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.third.Utils
import com.example.third.databinding.ActivityDeleteAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DeleteAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeleteAccountBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth
    private var firebaseUser: FirebaseUser? = null

    companion object{
        private const val TAG = "DELETE_ACCOUNT_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser

        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.submitBtn.setOnClickListener {
            hesabıSil()
        }


    }

    private fun hesabıSil(){
        Log.d((TAG), "hesabıSil: ")

        progressDialog.setMessage("Kullanıcı Hesabı Siliniyor...")
        progressDialog.show()

        val myUid = firebaseAuth.uid

        firebaseUser!!.delete()
            .addOnSuccessListener {
                Log.d(TAG, "hesabıSil: Hesap Silindi...")

                progressDialog.setMessage("Kullanıcı İlanları Siliniyor...")

                val refUserAds = FirebaseDatabase.getInstance().getReference("Ads")
                refUserAds.orderByChild("uid").equalTo(myUid!!)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapShot: DataSnapshot) {
                            for (ds in snapShot.children){
                                ds.ref.removeValue()
                            }
                           progressDialog.setMessage("Kullanıcı Verileri Siliniyor...")

                            val refUsers = FirebaseDatabase.getInstance().getReference("Users")
                            refUsers.child(myUid!!).removeValue()
                                .addOnSuccessListener {
                                    Log.d(TAG, "onDataChange: Kullanıcı Verileri Silindi...")
                                    progressDialog.dismiss()
                                    startMainActivity()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "onDataChange: ", e)
                                    progressDialog.dismiss()
                                    Utils.toast(
                                        this@DeleteAccountActivity,
                                        "Nedeniyle kullanıcı verileri silinemedi ${e.message}"
                                    )
                                    startMainActivity()
                                }

                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })
                }
            .addOnFailureListener { e->
                Log.e(TAG, "hesabıSil: Şu nedenlerden dolayı hesap silinemedi: ${e.message}")
                progressDialog.dismiss()
                Utils.toast(this, "Şu nedenlerden dolayı hesap silinemedi: ${e.message}")
            }
    }
    private fun startMainActivity(){
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }

}