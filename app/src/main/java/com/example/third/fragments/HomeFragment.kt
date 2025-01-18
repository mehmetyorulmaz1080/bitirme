package com.example.third.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.example.third.RvListenerCategory
import com.example.third.Utils
import com.example.third.activities.LocationPickerActivity
import com.example.third.adapters.AdapterAd
import com.example.third.adapters.AdapterCategory
import com.example.third.databinding.FragmentHomeBinding
import com.example.third.models.ModelAd
import com.example.third.models.ModelCategory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mContext: Context

    private companion object{
        const val TAG = "HOME_TAG"

        private const val MAX_DISTANCE_TO_lOAD_ADS_KM = 10
    }

    private lateinit var adArrylist: ArrayList<ModelAd>

    private lateinit var adapterAd: AdapterAd

    private lateinit var locationSp: SharedPreferences

    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAddress = ""

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(LayoutInflater.from(mContext), container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationSp = mContext.getSharedPreferences("LOCATION_SP", Context.MODE_PRIVATE)

        currentLatitude = locationSp.getFloat("CURRENT_LATITUDE", 0.0F).toDouble()
        currentLongitude = locationSp.getFloat("CURRENT_LONGITUDE", 0.0F).toDouble()
        currentAddress = locationSp.getString("CURRENT_ADDRESS", "")!!

        if (currentLatitude != 0.0 || currentLongitude != 0.0) {
            binding.locationTv.text = currentAddress
        }
        loadCategories()

        loadAds("Hepsi")

        binding.searchEt.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val query = s.toString()
                    adapterAd.filter.filter(s)
                } catch (e: Exception) {
                    Log.e(TAG, "onTextChanged: ", e)
                }
            }
            override fun afterTextChanged(p0: Editable?) {

            }
        })
        binding.locationCv.setOnClickListener{
            val intent = Intent(mContext, LocationPickerActivity::class.java)
            startActivity(intent)

        }

    }

    private val locationPickerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if (result.resultCode == Activity.RESULT_OK){
            Log.d(TAG, "locationPickerActivityResultLauncher: RESULT OK")
            val data = result.data

            if (data != null) {
                Log.d(TAG, "locationPickerActivityResultLauncher: Location Picked!")
                currentLatitude = data.getDoubleExtra("LATITUDE", 0.0)
                currentLongitude = data.getDoubleExtra("LONGITUDE", 0.0)
                currentAddress = data.getStringExtra("ADDRESS").toString()

                locationSp.edit()
                    .putFloat("CURRENT_LATITUDE", currentLatitude.toFloat())
                    .putFloat("CURRENT_LONGITUDE", currentLongitude.toFloat())
                    .putString("CURRENT_ADDRESS", currentAddress)
                    .apply()

                binding.locationTv.text = currentAddress

                loadAds("Hepsi")

            }
        }else{
            Utils.toast(mContext, "İşlem iptal edildi!")
        }
    }
    private fun loadAds(category: String) {
        Log.d(TAG, "loadAds: Category: $category")

        adArrylist = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                adArrylist.clear()
                for (ds in snapshot.children) {

                    try {
                        val modelAd = ds.getValue(ModelAd::class.java)
                        val distance = calculateDistanceKm(
                            modelAd?.latitude ?: 0.0,
                            modelAd?.longitude ?: 0.0
                        )
                        Log.d(TAG,"onDataChange: Distance: $distance")

                        if (category == "Hepsi"){
                            if (distance <= MAX_DISTANCE_TO_lOAD_ADS_KM){
                                adArrylist.add(modelAd!!)
                            }
                        }else{
                            if(modelAd!!.category.equals(category) ){
                                if (distance <= MAX_DISTANCE_TO_lOAD_ADS_KM){
                                    adArrylist.add(modelAd!!)
                                }
                            }
                        }
                    }catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }
                adapterAd = AdapterAd(mContext, adArrylist)
                binding.adsRv.adapter = adapterAd

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

    }
    private fun calculateDistanceKm(adlatitude: Double, adLongitude: Double): Double {
        Log.d(TAG, "calculateDistanceKm: currentLatitude: $currentLatitude")
        Log.d(TAG, "calculateDistanceKm: currentLongitude: $currentLongitude")
        Log.d(TAG, "calculateDistanceKm: Ad Latitude: $adlatitude")
        Log.d(TAG, "calculateDistanceKm: Ad Longitude: $adLongitude")

        val startPoint = android.location.Location(LocationManager.NETWORK_PROVIDER)
        startPoint.latitude = currentLatitude
        startPoint.longitude = currentLongitude

        val endPoint = android.location.Location(LocationManager.NETWORK_PROVIDER)
        endPoint.latitude = adlatitude
        endPoint.longitude = adLongitude

        val distanceInMeters = startPoint.distanceTo(endPoint).toDouble()

        return distanceInMeters / 1000.0

    }


    private fun loadCategories(){

        val categoryArrayList = ArrayList<ModelCategory>()

        for (i in 0 until  Utils.categories.size){
            val modelCategory = ModelCategory(Utils.categories[i], Utils.categoryIcons[i])
            categoryArrayList.add(modelCategory)
        }

        val adapterCategory = AdapterCategory(mContext, categoryArrayList, object:
            RvListenerCategory {
            override fun onCategoryClick(modelCategory: ModelCategory) {

                val selectedCategory = modelCategory.category
                loadAds(selectedCategory)
            }
        })

        binding.categoriesRv.adapter = adapterCategory

    }
}