package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.util.Locale

// 🚀 DIJAMIN FIX: Mengimpor R lokal secara eksplisit agar Gradle mengenali item_paket_pelanggan
import com.polinema.asabriwifi.R

class PaketPelangganAdapter(
    private val listPaket: List<JSONObject>,
    private val onPilihClick: (JSONObject) -> Unit
) : RecyclerView.Adapter<PaketPelangganAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.itemNamaPaket)
        val tvSpeed: TextView = view.findViewById(R.id.itemKecepatan)
        val tvHarga: TextView = view.findViewById(R.id.itemHarga)
        val btnPilih: Button = view.findViewById(R.id.itemBtnPilih)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Menginflate file XML item_paket_pelanggan yang berada di res/layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paket_pelanggan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paket = listPaket[position]
        holder.tvNama.text = paket.optString("nama_paket", "Paket Internet")
        holder.tvSpeed.text = "${paket.optString("kecepatan", "0")} Mbps - Unlimited Quota"

        val harga = paket.optLong("harga", 0)
        holder.tvHarga.text = String.format(Locale.US, "Rp %,d / bulan", harga).replace(",", ".")

        holder.btnPilih.setOnClickListener { onPilihClick(paket) }
    }

    override fun getItemCount(): Int = listPaket.size
}