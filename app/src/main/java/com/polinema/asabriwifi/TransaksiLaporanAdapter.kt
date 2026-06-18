package com.polinema.asabriwifi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class TransaksiLaporanAdapter(private val listData: ArrayList<JSONObject>, private var isHarian: Boolean) : RecyclerView.Adapter<TransaksiLaporanAdapter.ViewHolder>() {

    fun setMode(harian: Boolean) {
        this.isHarian = harian
        // 👇 WAJIB TAMBAHKAN INI: Memaksa RecyclerView menggambar ulang item saat pindah tab
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama = itemView.findViewById<TextView>(R.id.tvNamaPelangganLaporan)
        val tvPaket = itemView.findViewById<TextView>(R.id.tvPaketLaporan)
        val tvMetode = itemView.findViewById<TextView>(R.id.tvMetodeLaporan)
        val tvWaktu = itemView.findViewById<TextView>(R.id.tvWaktuLaporan)
        val tvJumlah = itemView.findViewById<TextView>(R.id.tvJumlahLaporan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaksi_laporan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listData[position]
        try {
            holder.tvNama.text = item.optString("nama", "Pelanggan")
            holder.tvPaket.text = item.optString("paket", "-")

            val metode = item.optString("metode", "-")
            holder.tvMetode.text = metode.uppercase()

            if (metode.lowercase() == "tunai") {
                holder.tvMetode.setTextColor(Color.parseColor("#F57F17"))
                holder.tvMetode.setBackgroundColor(Color.parseColor("#FFF9C4"))
            } else {
                holder.tvMetode.setTextColor(Color.parseColor("#1976D2"))
                holder.tvMetode.setBackgroundColor(Color.parseColor("#E3F2FD"))
            }

            // Membaca key data secara dinamis berdasarkan format JSON dari Api\LaporanController baru Anda
            holder.tvWaktu.text = if (isHarian) item.optString("waktu", "-") else item.optString("tanggal", "-")
            holder.tvJumlah.text = "Rp " + String.format("%,d", item.optLong("jumlah", 0)).replace(",", ".")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = listData.size
}