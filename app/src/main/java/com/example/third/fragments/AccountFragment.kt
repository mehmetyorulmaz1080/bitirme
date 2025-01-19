package com.example.third.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.third.activities.ChangePasswordActivity
import com.example.third.activities.DeleteAccountActivity
import com.example.third.activities.LoginOptionsActivity
import com.example.third.activities.ProfilEditActivity
import com.example.third.R
import com.example.third.Utils
import com.example.third.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class AccountFragment : Fragment() {
    private lateinit var binding: FragmentAccountBinding
    private lateinit var mContext: Context
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private companion object{
        private const val TAG = "ACCOUNT_TAG"
    }

    override fun onAttach(context: Context){

        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAccountBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressDialog = ProgressDialog(mContext)
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()

        bilgilerimiYükle()

        binding.logoutCv.setOnClickListener{
            firebaseAuth.signOut()
            val intent = Intent(mContext, LoginOptionsActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.editProfileCv.setOnClickListener{
            val intent = Intent(mContext, ProfilEditActivity::class.java)
            startActivity(intent)
        }

        binding.changePasswordCv.setOnClickListener{
            val intent = Intent(mContext, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        binding.verifyAccountCv.setOnClickListener{
            hesabıDoğrula()
        }

        binding.deleteAccountCv.setOnClickListener{
            val intent = Intent(mContext, DeleteAccountActivity::class.java)
            startActivity(intent)
        }
    }

    private fun bilgilerimiYükle(){

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dob = "${snapshot.child("dob").value}"
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val phoneCode = "${snapshot.child("phoneCode").value}"
                    val phoneNumber = "${snapshot.child("phoneNumber").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                    var timestamp = "${snapshot.child("timestamp").value}"
                    val userType = "${snapshot.child("userType").value}"

                    val phone = phoneCode + phoneNumber

                    if (timestamp == "null"){
                        timestamp = "0"
                    }

                    val formattedDate = Utils.formatZamanDamgasiTarih(timestamp.toLong())

                    binding.nameTv.text = name
                    binding.emailTv.text = email
                    binding.phoneTv.text = phone
                    binding.dobTv.text = dob
                    binding.memberSinceTv.text = formattedDate

                    if (userType == "Email"){

                        val isVerified = firebaseAuth.currentUser!!.isEmailVerified
                        if (isVerified){
                            binding.verifyAccountCv.visibility = View.GONE
                            binding.verificationTv.text = "Doğrulandı"
                        }
                        else{
                            binding.verifyAccountCv.visibility = View.VISIBLE
                            binding.verificationTv.text = "Doğrulanmadı"
                    }
                }
                    else{
                        binding.verifyAccountCv.visibility = View.GONE
                        binding.verificationTv.text = "Doğrulandı"
                    }
                    try {
                        Glide.with(mContext)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.profileIv)
                    }
                    catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun hesabıDoğrula(){
        Log.d(TAG, "hesabıDoğrula: hesap doğrulanıyor...")
        progressDialog.setMessage("...")
        progressDialog.show()

        firebaseAuth.currentUser!!.sendEmailVerification()
            .addOnSuccessListener {
                Log.d(TAG, "hesabıDoğrula: Başarıyla e-postaya gönderildi...")
                progressDialog.dismiss()
                Utils.toast(mContext, "Hesap doğrulama talimatları e-postanıza gönderiliyor...")

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "hesabıDoğrula: ", e)
                progressDialog.dismiss()
                Utils.toast(mContext, "Nedeniyle gönderilemedi ${e.message}")

            }

    }
}