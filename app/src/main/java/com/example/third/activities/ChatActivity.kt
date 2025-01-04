package com.example.third.activities

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.third.R
import com.example.third.Utils
import com.example.third.adapters.AdapterChat
import com.example.third.databinding.ActivityChatBinding
import com.example.third.models.ModelChat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.protobuf.Method
import org.json.JSONObject
import java.util.jar.Manifest

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private companion object{
        private const val TAG = "CHAT_TAG"
    }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var receiptUid = ""
    private var receiptFcmToken = ""

    private var myUid = ""
    private var myName = ""

    private var chatPath = ""

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        receiptUid = intent.getStringExtra("receiptUid")!!

        myUid = firebaseAuth.uid!!

        chatPath = Utils.chatPath(receiptUid,myUid)

        loadMyInfo()
        loadReceiptDetails()
        loadMessages()

        binding.toolbarBackBtn.setOnClickListener {
            finish()
        }

        binding.attachFab.setOnClickListener {
            imagePickDialog()
        }

        binding.sendFab.setOnClickListener{
            validateData()
        }

    }

    private fun loadMyInfo(){
        Log.d(TAG, "loadMyInfo: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    myName = "${snapshot.child("name").value}"
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadReceiptDetails(){
        Log.d(TAG, "loadReceiptDetails: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(receiptUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    try {

                        val name = "${snapshot.child("name").value}"
                        val profileImageUrl = "${snapshot.child("profileImage").value}"
                        receiptFcmToken = "${snapshot.child("fcmToken").value}"
                        Log.d(TAG, "onDataChange: name: $name")
                        Log.d(TAG, "onDataChange: profileImageUrl: $profileImageUrl")
                        Log.d(TAG, "onDataChange: receiptFcmToken: $receiptFcmToken")

                        binding.toolbarTitleTv.text = name

                        try {
                            Glide.with(this@ChatActivity)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_person_white)
                                .into(binding.toolbarProfileIv)

                        }catch (e:Exception) {
                            Log.e(TAG,"onDataChange: ",e)
                        }
                    }catch (e:Exception){
                        Log.e(TAG,"onDataChange: ",e)
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadMessages(){
        val messageArrayList = ArrayList<ModelChat>()

        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatPath)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageArrayList.clear()
                    for (ds: DataSnapshot in snapshot.children) {

                        try {
                            val modelChat = ds.getValue(ModelChat::class.java)

                            messageArrayList.add(modelChat!!)
                        }catch (e:Exception){
                            Log.e(TAG,"onDataChange: ",e)
                        }
                    }
                    val adapterChat = AdapterChat(this@ChatActivity, messageArrayList)
                    binding.chatRv.adapter = adapterChat

                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
    private fun imagePickDialog(){
        Log.d(TAG, "imagePickDialog: ")

        val popupMenu = PopupMenu(this, binding.attachFab)

        popupMenu.menu.add(Menu.NONE,1,1,"Camera")
        popupMenu.menu.add(Menu.NONE,2,2,"Gallery")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val itemId = menuItem.itemId

            if (itemId == 1){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA))
                }else{
                requestCameraPermissions.launch(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }

            }else if(itemId == 2){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    pickImageGallery()
                }else{
                    requestStoragePermissions.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }

            }

            true
        }
    }

    private val requestCameraPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ result ->

        Log.d(TAG, "requestCameraPermissions: $result")

        var areAllGranted = true

        for (isGranted in result.entries){
            areAllGranted = areAllGranted && isGranted.value
        }
        if (areAllGranted){
            Log.d(TAG, "requestCameraPermissions: granted")

            pickImageCamera()
        }else{
            Log.d(TAG, "requestCameraPermissions: denied")
            Utils.toast(this, "Please grant all permissions")
        }
    }


    private val requestStoragePermissions = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "requestStoragePermissions: $isGranted")

        if (isGranted) {
            pickImageGallery()
        }
        else{
            Utils.toast(this, "Please grant all permissions")
        }
    }

    private fun pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ")

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Pick Image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "THE_IMAGE_DESCRIPTION")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)

    }
    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if (result.resultCode == RESULT_OK){
            Log.d(TAG, "cameraActivityResultLauncher: imageUri: $imageUri")
            uploadFirebaseStorage()
    }else{
        Utils.toast(this, "Cancelled")
        }
    }

    private fun pickImageGallery(){
        Log.d(TAG,"pickImageGallery: ")

        val intent = Intent(Intent.ACTION_PICK)

        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        if (result.resultCode == RESULT_OK){
            val data = result.data
            imageUri = result.data!!.data
            Log.d(TAG, "galleryActivityResultLauncher: imageUri: $imageUri")

            uploadFirebaseStorage()
        }else{
            Utils.toast(this, "Cancelled")
        }

    }

    private fun uploadFirebaseStorage(){
        Log.d(TAG, "uploadFirebaseStorage: ")

        progressDialog.setMessage("Uploading image...")
        progressDialog.show()

        val timestamp = Utils.getTimestamp()

        val filePathAndName = "ChatImages/$timestamp"

        val storageRef = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageRef.putFile(imageUri!!)
            .addOnProgressListener { snapshot ->

                val progress = 100.0*snapshot.bytesTransferred/snapshot.totalByteCount
                progressDialog.setMessage("Uploading image: Progress ${progress.toUInt()} %")
            }
            .addOnSuccessListener { taskSnapshot ->

                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadImageUrl = uriTask.result.toString()

                if (uriTask.isSuccessful){
                    sendMessage(Utils.MESSAGE_TYPE_IMAGE, uploadImageUrl, timestamp)
                }

            }
            .addOnFailureListener{e ->
                progressDialog.dismiss()
                Log.e(TAG,"uploadToFirebaseStorage: ", e)
                Utils.toast(this, "Failed to upload due to ${e.message}")
            }
    }

    private fun validateData(){
        Log.d(TAG,"validateData:")

        val message = binding.messageEt.text.toString().trim()
        val timestamp = Utils.getTimestamp()

        if (message.isEmpty()){
            Utils.toast(this,"Enter message to send...")
        }else{
            sendMessage(Utils.MESSAGE_TYPE_TEXT,message, timestamp)
        }
    }

    private fun sendMessage(messageType: String, message: String,  timestamp: Long){

        Log.d(TAG, "sendMessage: messageType: $messageType")
        Log.d(TAG, "sendMessage: message: $message")
        Log.d(TAG, "sendMessage: timestamp: $timestamp")

        progressDialog.setMessage("Sending message...")
        progressDialog.show()

        val refChat = FirebaseDatabase.getInstance().getReference("Chats")

        val keyId = "${refChat.push().key}"

        val hashMap = HashMap<String,Any>()
        hashMap["messageId"] = "$keyId"
        hashMap["senderUid"] = "$myUid"
        hashMap["receiptUid"] = "$receiptUid"
        hashMap["message"] = "$message"
        hashMap["timestamp"] = "$timestamp"
        hashMap["messageType"] = "$messageType"
        hashMap["fromUid"] = "$myUid"

        refChat.child(chatPath)
            .child(keyId)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG,"sendMessage: message sent")
                progressDialog.dismiss()

                binding.messageEt.setText("")

                if (messageType == Utils.MESSAGE_TYPE_TEXT) {
                    prepareNotification(message)
                } else {
                    prepareNotification("Sent an attachment")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG,"sendMessage: ",e)
                progressDialog.dismiss()
                Utils.toast(this,"Failed to send message due to ${e.message}")
            }
    }

    private fun prepareNotification(message: String){
        Log.d(TAG,"prepareNotification: ")

        val notificationJo = JSONObject()
        val notificationDataJo = JSONObject()
        val notificationNotificationJo = JSONObject()

        try {

            notificationDataJo.put("notificationType", "${Utils.NOTIFICATION_TYPE_NEW_MESSAGE}")
            notificationDataJo.put("senderUid", "${firebaseAuth.uid}")

            notificationNotificationJo.put("title", "$myName")
            notificationNotificationJo.put("body", "$message")
            notificationNotificationJo.put("sound", "default")

            notificationJo.put("to", "$receiptFcmToken")
            notificationJo.put("notification", notificationNotificationJo)
            notificationJo.put("data", notificationDataJo)
        } catch (e: Exception){
            Log.e(TAG,"sendMessage: ",e)
        }

        sendFcmNotification(notificationJo)
    }

    private fun sendFcmNotification(notificationJo: JSONObject){

        val jsonObjectRequest : JsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST,
            "https://fcm.googleapis.com/fcm/send",
            notificationJo,
            Response.Listener {

                Log.d(TAG, "sendFcmNotification: Notification Send $it")
            },
            Response.ErrorListener { e ->
                Log.e(TAG, "sendFcmNotification: ", e)
            }
        ){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "key=$Utils.FCM_SERVER_KEY"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

       Volley.newRequestQueue(this).add(jsonObjectRequest)
    }
}