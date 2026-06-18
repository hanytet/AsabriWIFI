package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class TagihanPelangganAdapter(
    private val listTagihan: List<JSONObject>,
    private val onItemClick: (JSONObject) -> Unit
) : RecyclerView.Adapter<TagihanPelangganAdapter.TagihanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagihanViewHolder {
        // Menggunakan layout item tagihan bawaan proyek Tuan jika sudah ada,
        // atau Tuan bisa menyesuaikannya dengan layout item yang Tuan miliki.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tagihan_pelanggan, parent, false)
        return TagihanViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagihanViewHolder, position: Int) {
        val tagihan = listTagihan[position]
        holder.bind(tagihan, onItemClick)
    }

    override fun getItemCount(): Int = listTagihan.size

    class TagihanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPeriode: TextView = itemView.findViewById(R.id.tvItemPeriode)
        private val tvNamaPaket: TextView = itemView.findViewById(R.id.tvItemNamaPaket)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvItemStatus)
        private val tvJumlah: TextView = itemView.findViewById(R.id.tvItemJumlahTagihan)
        private val btnBayar: Button = itemView.findViewById(R.id.btnItemBayarTagihan)

        fun bind(tagihan: JSONObject, onClick: (JSONObject) -> Unit) {
            val id = tagihan.optString("id")
            val namaPaket = tagihan.optString("nama_paket")
            val kecepatan = tagihan.optString("kecepatan")
            val bulan = tagihan.optString("periode_bulan")
            val tahun = tagihan.optString("periode_tahun")
            val status = tagihan.optString("status")
            val jumlah = tagihan.optDouble("jumlah_tagihan", 0.0)

            tvPeriode.text = "Periode: $bulan / $tahun"
            tvNamaPaket.text = "$namaPaket ($kecepatan)"

            // Format Rupiah Indonesia
            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvJumlah.text = formatRupiah.format(jumlah).replace("Rp", "Rp ")

            if (status == "belum_lunas") {
                tvStatus.text = "Belum Lunas"
                tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                btnBayar.visibility = View.VISIBLE
            } else {
                tvStatus.text = "⏳ Menunggu Verifikasi"
                tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                btnBayar.visibility = View.GONE
            }

            btnBayar.setOnClickListener { onClick(tagihan) }
        }
    }
}