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

class PaketLandingAdapter(
    private val listPaket: ArrayList<JSONObject>,
    private val onPesanClickListener: (JSONObject) -> Unit
) : RecyclerView.Adapter<PaketLandingAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama = itemView.findViewById<TextView>(R.id.tvNamaPaketLanding)
        val tvKecepatan = itemView.findViewById<TextView>(R.id.tvKecepatanLanding)
        val tvHarga = itemView.findViewById<TextView>(R.id.tvHargaLanding)
        val tvDeskripsi = itemView.findViewById<TextView>(R.id.tvDeskripsiLanding)
        val btnPesan = itemView.findViewById<Button>(R.id.btnPilihPaketLanding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_paket_landing, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listPaket[position]
        try {
            holder.tvNama.text = item.optString("nama_paket", "-")
            holder.tvKecepatan.text = item.optString("kecepatan", "-") + " Mbps"
            holder.tvDeskripsi.text = item.optString("deskripsi", "Kuota Unlimited Tanpa Batasan")

            val hargaRaw = item.optLong("harga", 0)
            val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
            holder.tvHarga.text = "Rp " + formatter.format(hargaRaw) + " / bulan"

            holder.btnPesan.setOnClickListener { onPesanClickListener(item) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun getItemCount(): Int = listPaket.size
}