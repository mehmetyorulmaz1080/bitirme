package com.example.third.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.third.R
import com.example.third.Utils
import com.example.third.databinding.RowImagesPickedBinding
import com.example.third.models.ModelImagePicked
import com.google.firebase.database.FirebaseDatabase

class AdapterImagePicked(
    private val context: Context,
    private val imagesPickedArrayList: ArrayList<ModelImagePicked>,
    private val adId: String
    ) : RecyclerView.Adapter<AdapterImagePicked.HolderImagePicked>(){

    private lateinit var binding: RowImagesPickedBinding

    private companion object{
        const val TAG = "IMAGES_PICKED"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImagePicked {

        binding = RowImagesPickedBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderImagePicked(binding.root)
    }

    override fun onBindViewHolder(holder: HolderImagePicked, position: Int) {

        val model = imagesPickedArrayList[position]


        if (model.fromInternet){

            try {

                val imageUrl = model.imageUrl
                Log.d(TAG, "onBindViewHolder: imageUrl: $imageUrl")
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_gray)
                    .into(holder.imageIv)
            }catch (e: Exception){
                Log.e(TAG, "onBindViewHolder: ", e)
            }
        }else{

            try {

                val imageUri = model.imageUri
                Log.d(TAG, "onBindViewHolder: imageUri: $imageUri")

                Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_image_gray)
                    .into(holder.imageIv)
            }
            catch (e: Exception){
                Log.e(TAG, "onBindViewHolder: ", e)
            }

        }



        holder.closeBtn.setOnClickListener {

            if (model.fromInternet){

                deleteImageFirebase(model, holder, position)

            } else{

                imagesPickedArrayList.remove(model)
                notifyDataSetChanged()
            }

        }
    }

    private fun deleteImageFirebase(model: ModelImagePicked, holder: HolderImagePicked, position: Int) {

        val imageId = model.id

        Log.d(TAG, "deleteImageFirebase: adId: $adId")
        Log.d(TAG, "deleteImageFirebase: imageId: $imageId")

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId).child("Images").child(imageId)
            .removeValue()
            .addOnSuccessListener {

                Log.d(TAG, "deleteImageFirebase: Image $imageId deleted")

                Utils.toast(context, "Image deleted")

                try {
                    imagesPickedArrayList.remove(model)
                    notifyItemRemoved(position)
                }catch (e: Exception){
                    Log.e(TAG, "deleteImageFirebase1: ", e)
                }
            }
            .addOnFailureListener { e ->

                Log.e(TAG, "deleteImageFirebase2: ", e)
                Utils.toast(context, "Failed to delete image due to ${e.message}")
            }
    }

    override fun getItemCount(): Int {

        return imagesPickedArrayList.size
    }



    inner class HolderImagePicked(itemView: View) : ViewHolder(itemView){

        var imageIv = binding.imageIv
        var closeBtn = binding.closeBtn

    }


}