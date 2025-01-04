package com.example.third

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.DateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Locale

object Utils {

    const val MESSAGE_TYPE_TEXT = "TEXT"
    const val MESSAGE_TYPE_IMAGE = "IMAGE"

    const val AD_STATUS_AVAILABLE = "AVAILABLE"
    const val AD_STATUS_SOLD = "SOLD"

    const val NOTIFICATION_TYPE_NEW_MESSAGE = "NOTIFICATION_TYPE_NEW_MESSAGE"
    const val FCM_SERVER_KEY = "MESSAGECLOUDIP"


    val categories = arrayOf(
        "All",
        "Mobiles",
        "Computer/Laptop",
        "Electronics & Appliances",
        "Vehicles",
        "Furniture & Home Decor",
        "Fashion & Beauty",
        "Books",
        "Sports",
        "Animals",
        "Businesses",
        "Agriculture"
    )

    val categoryIcons = arrayOf(
        R.drawable.ic_category_all,
        R.drawable.ic_category_mobiles,
        R.drawable.ic_category_computer,
        R.drawable.ic_category_electronics,
        R.drawable.ic_category_vehicles,
        R.drawable.ic_category_furniture,
        R.drawable.ic_category_fashion,
        R.drawable.ic_category_books,
        R.drawable.ic_category_sports,
        R.drawable.ic_category_pets,
        R.drawable.ic_category_business,
        R.drawable.ic_category_agriculture,
    )
    val condition = arrayOf(
        "New",
        "Used",
        "Refurbished",
    )


    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun getTimestamp() : Long {

        return System.currentTimeMillis()
    }

    fun formatTimestampDate(timestamp: Long) : String {

        val calender = Calendar.getInstance(Locale.ENGLISH)
        calender.timeInMillis = timestamp

        return android.text.format.DateFormat.format("dd/MM/yyyy", calender).toString()
    }
    fun formatTimestampDateTime(timestamp: Long) : String {
        val calender = Calendar.getInstance(Locale.ENGLISH)
        calender.timeInMillis = timestamp

        return android.text.format.DateFormat.format("dd/MM/yyyy hh:mm:aa", calender).toString()
    }

    fun chatPath(receiptUid: String, yourUid: String) : String {


        val arrayUids = arrayOf(receiptUid, yourUid)

        Arrays.sort(arrayUids)

        return "${arrayUids[0]}_${arrayUids[1]}"
    }

    fun addToFavorite(context: Context, adId: String) {

        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null) {
            Utils.toast(context, "You're not logged in")
        } else {

            val timestamp = Utils.getTimestamp()

            val hashMap = HashMap<String, Any>()
            hashMap["adId"] = adId
            hashMap["timestamp"] = timestamp

            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(adId)
                .setValue(hashMap)
                .addOnSuccessListener {
                    Utils.toast(context, "Added to favorite")
                }
                .addOnFailureListener { e ->
                    Utils.toast(context, "Failed to add to favorite due to ${e.message}")
                }
        }
    }

    fun removeFromFavorite(context: Context, adId: String) {
        val firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser == null) {
            Utils.toast(context, "You're not logged in")
        } else {
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(adId)
                .removeValue()
                .addOnSuccessListener {
                    Utils.toast(context, "Removed from favorite")
                    }
                .addOnFailureListener { e ->
                    Utils.toast(context, "Failed to remove from favorite due to ${e.message}")
                }
        }
    }

    fun callIntent(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:${Uri.encode(phone)}"))
        context.startActivity(intent)
    }
    fun smsIntent(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms: ${Uri.encode(phone)}"))
        context.startActivity(intent)
    }

    fun mapIntent(context: Context, latitude: Double, longitude: Double) {

        val gmmIntentUri = Uri.parse("http://maps.google.com/maps?daddr=$latitude,$longitude")

        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(context.packageManager) != null) {

            context.startActivity(mapIntent)
        } else {

            Utils.toast(context, "Google Maps not found")

        }


    }

}