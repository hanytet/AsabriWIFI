package com.polinema.asabriwifi

import android.graphics.Color
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PelangganAdapter(private val listPelanggan: ArrayList<JSONObject>) : RecyclerView.Adapter<PelangganAdapter.PelangganViewHolder>() {

    // Menyimpan posisi untuk ContextMenu saat mendeteksi Klik Lama (Long Click)
    var positionTerpilih: Int = -1

    inner class PelangganViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        val tvNamaPelanggan: TextView = itemView.findViewById(R.id.tvNamaPelanggan)
        val tvAlamatPelanggan: TextView = itemView.findViewById(R.id.tvAlamatPelanggan)
        val tvNoHpPelanggan: TextView = itemView.findViewById(R.id.tvNoHpPelanggan)
        val tvStatusPelanggan: TextView = itemView.findViewById(R.id.tvStatusPelanggan)

        init {
            // Daftarkan View Holder ke sistem Context Menu Android
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.setHeaderTitle("Opsi Pelanggan")
            menu?.add(this.adapterPosition, 101, 0, "Edit Pelanggan")
            menu?.add(this.adapterPosition, 102, 1, "Hapus Pelanggan")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PelangganViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pelanggan, parent, false)
        return PelangganViewHolder(view)
    }

    override fun onBindViewHolder(holder: PelangganViewHolder, position: Int) {
        val pelanggan = listPelanggan[position]

        try {
            val nama = pelanggan.optString("name", "Tanpa Nama")
            val alamat = if (!pelanggan.isNull("alamat") && pelanggan.optString("alamat").isNotEmpty()) {
                pelanggan.optString("alamat")
            } else "-"

            val noHp = if (!pelanggan.isNull("no_hp") && pelanggan.optString("no_hp").isNotEmpty()) {
                pelanggan.optString("no_hp")
            } else "-"

            val status = pelanggan.optString("status", "Aktif").uppercase()

            holder.tvNamaPelanggan.text = nama
            holder.tvAlamatPelanggan.text = "Alamat: $alamat"
            holder.tvNoHpPelanggan.text = "No HP: $noHp"

            holder.tvStatusPelanggan.text = status
            holder.tvStatusPelanggan.setTextColor(if (status == "NONAKTIF") Color.parseColor("#F44336") else Color.parseColor("#4CAF50"))

        } catch (e: Exception) { e.printStackTrace() }

        // 🚀 HAPUS ATAU KOSONGKAN KLIK BIASA
        // Jika tidak ingin ada aksi apa pun saat diklik biasa,
        // Anda bisa menghapus blok setOnClickListener ini sepenuhnya.
        holder.itemView.setOnClickListener(null)

        // 🚀 BIARKAN LONG CLICK YANG BEKERJA:
        // Sistem Android secara otomatis akan menampilkan Context Menu
        // yang sudah didaftarkan di atas saat item ditekan lama.
        holder.itemView.setOnLongClickListener {
            positionTerpilih = holder.adapterPosition
            false // return false agar ContextMenu bawaan Android muncul
        }
    }

    override fun getItemCount(): Int = listPelanggan.size
}