package com.example.third.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.third.FilterAd
import com.example.third.R
import com.example.third.Utils
import com.example.third.activities.AdDetailsActivity
import com.example.third.databinding.RowAdBinding
import com.example.third.models.ModelAd
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterAd : RecyclerView.Adapter<AdapterAd.HolderAd>, Filterable {

    private lateinit var binding: RowAdBinding

    private companion object{
        private const val TAG = "ADAPTER_AD_TAG"
    }

    private  var context: Context
    var adArrylist: ArrayList<ModelAd>
    private var filterList: ArrayList<ModelAd>

    private var filter: FilterAd? = null

    private var firebaseAuth: FirebaseAuth

    constructor(context: Context, adArrylist: ArrayList<ModelAd>) {
        this.context = context
        this.adArrylist = adArrylist
        this.filterList = adArrylist

        firebaseAuth = FirebaseAuth.getInstance()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAd {
        binding = RowAdBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderAd(binding.root)
    }

    override fun onBindViewHolder(holder: HolderAd, position: Int) {

        val modelAd = adArrylist[position]

        val title = modelAd.title
        val description = modelAd.description
        val condition = modelAd.condition
        val address = modelAd.address
        val price = modelAd.price
        val timestamp = modelAd.timestamp
        val formattedData = Utils.formatTimestampDate(timestamp)
        
        loadAdFirstImage(modelAd, holder)
        
        if (firebaseAuth.currentUser != null) {
            checkIsFavorite(modelAd, holder)
        }

        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.conditionTv.text = condition
        holder.addressTv.text = address
        holder.priceTv.text = price
        holder.dateTv.text = formattedData

        holder.itemView.setOnClickListener{
            val intent = Intent(context, AdDetailsActivity::class.java)
            intent.putExtra("adId", modelAd.id)
            context.startActivity(intent)
        }

        holder.favBtn.setOnClickListener{

            val favorite = modelAd.favorite
            if (favorite) {
                Utils.removeFromFavorite(context, modelAd.id)
            } else {
                Utils.addToFavorite(context, modelAd.id)
            }
        }

    }

    private fun checkIsFavorite(modelAd: ModelAd, holder: HolderAd) {

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(modelAd.id)
            .addValueEventListener(object: ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val favorite = snapshot.exists()

                    modelAd.favorite = favorite

                    if (favorite){
                        holder.favBtn.setImageResource(R.drawable.ic_fav_yes)
                    }else{
                        holder.favBtn.setImageResource(R.drawable.ic_fav_no)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

    }

    private fun loadAdFirstImage(modelAd: ModelAd, holder: HolderAd) {

        val adId = modelAd.id

        Log.d(TAG, "loadAdFirstImage: AdId: $adId")
        val reference = FirebaseDatabase.getInstance().getReference("Ads")
        reference.child(adId).child("Images").limitToFirst(1)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {
                        val imageUrl = "${ds.child("imageUrl").value}"
                        Log.d(TAG, "onDataChange: ImageUrl: $imageUrl")

                        try {
                            Glide.with(context)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_image_gray)
                                .into(holder.imageIv)
                        }
                        catch (e: Exception){
                            Log.e(TAG, "onDataChange: ", e)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
    override fun getItemCount(): Int {

        return adArrylist.size
    }

    override fun getFilter(): android.widget.Filter {
        if (filter == null) {
            filter = FilterAd(this, filterList)
        }
        return filter as FilterAd
    }

    inner class HolderAd(itemView: View): RecyclerView.ViewHolder(itemView){

        var imageIv = binding.imageIv
        var titleTv = binding.titleTv
        var descriptionTv = binding.descriptionTv
        var favBtn = binding.favBtn
        var addressTv = binding.addressTv
        var conditionTv = binding.conditionTv
        var priceTv = binding.priceTv
        var dateTv = binding.dateTv

    }


}