package com.example.third.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest
import com.example.third.fragments.HomeFragment
import com.example.third.R
import com.example.third.Utils
import com.example.third.databinding.ActivityMainBinding
import com.example.third.fragments.AccountFragment
import com.example.third.fragments.ChatsFragment
import com.example.third.fragments.MyAdsFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private companion object{
        const val TAG = "MAIN_TAG"
    }

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser == null){
            starLoginOptions()
        } else {

            updateFcmToken()
            askNotificationPermission()
        }
        showHomeFragment()

        binding.bottomNv.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {

                    showHomeFragment()

                    true
                }

                R.id.menu_chats -> {

                    if(firebaseAuth.currentUser == null){
                        Utils.toast(this, "Login Required")
                        starLoginOptions()

                        false
                    }
                    else{
                        showChatsFragment()
                        true
                    }
                }

                R.id.menu_my_ads -> {
                    if(firebaseAuth.currentUser == null){
                        Utils.toast(this, "Login Required")
                        starLoginOptions()

                        false
                    }
                    else{
                        showMyAdsFragment()
                        true
                    }
                }

                R.id.menu_account -> {
                    if(firebaseAuth.currentUser == null){
                        Utils.toast(this, "Login Required")
                        starLoginOptions()

                        false
                    }
                    else{
                        showAccountFragment()
                        true
                    }
                }

                else -> {
                    false
                }

            }
        }
        binding.sellFab.setOnClickListener {
            val intent = Intent(this, AdCreateActivity::class.java)
            intent.putExtra("isEditMode",false)
            startActivity(intent)
        }
    }
    private fun showHomeFragment() {
        binding.toolbarTitleTv.text = "Home"

        val fragment = HomeFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "HomeFragment")
        fragmentTransaction.commit()
    }

    private fun showChatsFragment() {
        binding.toolbarTitleTv.text = "Chats"

        val fragment = ChatsFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "ChatsFragment")
        fragmentTransaction.commit()
    }

    private fun showMyAdsFragment() {
        binding.toolbarTitleTv.text = "My Ads"

        val fragment = MyAdsFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "MyAdsFragment")
        fragmentTransaction.commit()

    }

    private fun showAccountFragment() {
        binding.toolbarTitleTv.text = "Account"

        val fragment = AccountFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "AccountFragment")
        fragmentTransaction.commit()

    }

    private fun starLoginOptions() {
        val intent = Intent(this, LoginOptionsActivity::class.java)
        startActivity(intent)

    }

    private fun updateFcmToken(){
        val myUid = "${firebaseAuth.uid}"
        Log.d(TAG,"updateFcmToken: ")

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->

                Log.d(TAG, "updateFcmToken: fcmToken $fcmToken")
                val hashMap = HashMap<String, Any>()
                hashMap["fcmToken"] = "$fcmToken"

                val ref = FirebaseDatabase.getInstance().getReference("Users")
                ref.child(myUid)
                    .updateChildren(hashMap)
                    .addOnSuccessListener {

                        Log.d(TAG, "updateFcmToken: FCM Token Update to db success")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "updateFcmToken: ", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "updateFcmToken: ", e)
            }
    }

    private fun askNotificationPermission(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_DENIED){
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted ->

        Log.d(TAG,"requestNotificationPermission: isGranted: $isGranted")
    }

}