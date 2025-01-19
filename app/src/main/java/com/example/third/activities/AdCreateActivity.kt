package com.example.third.activities

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.third.R
import com.example.third.Utils
import com.example.third.adapters.AdapterImagePicked
import com.example.third.databinding.ActivityAdCreateBinding
import com.example.third.models.ModelImagePicked
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class AdCreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdCreateBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var imagesPickedArrayList: ArrayList<ModelImagePicked>
    private lateinit var adapterImagePicked: AdapterImagePicked

    private var imageUri: Uri? = null

    private companion object{
        const val TAG = "ADD_CREATE_TAG"
    }

    private var isEditMode = false
    private var adIdForEditing = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()

        val adapterCategories = ArrayAdapter(this,
            R.layout.raw_category_act,
            Utils.kategoriler
        )
        binding.categoryAct.setAdapter(adapterCategories)

        val adapterCondition = ArrayAdapter(this,
            R.layout.row_condition_act,
            Utils.durum
        )
        binding.conditionAct.setAdapter(adapterCondition)

        isEditMode = intent.getBooleanExtra("isEditMode", false)
        Log.d(TAG, "onCreate: isEditMode: $isEditMode")

        if (isEditMode){

            adIdForEditing = intent.getStringExtra("adId") ?: ""

            IlanAyrintilariniYukle()

            binding.toolbarTitleTv.text = "İlanı Güncelle"
            binding.postAdBtn.text = "İlanı Güncelle"

        }else{

            binding.toolbarTitleTv.text = "İlan Oluştur"
            binding.postAdBtn.text = "İlan ver"
        }

        imagesPickedArrayList = ArrayList()

        ResimleriYükle()

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }

        binding.locationAct.setOnClickListener{
            val intent = Intent(this, LocationPickerActivity::class.java)
            konumSeciciActivitySonucBaslatici.launch(intent)
        }
        binding.toolbarAdImageBtn.setOnClickListener{
            ResimSecmeSecenekleriniGoster()
        }

        binding.postAdBtn.setOnClickListener{
            doğrulamaVerileri()
        }
    }

    private val konumSeciciActivitySonucBaslatici =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            Log.d(TAG, "konumSeciciActivitySonucBaslatici: ")

            if(result.resultCode == Activity.RESULT_OK){

                val data = result.data

                if (data != null) {
                    latitude = data.getDoubleExtra("latitude", 0.0)
                    longitude = data.getDoubleExtra("longitude", 0.0)
                    adres = data.getStringExtra("address") ?: ""

                    Log.d(TAG, "konumSeciciActivitySonucBaslatici: latitude: $latitude")
                    Log.d(TAG, "konumSeciciActivitySonucBaslatici: longitude: $longitude")
                    Log.d(TAG, "konumSeciciActivitySonucBaslatici: address: $adres")

                    binding.locationAct.setText(adres)
                }
            }else{
                Log.d(TAG, "konumSeciciActivitySonucBaslatici: Cancelled")
                Utils.toast(this, "İptal edildi")
            }
        }

    private fun ResimleriYükle() {
        Log.d(TAG, "loadImages: ")

        adapterImagePicked = AdapterImagePicked(this, imagesPickedArrayList, adIdForEditing)

        binding.imagesRv.adapter = adapterImagePicked
    }

    private fun ResimSecmeSecenekleriniGoster() {
        Log.d(TAG, "ResimSecmeSecenekleriniGoster: ")

        val popupMenu = PopupMenu(this, binding.toolbarAdImageBtn)

        popupMenu.menu.add(Menu.NONE, 1, 1, "Kamera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Galeri")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->

            val itemId = item.itemId

            if(itemId == 1){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    val cameraPermission = arrayOf(Manifest.permission.CAMERA)
                    istekKameraIzni.launch(cameraPermission)
                }
                else{
                    val cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    istekKameraIzni.launch(cameraPermission)
                }
            }
            else if (itemId == 2){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ResimGalerisiniSec()
                }
                else{
                    val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                    istekDepolamaIzni.launch(storagePermission)
                }
            }
            true

        }
    }

    private val istekDepolamaIzni = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted ->
        Log.d(TAG, "istekDepolamaIzni: isGranted: $isGranted")

        if(isGranted){
            ResimGalerisiniSec()
        }
        else{
            Utils.toast(this, "Depolama İzni Reddedildi")
        }

    }

    private val istekKameraIzni = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { result ->
        Log.d(TAG, "istekKameraIzni: result: $result")

        var areAllGranted = true
        for(isGranted in result.values){
            areAllGranted = areAllGranted && isGranted
        }

        if(areAllGranted){
            ResimKamerasiniSec()
        }
        else{
            Utils.toast(this, "Kamera veya Depolama veya Her İkisinin İzinleri Reddedildi")
        }
    }

    private fun ResimGalerisiniSec(){
        Log.d(TAG, "ResimGalerisiniSec: ")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galeriAktiviteSonucBaslatici.launch(intent)
    }

    private fun ResimKamerasiniSec(){
        Log.d(TAG,"ResimKamerasiniSec: ")

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image_Title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image_Description")


        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        kameraAktiviteSonuçBaşlatıcı.launch(intent)
    }

    private val galeriAktiviteSonucBaslatici = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        Log.d(TAG, "galeriAktiviteSonucBaslatici: ")

        if(result.resultCode == Activity.RESULT_OK) {

            val data = result.data
            imageUri = data!!.data
            Log.d(TAG, "galeriAktiviteSonucBaslatici: imageUri: $imageUri")

            val timestamp = "${Utils.getTimestamp()}"

            val modelImagePicked = ModelImagePicked(timestamp, imageUri, null, false)

            imagesPickedArrayList.add(modelImagePicked)

            ResimleriYükle()
        }
        else{
            Utils.toast(this, "İptal edildi")
        }

    }

    private val kameraAktiviteSonuçBaşlatıcı = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){result ->
        Log.d(TAG, "kameraAktiviteSonuçBaşlatıcı: ")

        if(result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "kameraAktiviteSonuçBaşlatıcı: imageUri: $imageUri")
            val timestamp = "${Utils.getTimestamp()}"
            val modelImagePicked = ModelImagePicked(timestamp, imageUri, null, false)
            imagesPickedArrayList.add(modelImagePicked)
            ResimleriYükle()
        }
        else{
            Utils.toast(this, "İptal edildi")
        }
    }

    private var marka = ""
    private var kategori = ""
    private var durum = ""
    private var adres = ""
    private var fiyat = ""
    private var baslik = ""
    private var aciklama = ""
    private var latitude = 0.0
    private var longitude = 0.0

    private fun doğrulamaVerileri() {
        Log.d(TAG, "doğrulamaVerileri: ")

        marka = binding.brandEt.text.toString().trim()
        kategori = binding.categoryAct.text.toString().trim()
        durum = binding.conditionAct.text.toString().trim()
        adres = binding.locationAct.text.toString().trim()
        fiyat = binding.priceEt.text.toString().trim()
        baslik = binding.titleEt.text.toString().trim()
        aciklama = binding.descriptionEt.text.toString().trim()

        if(marka.isEmpty()){

            binding.brandEt.error = "Marka Girin"
            binding.brandEt.requestFocus()
        }
        else if(kategori.isEmpty()){
            binding.categoryAct.error = "Kategori Seçin"
            binding.categoryAct.requestFocus()
        }
        else if(durum.isEmpty()){
            binding.conditionAct.error = "Durum Seçin"
            binding.conditionAct.requestFocus()
        }
        else if(baslik.isEmpty()){
            binding.titleEt.error = "Başlık Girin"
            binding.titleEt.requestFocus()
        }
        else if(fiyat.isEmpty()){
            binding.priceEt.error = "Açıklama Girin"
            binding.priceEt.requestFocus()
        }
        else if(aciklama.isEmpty()){
            binding.descriptionEt.error = "Açıklama Girin"
            binding.descriptionEt.requestFocus()
        }
        else{

            if(isEditMode){

                İlanigüncelle()
            }else{

                ilanVer()
            }
        }
    }
    private fun ilanVer() {
        Log.d(TAG, "ilanVer: ")
        progressDialog.setMessage("İlan veriliyor...")
        progressDialog.show()
        val timestamp = Utils.getTimestamp()

        val refAds = FirebaseDatabase.getInstance().getReference("Ads")

        val keyId = refAds.push().key

        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$keyId"
        hashMap["uid"] = "${firebaseAuth.uid}"
        hashMap["brand"] = "$marka"
        hashMap["category"] = "$kategori"
        hashMap["condition"] = "$durum"
        hashMap["address"] = "$adres"
        hashMap["price"] = "$fiyat"
        hashMap["title"] = "$baslik"
        hashMap["description"] = "$aciklama"
        hashMap["status"] = "${Utils.AD_STATUS_AVAILABLE}"
        hashMap["timestamp"] = timestamp
        hashMap["latitude"] = latitude
        hashMap["longitude"] = longitude

        refAds.child(keyId!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "ilanVer: ")
                ResmiDepolamayaYukle(keyId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG,"ilanVer: İlan Yayınlandı ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Nedeniyle ilan yayınlanamadı ${e.message}")
            }
    }

    private fun İlanigüncelle(){
        Log.d(TAG, "İlanigüncelle: ")

        progressDialog.setMessage("İlan Güncelleniyor...")
        progressDialog.show()

        val hashMap = HashMap<String, Any>()
        hashMap["brand"] = "$marka"
        hashMap["category"] = "$kategori"
        hashMap["condition"] = "$durum"
        hashMap["address"] = "$adres"
        hashMap["price"] = "$fiyat"
        hashMap["title"] = "$baslik"
        hashMap["description"] = "$aciklama"
        hashMap["latitude"] = latitude
        hashMap["longitude"] = longitude

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adIdForEditing)
            .updateChildren(hashMap)
            .addOnSuccessListener {

                Log.d(TAG,"İlanigüncelle: İlan Güncellendi...")
                progressDialog.dismiss()
                ResmiDepolamayaYukle(adIdForEditing)
            }
            .addOnFailureListener { e ->

                Log.e(TAG, "ilaniGuncelle: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Nedeniyle güncellenemedi ${e.message}")
            }
    }

    private fun ResmiDepolamayaYukle(adId: String) {

        for(i in imagesPickedArrayList.indices){

            val modelImagePicked = imagesPickedArrayList[i]

            if (!modelImagePicked.fromInternet){
                val imageName = modelImagePicked.id

                val filePathAndName = "Ads/$imageName"
                val imageIndexForProgress = i + 1

                val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)

                storageReference.putFile(modelImagePicked.imageUri!!)
                    .addOnProgressListener { snapshot ->

                        val progress = 100.0 * snapshot.bytesTransferred / snapshot.totalByteCount
                        Log.d(TAG, "ResmiDepolamayaYukle: yüzde: $progress")
                        val message = "Resimler Yükleniyor... $imageIndexForProgress nin ${imagesPickedArrayList.size} resimler... % ${progress.toInt()}"
                        Log.d(TAG, "ResmiDepolamayaYukle: $message")

                        progressDialog.setMessage(message)
                        progressDialog.show()
                    }
                    .addOnSuccessListener{ taskSnapshot ->
                        Log.d(TAG, "ResmiDepolamayaYukle: Başarılı")

                        val uriTask = taskSnapshot.storage.downloadUrl
                        while (!uriTask.isSuccessful);
                        val uploadedImageUrl = uriTask.result

                        if (uriTask.isSuccessful){

                            val hashMap = HashMap<String, Any>()
                            hashMap["id"] = "${modelImagePicked.id}"
                            hashMap["imageUrl"] = "$uploadedImageUrl"

                            val ref = FirebaseDatabase.getInstance().getReference("Ads")
                            ref.child(adId)
                                .child("Images")
                                .child(imageName)
                                .updateChildren(hashMap)
                        }

                        progressDialog.dismiss()
                    }
                    .addOnFailureListener{ e ->
                        Log.e(TAG, "ResmiDepolamayaYukle: ", e)
                        progressDialog.dismiss()


                    }

            }



        }
    }

    private fun IlanAyrintilariniYukle(){
        Log.d(TAG, "IlanAyrintilariniYukle: ")

        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adIdForEditing)
            .addListenerForSingleValueEvent(object: ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {

                    val marka = "${snapshot.child("brand").value}"
                    val kategori = "${snapshot.child("category").value}"
                    val durum = "${snapshot.child("condition").value}"
                    latitude = (snapshot.child("latitude").value as Double) ?: 0.0
                    longitude = (snapshot.child("longitude").value as Double) ?: 0.0
                    val adres = "${snapshot.child("address").value}"
                    val fiyat = "${snapshot.child("price").value}"
                    val baslik = "${snapshot.child("title").value}"
                    val aciklama = "${snapshot.child("description").value}"

                    binding.brandEt.setText(marka)
                    binding.categoryAct.setText(kategori)
                    binding.conditionAct.setText(durum)
                    binding.locationAct.setText(adres)
                    binding.priceEt.setText(fiyat)
                    binding.titleEt.setText(baslik)
                    binding.descriptionEt.setText(aciklama)

                    val refImages = snapshot.child("Images").ref
                    refImages.addListenerForSingleValueEvent(object: ValueEventListener{

                        override fun onDataChange(snapshot: DataSnapshot) {

                            for (ds in snapshot.children){

                                val id = "${ds.child("id").value}"
                                val imageUrl = "${ds.child("imageUrl").value}"

                                val modelImagePicked = ModelImagePicked(id, null, imageUrl, true)
                                imagesPickedArrayList.add(modelImagePicked)
                            }

                            ResimleriYükle()
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}

