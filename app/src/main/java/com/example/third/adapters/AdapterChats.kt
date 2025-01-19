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
import com.example.third.FilterChats
import com.example.third.R
import com.example.third.Utils
import com.example.third.activities.ChatActivity
import com.example.third.databinding.RowChatsBinding
import com.example.third.models.ModelChats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.logging.Filter

class AdapterChats : RecyclerView.Adapter<AdapterChats.HolderChats>, Filterable {

    private var context: Context

     var chatsArrayList: ArrayList<ModelChats>
     private var filterlist: ArrayList<ModelChats>

     private var filter: FilterChats? = null

    private lateinit var binding: RowChatsBinding

    private companion object{
        private const val TAG = "ADAPTER_CHATS_TAG"
    }

    private lateinit var firebaseAuth: FirebaseAuth

    private var myUid = ""

    constructor(context: Context, chatArrayList: ArrayList<ModelChats>) {
        this.context = context
        this.chatsArrayList = chatArrayList
        this.filterlist = chatArrayList

        firebaseAuth = FirebaseAuth.getInstance()
        myUid = "${firebaseAuth.uid}"

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChats {

        binding = RowChatsBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderChats(binding.root)
    }

    override fun getItemCount(): Int {

        return chatsArrayList.size
    }

    override fun onBindViewHolder(holder: HolderChats, position: Int) {
        val modelChats = chatsArrayList[position]

        loadLastMessage(modelChats, holder)

        holder.itemView.setOnClickListener{

            val receiptUid = modelChats.receiptUid

            if (receiptUid != null) {
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("receiptUid", receiptUid)
                context.startActivity(intent)

            }
        }
    }

    private fun loadLastMessage(modelChats: ModelChats, holder: AdapterChats.HolderChats) {
        val chatKey = modelChats.chatKey
        Log.d(TAG, "loadLastMessage: ChatKey: $chatKey")

        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatKey).limitToLast(1)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children){

                        val fromUid = "${ds.child("fromUid").value}"
                        val message = "${ds.child("message").value}"
                        val messageId = "${ds.child("messageId").value}"
                        val messageType = "${ds.child("messageType").value}"
                        val timestamp = ds.child("timestamp").value as Long ?: 0
                        val toUid = "${ds.child("toUid").value}"

                        val formattedDate = Utils.formatZamanDamgasiTarih(timestamp)

                        modelChats.message = message
                        modelChats.messageId = messageId
                        modelChats.messageType = messageType
                        modelChats.fromUid = fromUid
                        modelChats.toUid = toUid

                        holder.dateTimeTv.text = "$formattedDate"

                        if (messageType == Utils.MESSAGE_TYPE_TEXT){
                            holder.lastMessageTv.text = message
                        }else{
                            holder.lastMessageTv.text = "Sends Attachment"
                        }
                    }

                    loadReceiptUserInfo(modelChats, holder)
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadReceiptUserInfo(modelChats: ModelChats, holder: AdapterChats.HolderChats) {

        val fromUid = modelChats.fromUid
        val toUid = modelChats.toUid

        var receiptUid = ""
        if (fromUid == myUid){

            receiptUid = toUid

        }else{

            receiptUid = fromUid

        }

        Log.d(TAG,"loadReceiptUSerInfo: fromUid: $fromUid")
        Log.d(TAG,"loadReceiptUSerInfo: toUid: $toUid")
        Log.d(TAG,"loadReceiptUSerInfo: receiptUid: $receiptUid")

        modelChats.receiptUid = receiptUid

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(receiptUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    val name = "${snapshot.child("name").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"

                    modelChats.name = name
                    modelChats.profileImageUrl = profileImageUrl

                    holder.nameTv.text = name
                    try {
                        Glide.with(context)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(holder.profileIv)
                    }catch (e:Exception){
                        Log.d(TAG, "onDataChange: ", e)
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    override fun getFilter(): android.widget.Filter {

        if (filter == null){
            filter = FilterChats(this, filterlist)
        }
        return filter as FilterChats

    }

    inner class HolderChats(itemView: View) : RecyclerView.ViewHolder(itemView){

        val profileIv = binding.profileIv
        val nameTv = binding.nameTv
        val lastMessageTv = binding.lastMessageTv
        val dateTimeTv = binding.dateTimeTv
    }

}