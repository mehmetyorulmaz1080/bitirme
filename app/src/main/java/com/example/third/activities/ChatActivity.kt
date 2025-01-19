package com.example.third.activities

import android.app.Activity
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

        chatPath = Utils.sohbetYolu(receiptUid,myUid)

        bilgilerimiYükle()
        yükleAlındıDetayları()
        yükleMesajları()

        binding.toolbarBackBtn.setOnClickListener {
            finish()
        }

        binding.attachFab.setOnClickListener {
            iletişimKutusuResimSeçimi()
        }

        binding.sendFab.setOnClickListener{
            doğrulamaVerileri()
        }

    }

    private fun bilgilerimiYükle(){
        Log.d(TAG, "bilgilerimiYükle: ")

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

    private fun yükleAlındıDetayları(){
        Log.d(TAG, "yükleAlındıDetayları: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(receiptUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    try {

                        val name = "${snapshot.child("name").value}"
                        val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                        receiptFcmToken = "${snapshot.child("fcmToken").value}"
                        Log.d(TAG, "VeriDeğişikliğiHakkında: name: $name")
                        Log.d(TAG, "VeriDeğişikliğiHakkında: profileImageUrl: $profileImageUrl")
                        Log.d(TAG, "VeriDeğişikliğiHakkında: receiptFcmToken: $receiptFcmToken")

                        binding.toolbarTitleTv.text = name

                        try {
                            Glide.with(this@ChatActivity)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_person_white)
                                .into(binding.toolbarProfileIv)

                        }catch (e:Exception) {
                            Log.e(TAG,"VeriDeğişikliğiHakkında: ",e)
                        }
                    }catch (e:Exception){
                        Log.e(TAG,"VeriDeğişikliğiHakkında: ",e)
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun yükleMesajları(){
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
                            Log.e(TAG,"VeriDeğişikliğiHakkında: ",e)
                        }
                    }
                    val adapterChat = AdapterChat(this@ChatActivity, messageArrayList)
                    binding.chatRv.adapter = adapterChat

                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
    private fun iletişimKutusuResimSeçimi(){
        Log.d(TAG, "iletişimKutusuResimSeçimi: ")

        val popupMenu = PopupMenu(this, binding.attachFab)

        popupMenu.menu.add(Menu.NONE,1,1,"Kamera")
        popupMenu.menu.add(Menu.NONE,2,2,"Galeri")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->

            val itemId = menuItem.itemId

            if (itemId == 1){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

                    istekKameraİzinleri.launch(arrayOf(android.Manifest.permission.CAMERA))
                }else{
                    istekKameraİzinleri.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }

            }else if(itemId == 2){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    resimGalerisiniSeç()
                }else{
                    depolamaİzinleriİsteyin.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }

            }

            true
        }
    }

    private val istekKameraİzinleri = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ sonuc ->

        Log.d(TAG, "istekKameraİzinleri: $sonuc")

        var hepsiVerildi = true

        for (izinVerildi in sonuc.values){
            hepsiVerildi = hepsiVerildi && izinVerildi
        }
        if (hepsiVerildi){
            Log.d(TAG, "istekKameraİzinleri: verildi ")

            resimKamerasınıSeç()
        }else{
            Log.d(TAG, "istekKameraİzinleri: reddedildi ")
            Utils.toast(this, "Lütfen tüm izinleri verin")
        }
    }


    private val depolamaİzinleriİsteyin = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { izinVerildi ->
        Log.d(TAG, "depolamaİzinleriİsteyin: $izinVerildi")

        if (izinVerildi) {
            resimGalerisiniSeç()
        }
        else{
            Utils.toast(this, "Lütfen tüm izinleri verin")
        }
    }

    private fun resimKamerasınıSeç(){
        Log.d(TAG, "resimKamerasınıSeç: ")

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "THE_IMAGE_TITLE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "THE_IMAGE_DESCRIPTION")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        kameraAktiviteSonuçBaşlatıcı.launch(intent)

    }
    private val kameraAktiviteSonuçBaşlatıcı = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if (result.resultCode == Activity.RESULT_OK){
            Log.d(TAG, "kameraAktiviteSonuçBaşlatıcı: imageUri: $imageUri")
            FirebaseStorageyükle()
    }else{
        Utils.toast(this, "İptal edildi")
        }
    }

    private fun resimGalerisiniSeç(){
        Log.d(TAG,"resimGalerisiniSeç: ")

        val intent = Intent(Intent.ACTION_PICK)

        intent.type = "image/*"
        galeriAktiviteSonuçBaşlatıcı.launch(intent)
    }

    private val  galeriAktiviteSonuçBaşlatıcı = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){sonuc ->
        if (sonuc.resultCode == Activity.RESULT_OK){

            val data = sonuc.data

            imageUri = data!!.data
            Log.d(TAG, "galeriAktiviteSonuçBaşlatıcı: imageUri: $imageUri")

            FirebaseStorageyükle()
        }else{
            Utils.toast(this, "İptal edildi")
        }

    }

    private fun FirebaseStorageyükle(){
        Log.d(TAG, "FirebaseStorageyükle: ")

        progressDialog.setMessage("Resim yükleniyor...")
        progressDialog.show()

        val timestamp = Utils.getTimestamp()

        val filePathAndName = "ChatImages/$timestamp"

        val storageRef = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageRef.putFile(imageUri!!)
            .addOnProgressListener { snapshot ->

                val progress = 100.0*snapshot.bytesTransferred/snapshot.totalByteCount
                progressDialog.setMessage("Resim yükleniyor: Yüzde ${progress.toUInt()} %")
            }
            .addOnSuccessListener { taskSnapshot ->

                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadImageUrl = uriTask.result.toString()

                if (uriTask.isSuccessful){
                    mesajGönder(Utils.MESSAGE_TYPE_IMAGE, uploadImageUrl, timestamp)
                }

            }
            .addOnFailureListener{e ->
                progressDialog.dismiss()
                Log.e(TAG,"FirebaseStorageyükle: ", e)
                Utils.toast(this, "Nedeniyle yüklenemedi ${e.message}")
            }
    }

    private fun doğrulamaVerileri(){
        Log.d(TAG,"doğrulamaVerileri:")

        val message = binding.messageEt.text.toString().trim()
        val timestamp = Utils.getTimestamp()

        if (message.isEmpty()){
            Utils.toast(this,"Gönderilecek mesajı girin...")
        }else{
            mesajGönder(Utils.MESSAGE_TYPE_TEXT,message, timestamp)
        }
    }

    private fun mesajGönder(messageType: String, message: String,  timestamp: Long){

        Log.d(TAG, "mesajGönder: messageType: $messageType")
        Log.d(TAG, "mesajGönder: message: $message")
        Log.d(TAG, "mesajGönder: timestamp: $timestamp")

        progressDialog.setMessage("Mesaj gönderiliyor...")
        progressDialog.show()

        val refChat = FirebaseDatabase.getInstance().getReference("Chats")

        val keyId = "${refChat.push().key}"

        val hashMap = HashMap<String,Any>()
        hashMap["messageId"] = "$keyId"
        hashMap["messageType"] = "$messageType"
        hashMap["message"] = "$message"
        hashMap["fromUid"] = "$myUid"
        hashMap["toUid"] = "$receiptUid"
        hashMap["timestamp"] = timestamp


        refChat.child(chatPath)
            .child(keyId)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG,"mesajGönder: mesaj gönderildi")
                progressDialog.dismiss()

                binding.messageEt.setText("")

                if (messageType == Utils.MESSAGE_TYPE_TEXT) {
                    bildirimiHazırlama(message)
                } else {
                    bildirimiHazırlama("Bir ek gönderildi")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG,"mesajGönder: ",e)
                progressDialog.dismiss()
                Utils.toast(this,"Nedeniyle mesaj gönderilemedi ${e.message}")
            }
    }

    private fun bildirimiHazırlama(message: String){
        Log.d(TAG,"bildirimiHazırlama: ")

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
            Log.e(TAG,"mesajGönder: ",e)
        }

        gönderFcmBildirimi(notificationJo)
    }

    private fun gönderFcmBildirimi(notificationJo: JSONObject){

        val jsonObjectRequest : JsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST,
            "https://fcm.googleapis.com/fcm/send",
            notificationJo,
            Response.Listener {

                Log.d(TAG, "gönderFcmBildirimi: Bildirim Gönder $it")
            },
            Response.ErrorListener { e ->
                Log.e(TAG, "gönderFcmBildirimi: ", e)
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