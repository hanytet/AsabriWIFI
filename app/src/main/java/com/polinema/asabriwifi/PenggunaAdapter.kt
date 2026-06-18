package com.polinema.asabriwifi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PenggunaAdapter(
    private val listUser: ArrayList<JSONObject>,
    private val onItemClickListener: (JSONObject) -> Unit
) : RecyclerView.Adapter<PenggunaAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Gunakan nullable TextView (?) untuk berjaga-jaga jika ID di XML salah ketik
        val tvNama: TextView? = itemView.findViewById(R.id.tvNamaPengguna)
        val tvEmail: TextView? = itemView.findViewById<TextView>(R.id.tvEmailPengguna)
        val tvNik: TextView? = itemView.findViewById<TextView>(R.id.tvNikPengguna)
        val tvRole: TextView? = itemView.findViewById<TextView>(R.id.tvRolePengguna)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pengguna, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listUser[position]
        try {
            // Gunakan safe call (?.) agar jika ID XML salah, aplikasi tidak akan crash keluar sendiri
            holder.tvNama?.text = item.optString("name", "-")
            holder.tvEmail?.text = item.optString("email", "-")

            val nik = item.optString("nik", "")
            holder.tvNik?.text = if(nik.isNotEmpty() && nik != "null") "NIK: $nik" else "NIK: -"

            val role = item.optString("role", "pelanggan").lowercase()
            holder.tvRole?.text = role.uppercase()

            when (role) {
                "admin", "secondadmin" -> {
                    holder.tvRole?.setTextColor(Color.parseColor("#C62828"))
                    holder.tvRole?.setBackgroundColor(Color.parseColor("#FFCDD2"))
                }
                "teknisi" -> {
                    holder.tvRole?.setTextColor(Color.parseColor("#0D47A1"))
                    holder.tvRole?.setBackgroundColor(Color.parseColor("#BBDEFB"))
                }
                else -> {
                    holder.tvRole?.setTextColor(Color.parseColor("#2E7D32"))
                    holder.tvRole?.setBackgroundColor(Color.parseColor("#C8E6C9"))
                }
            }

            holder.itemView.setOnClickListener {
                onItemClickListener(item)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = listUser.size
}