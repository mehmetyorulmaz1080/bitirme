package com.example.third.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.third.R
import com.example.third.Utils
import com.example.third.adapters.AdapterAd
import com.example.third.databinding.ActivityAdSellerProfileBinding
import com.example.third.models.ModelAd
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdSellerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdSellerProfileBinding

    private companion object{

        private const val TAG = "SELLER_PROFILE_TAG"
    }

    private var sellerUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdSellerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sellerUid = intent.getStringExtra("sellerUid").toString()
        Log.d(TAG, "onCreate: sellerUid: $sellerUid")

        yükleSatıcıDetayları()
        ilanlarıYükle()

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

    }

    private fun yükleSatıcıDetayları() {
        Log.d(TAG,"yükleSatıcıDetayları: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(sellerUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    val name = "${snapshot.child("name").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                    val timestamp = snapshot.child("timestamp").value as Long

                    val formattedDate = Utils.formatZamanDamgasiTarih(timestamp)

                    binding.sellerNameTv.text = name
                    binding.sellerMemberSinceIv.text = formattedDate
                    try {
                        Glide.with(this@AdSellerProfileActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.sellerProfileIv)
                    }catch (e: Exception){
                        Log.d(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun ilanlarıYükle(){
        Log.d(TAG, "ilanlarıYükle: ")

        val adArraylist:ArrayList<ModelAd> = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.orderByChild("uid").equalTo(sellerUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    adArraylist.clear()

                    for(ds in snapshot.children) {
                        try {

                            val modelAd = ds.getValue(ModelAd::class.java)

                            adArraylist.add(modelAd!!)
                        }catch (e: java.lang.Exception){
                            Log.d(TAG, "onDataChange: ", e)

                        }
                    }

                    val adapterAd = AdapterAd(this@AdSellerProfileActivity, adArraylist)
                    binding.adsRv.adapter = adapterAd

                    val adsCount = "${adArraylist.size}"
                    binding.publishedAdsCountTv.text = adsCount
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}
