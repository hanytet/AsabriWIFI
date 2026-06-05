package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PaketAdapter(private val listPaket: ArrayList<JSONObject>) : RecyclerView.Adapter<PaketAdapter.PaketViewHolder>() {

    // 👇 INI DIA BAGIAN YANG TERLEWAT: Deklarasi variabel onItemClick
    var onItemClick: ((JSONObject) -> Unit)? = null

    inner class PaketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaPaket: TextView = itemView.findViewById(R.id.tvNamaPaket)
        val tvHargaPaket: TextView = itemView.findViewById(R.id.tvHargaPaket)
        val tvDeskripsiPaket: TextView = itemView.findViewById(R.id.tvDeskripsiPaket)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paket, parent, false)
        return PaketViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaketViewHolder, position: Int) {
        val paket = listPaket[position]

        try {
            holder.tvNamaPaket.text = paket.getString("nama_paket")
            holder.tvHargaPaket.text = "Rp " + paket.getString("harga")

            // Cek jika kecepatan ada, gabungkan ke deskripsi
            val kecepatan = if (paket.has("kecepatan") && !paket.isNull("kecepatan")) paket.getString("kecepatan") + " " else ""
            val deskripsi = if (paket.has("deskripsi") && !paket.isNull("deskripsi")) paket.getString("deskripsi") else ""
            holder.tvDeskripsiPaket.text = "$kecepatan | $deskripsi"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 👇 DAN INI PEMICUNYA: Saat item diklik, kirim datanya ke Fragment
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(paket)
        }
    }

    override fun getItemCount(): Int {
        return listPaket.size
    }
}