package com.example.third.activities

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.example.third.R
import com.example.third.Utils
import com.example.third.databinding.ActivityProfilEditBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ProfilEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfilEditBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var myUserType = ""

    private var imageUri: Uri? = null

    private companion object{
        private const val TAG = "PROFILE_EDIT_TAG"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilEditBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Lütfen bekleyin...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        bilgilerimiYükle()

        binding.toolbarBackBtn.setOnClickListener{
            onBackPressed()
        }
        binding.profileImagePickFab.setOnClickListener{
            iletişimKutusuResimSeçimi()
        }

        binding.updateBtn.setOnClickListener{
            ResimSecmeSecenekleriniGoster()
        }
    }
    private var name = ""
    private var dob = ""
    private var email = ""
    private var phoneCode = ""
    private var phoneNumber = ""

    private fun ResimSecmeSecenekleriniGoster(){
        name = binding.nameEt.text.toString().trim()
        dob = binding.dobEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        phoneCode = binding.countryCodePicker.selectedCountryCodeWithPlus
        phoneNumber = binding.phoneNumberEt.text.toString().trim()

        if (imageUri == null){

            kullanıcıBilgisiVeritabanınıGüncelle(null)
        }
        else{
            profilResimDepolamasınıYükle()

        }
    }
    private fun profilResimDepolamasınıYükle(){
        Log.d(TAG,"profilResimDepolamasınıYükle: ")

        progressDialog.setMessage("Kullanıcı profili resmi yükleniyor")
        progressDialog.show()

        val filePathAndName = "UserProfile/profile_${firebaseAuth.uid}"

        val ref = FirebaseStorage.getInstance().reference.child(filePathAndName)
        ref.putFile(imageUri!!)
            .addOnProgressListener { snapshot ->

                val progress = (100 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                Log.d(TAG, "Profil Resmi Depolamaya Yükleme: yüzde: $progress")
                progressDialog.setMessage("Profil resmi yükleniyor. Yüzde: $progress")
            }
            .addOnSuccessListener { taskSnapShot ->

                Log.d(TAG, "profilResimDepolamasınıYükle: Yükleme başarılı")
                val uriTask = taskSnapShot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadImageUrl = uriTask.result.toString()
                if (uriTask.isSuccessful){
                    kullanıcıBilgisiVeritabanınıGüncelle(uploadImageUrl)
                }
            }
            .addOnFailureListener{e ->
                Log.e(TAG, "profilResimDepolamasınıYükle: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Şu nedenlerden dolayı profil resmi yüklenemedi: ${e.message}")
            }
    }
    private fun kullanıcıBilgisiVeritabanınıGüncelle(uploadedImageUrl: String?){
        Log.d(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: ")
        progressDialog.setMessage("Profil güncelleniyor...")
        progressDialog.show()

        val hashMap = HashMap<String, Any>()
        hashMap["name"] = "$name"
        hashMap["dob"] = "$dob"
        if (uploadedImageUrl != null){
            hashMap["profileImageUrl"] = "$uploadedImageUrl"
        }

        if (myUserType.equals("Phone",true)){
            hashMap["email"] = "$email"
        }
        else if (myUserType.equals("Email", true) || myUserType.equals("Google", true)) {
            hashMap["phoneCode"] = "$phoneCode"
            hashMap["phoneNumber"] = "$phoneNumber"
        }

        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child("${firebaseAuth.uid}")
            .updateChildren(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: Güncellendi...")
                progressDialog.dismiss()
                Utils.toast(this, "Güncellendi...")

                imageUri = null
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "kullanıcıBilgisiVeritabanınıGüncelle: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Nedeniyle güncellenemedi ${e.message}")
            }
    }
    private fun bilgilerimiYükle(){
        Log.d(TAG, "bilgilerimiYükle: ")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dob = "${snapshot.child("dob").value}"
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val phoneCode = "${snapshot.child("phoneCode").value}"
                    val phoneNumber = "${snapshot.child("phoneNumber").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    myUserType = "${snapshot.child("userType").value}"

                    val phone = phoneCode + phoneNumber

                    if (myUserType.equals("Email", true) || myUserType.equals("Google", true)){

                        binding.emailTil.isEnabled = false
                        binding.emailEt.isEnabled =false
                    }
                    else{
                        binding.phoneNumberTil.isEnabled = false
                        binding.phoneNumberEt.isEnabled = false
                        binding.countryCodePicker.isEnabled = false
                    }

                    binding.emailEt.setText(email)
                    binding.dobEt.setText(dob)
                    binding.nameEt.setText(name)
                    binding.phoneNumberEt.setText(phoneNumber)

                    try {
                        val phoneCodeInt = phoneCode.replace("+","").toInt()
                        binding.countryCodePicker.setCountryForPhoneCode(phoneCodeInt.toInt())
                    }
                    catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }

                    try {
                        Glide.with(this@ProfilEditActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_phone_white)
                            .into(binding.profileIv)
                    }
                    catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })

    }

    private fun iletişimKutusuResimSeçimi(){

        val popUpMenu = PopupMenu(this, binding.profileImagePickFab)
        popUpMenu.menu.add(Menu.NONE,1,1,"Kamera")
        popUpMenu.menu.add(Menu.NONE,2,2,"Galeri")
        popUpMenu.show()
        popUpMenu.setOnMenuItemClickListener { item ->

            val itemId = item.itemId
            if (itemId == 1){
                Log.d(TAG, "iletişimKutusuResimSeçimi: Kamera Tıklandı, Kamera izninin verilip verilmediğini kontrol edin ")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    istekKameraİzinleri.launch(arrayOf(android.Manifest.permission.CAMERA))
                }
                else{
                    istekKameraİzinleri.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            }

            else if (itemId == 2){
                Log.d(TAG, "iletişimKutusuResimSeçimi: Galeri Tıklandı, Depolama izninin verilip verilmediğini kontrol edin ")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    resimGalerisiniSeç()
                }
                else{
                    depolamaİzinleriİsteyin.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private val istekKameraİzinleri = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){result ->
        Log.d(TAG, "istekKameraİzinleri: result: $result ")
        var areAllGranted = true
        for (isGranted in result.values){
            areAllGranted = areAllGranted && isGranted
        }
        if (areAllGranted){
            Log.d(TAG, "istekKameraİzinleri: Hepsi verildi; Kamera, Depolama ")
            resimKamerasınıSeç()
        }
        else{
            Log.d(TAG, "istekKameraİzinleri: Hepsi veya biri reddedildi")
            Utils.toast(this, "Kamera veya Depolama veya her iki izin de reddedildi")
        }
    }
    private val depolamaİzinleriİsteyin = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        Log.d(TAG, "depolamaİzinleriİsteyin: permissions: $permissions ")

        val allGranted = permissions.all { it.value }

        if (allGranted) {
            resimGalerisiniSeç()
        } else {
            Utils.toast(this, "Depolama izni reddedildi")
        }
    }


    private fun resimKamerasınıSeç(){
        Log.d(TAG, "resimKamerasınıSeç: ")

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_İmage_title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_image_description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        kameraAktiviteSonuçBaşlatıcı.launch(intent)

    }

    private val kameraAktiviteSonuçBaşlatıcı = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->

        if (result.resultCode == Activity.RESULT_OK){
            Log.d(TAG, "kameraAktiviteSonuçBaşlatıcı: Yakalanan resim: imageUri: $imageUri")

            try {
                Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_person_white)
                    .into(binding.profileIv)
            }
            catch (e: Exception){
                Log.e(TAG, "kameraAktiviteSonuçBaşlatıcı: ", e)
            }
        }
        else{
            Utils.toast(this, "")
        }
    }

    private fun resimGalerisiniSeç(){
        Log.d(TAG, "resimGalerisiniSeç: ")

        val intent = Intent(Intent.ACTION_PICK)

        intent.type = "image/*"
        galeriAktiviteSonuçBaşlatıcı.launch(intent)
    }

    private val galeriAktiviteSonuçBaşlatıcı = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
        if (result.resultCode == Activity.RESULT_OK){

            val data = result.data

            imageUri = data!!.data

            try {
                Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_person_white)
                    .into(binding.profileIv)
            }
            catch (e: Exception){
                Log.e(TAG, "galeriAktiviteSonuçBaşlatıcı: ", e)
            }
        }
        else{
        }

    }
}