package com.polinema.asabriwifi

import android.graphics.Color
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PenggunaAdapter(
    private val listUser: ArrayList<JSONObject>
) : RecyclerView.Adapter<PenggunaAdapter.ViewHolder>() {

    // 🚀 POSISI TERPILIH: Menyimpan indeks item yang sedang di-long press untuk Fragment
    var positionTerpilih: Int = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        val tvNama: TextView? = itemView.findViewById(R.id.tvNamaPengguna)
        val tvEmail: TextView? = itemView.findViewById<TextView>(R.id.tvEmailPengguna)
        val tvNik: TextView? = itemView.findViewById<TextView>(R.id.tvNikPengguna)
        val tvRole: TextView? = itemView.findViewById<TextView>(R.id.tvRolePengguna)

        init {
            // Daftarkan View Holder ke sistem Context Menu Android
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.setHeaderTitle("Opsi Pengguna")
            menu?.add(this.adapterPosition, 201, 0, "Edit Informasi Pengguna")
            menu?.add(this.adapterPosition, 202, 1, "Reset Password ke Default")
            menu?.add(this.adapterPosition, 203, 2, "Hapus Akun Pengguna")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pengguna, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listUser[position]
        try {
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

            // 🚀 HAPUS KLIK BIASA: Di-set null agar ketukan singkat tidak memicu aksi apa pun
            holder.itemView.setOnClickListener(null)

            // 🚀 MURNI LONG CLICK: Menyimpan posisi indeks daftar sebelum menu melayang Android muncul
            holder.itemView.setOnLongClickListener {
                positionTerpilih = holder.adapterPosition
                false // return false agar ContextMenu bawaan OS mencuat keluar
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = listUser.size
}