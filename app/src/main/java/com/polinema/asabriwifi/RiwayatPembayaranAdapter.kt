package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class RiwayatPembayaranAdapter(private val listRiwayat: List<JSONObject>) :
    RecyclerView.Adapter<RiwayatPembayaranAdapter.RiwayatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_riwayat_pembayaran, parent, false)
        return RiwayatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        val riwayat = listRiwayat[position]
        holder.bind(riwayat)
    }

    override fun getItemCount(): Int = listRiwayat.size

    class RiwayatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNamaPaket: TextView = itemView.findViewById(R.id.tvRiwayatNamaPaket)
        private val tvPeriode: TextView = itemView.findViewById(R.id.tvRiwayatPeriode)
        private val tvMetode: TextView = itemView.findViewById(R.id.tvRiwayatMetode)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvRiwayatStatus)
        private val tvJumlah: TextView = itemView.findViewById(R.id.tvRiwayatJumlah)

        fun bind(riwayat: JSONObject) {
            // Mengambil data paket dan kecepatan dengan fallback aman
            val namaPaket = if (!riwayat.isNull("nama_paket")) riwayat.optString("nama_paket") else "Paket Wifi"
            val kecepatan = if (!riwayat.isNull("kecepatan")) riwayat.optString("kecepatan") else "-"

            // Mengambil data periode bulan dan tahun bulanan
            val bulan = if (!riwayat.isNull("periode_bulan")) riwayat.optString("periode_bulan") else "-"
            val tahun = if (!riwayat.isNull("periode_tahun")) riwayat.optString("periode_tahun") else "-"

            val metode = riwayat.optString("metode", "TUNAI").uppercase()
            val jumlah = riwayat.optDouble("jumlah_bayar", 0.0)

            // 🚀 FIXED SINKRONISASI: Menangkap data berdasarkan key alias 'status_bayar' dari backend Laravel
            val status = riwayat.optString("status_bayar", "pending")

            tvNamaPaket.text = "$namaPaket ($kecepatan)"
            tvPeriode.text = "Periode: $bulan / $tahun"
            tvMetode.text = "Metode: $metode"

            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvJumlah.text = formatRupiah.format(jumlah).replace("Rp", "Rp ")

            // Mewarnai status transaksi secara dinamis berdasarkan respons API
            when (status.lowercase()) {
                "lunas", "success", "settlement" -> {
                    tvStatus.text = "Berhasil / Lunas"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                }
                "pending" -> {
                    tvStatus.text = "⏳ Pending / Menunggu"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                }
                else -> {
                    tvStatus.text = "Ditolak / Gagal"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                }
            }
        }
    }
}