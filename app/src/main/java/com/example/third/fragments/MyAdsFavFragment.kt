package com.example.third.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.third.adapters.AdapterAd
import com.example.third.databinding.FragmentMyAdsFavBinding
import com.example.third.models.ModelAd
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MyAdsFavFragment : Fragment() {
    private lateinit var binding: FragmentMyAdsFavBinding

    companion object{
        const val TAG = "FAV_ADS_TAG"
    }

    private lateinit var mContext: Context

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var adArraylist: ArrayList<ModelAd>

    private lateinit var adapterAd: AdapterAd

    override fun onAttach(context: Context) {

        this.mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = FragmentMyAdsFavBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        ilanlarıYükle()

        binding.searchEt.addTextChangedListener(object: TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val query = s.toString()
                    adapterAd.filter.filter(query)
                }catch (e: Exception) {
                    Log.e(TAG, "onTextChanged: ", e)
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

    }

    private fun ilanlarıYükle() {
        Log.d(TAG, "ilanlarıYükle")

        adArraylist = ArrayList()

        val favRef = FirebaseDatabase.getInstance().getReference("Users")
        favRef.child(firebaseAuth.uid!!).child("Favorites")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    adArraylist.clear()
                    for (ds in snapshot.children) {
                        val adId = "${ds.child("adId").value}"
                        Log.d(TAG, "onDataChange: AdId: $adId")

                        val adRef = FirebaseDatabase.getInstance().getReference("Ads")
                        adRef.child(adId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {

                                    try {
                                        val modelAd = snapshot.getValue(ModelAd::class.java)

                                        adArraylist.add(modelAd!!)

                                    } catch (e: Exception) {
                                        Log.e(TAG, "onDataChange: ", e)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {

                                }

                            })

                    }
                    Handler().postDelayed({
                        adapterAd = AdapterAd(mContext, adArraylist)
                        binding.adsRv.adapter = adapterAd
                    }, 500)
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}