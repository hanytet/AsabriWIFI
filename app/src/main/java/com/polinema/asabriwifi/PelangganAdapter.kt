package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PelangganAdapter(private val listPelanggan: ArrayList<JSONObject>) : RecyclerView.Adapter<PelangganAdapter.PelangganViewHolder>() {

    var onItemClick: ((JSONObject) -> Unit)? = null

    inner class PelangganViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaPelanggan: TextView = itemView.findViewById(R.id.tvNamaPelanggan)
        val tvAlamatPelanggan: TextView = itemView.findViewById(R.id.tvAlamatPelanggan)
        val tvNoHpPelanggan: TextView = itemView.findViewById(R.id.tvNoHpPelanggan)
        val tvStatusPelanggan: TextView = itemView.findViewById(R.id.tvStatusPelanggan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PelangganViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pelanggan, parent, false)
        return PelangganViewHolder(view)
    }

    override fun onBindViewHolder(holder: PelangganViewHolder, position: Int) {
        val pelanggan = listPelanggan[position]

        try {
            // Gunakan optString agar kebal terhadap data kosong (null) dari Laravel
            val nama = pelanggan.optString("name", "Tanpa Nama")

            // Cek apakah alamat null atau kosong
            val alamat = if (!pelanggan.isNull("alamat") && pelanggan.optString("alamat").isNotEmpty()) {
                pelanggan.optString("alamat")
            } else {
                "-"
            }

            // Cek apakah no_hp null atau kosong
            val noHp = if (!pelanggan.isNull("no_hp") && pelanggan.optString("no_hp").isNotEmpty()) {
                pelanggan.optString("no_hp")
            } else {
                "-"
            }

            val status = pelanggan.optString("status", "Aktif").uppercase()

            // Pasang ke komponen layar
            holder.tvNamaPelanggan.text = nama
            holder.tvAlamatPelanggan.text = "Alamat: $alamat"
            holder.tvNoHpPelanggan.text = "No HP: $noHp"

            // Opsional: Beri warna beda jika statusnya nonaktif
            holder.tvStatusPelanggan.text = status
            if (status == "NONAKTIF") {
                holder.tvStatusPelanggan.setTextColor(android.graphics.Color.parseColor("#F44336")) // Merah
            } else {
                holder.tvStatusPelanggan.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Hijau
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Aksi saat kotak diklik
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(pelanggan)
        }
    }

    override fun getItemCount(): Int {
        return listPelanggan.size
    }
}