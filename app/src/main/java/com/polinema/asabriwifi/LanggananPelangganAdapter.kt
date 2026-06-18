package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class LanggananPelangganAdapter(
    private val listLangganan: List<JSONObject>,
    private val onBerhentiClick: (JSONObject) -> Unit
) : RecyclerView.Adapter<LanggananPelangganAdapter.LanggananViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanggananViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_langganan_aktif, parent, false)
        return LanggananViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanggananViewHolder, position: Int) {
        holder.bind(listLangganan[position], onBerhentiClick)
    }

    override fun getItemCount(): Int = listLangganan.size

    class LanggananViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNama: TextView = itemView.findViewById(R.id.tvItemNamaPaketAktif)
        private val tvMulai: TextView = itemView.findViewById(R.id.tvItemTanggalMulai)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvItemStatusPaket)
        private val btnBerhenti: Button = itemView.findViewById(R.id.btnItemBerhentiLayanan)

        fun bind(obj: JSONObject, onBerhenti: (JSONObject) -> Unit) {
            val nama = obj.optString("nama_paket")
            val kecepatan = obj.optString("kecepatan")
            val tglMulai = obj.optString("tanggal_mulai")
            val status = obj.optString("status_langganan")

            tvNama.text = "$nama ($kecepatan)"
            tvMulai.text = "Mulai Berlangganan: $tglMulai"
            tvStatus.text = "Status Paket: ${status.uppercase()}"

            btnBerhenti.setOnClickListener { onBerhenti(obj) }
        }
    }
}