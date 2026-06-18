package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class KeluhanPelangganAdapter(private val listKeluhan: List<JSONObject>) :
    RecyclerView.Adapter<KeluhanPelangganAdapter.KeluhanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeluhanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_keluhan_pelanggan, parent, false)
        return KeluhanViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeluhanViewHolder, position: Int) {
        holder.bind(listKeluhan[position])
    }

    override fun getItemCount(): Int = listKeluhan.size

    class KeluhanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvKategori = itemView.findViewById<TextView>(R.id.tvItemKategoriKeluhan)
        private val tvIsi = itemView.findViewById<TextView>(R.id.tvItemIsiKeluhan)
        private val tvTeknisi = itemView.findViewById<TextView>(R.id.tvItemTeknisiKeluhan)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tvItemStatusKeluhan)

        fun bind(keluhan: JSONObject) {
            tvKategori.text = keluhan.optString("kategori")
            tvIsi.text = keluhan.optString("isi_keluhan")

            val teknisi = keluhan.optString("nama_teknisi", "")
            tvTeknisi.text = if (teknisi.isEmpty() || teknisi == "null") "Teknisi: -" else "Teknisi: $teknisi"

            val status = keluhan.optString("status")
            tvStatus.text = status.replace("_", " ").uppercase()

            when (status) {
                "baru" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                "selesai" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                "ditolak" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                else -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
            }
        }
    }
}