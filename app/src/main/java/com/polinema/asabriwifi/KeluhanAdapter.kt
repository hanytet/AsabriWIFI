package com.polinema.asabriwifi

import android.graphics.Color
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class KeluhanAdapter(private val listKeluhan: ArrayList<JSONObject>) : RecyclerView.Adapter<KeluhanAdapter.KeluhanViewHolder>() {

    var positionTerpilih: Int = -1

    inner class KeluhanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        val tvKategori = itemView.findViewById<TextView>(R.id.itemKategoriKeluhan)
        val tvNama = itemView.findViewById<TextView>(R.id.itemNamaPelanggan)
        val tvStatus = itemView.findViewById<TextView>(R.id.itemStatusKeluhan)
        val tvIsi = itemView.findViewById<TextView>(R.id.itemIsiKeluhan)
        val tvTeknisi = itemView.findViewById<TextView>(R.id.itemInfoTeknisi)

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.setHeaderTitle("Opsi Tindakan Keluhan")
            menu?.add(this.adapterPosition, 401, 0, "Proses / Ubah Status Keluhan")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeluhanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_keluhan, parent, false)
        return KeluhanViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeluhanViewHolder, position: Int) {
        val item = listKeluhan[position]

        try {
            holder.tvKategori.text = item.optString("kategori", "Keluhan")
            holder.tvNama.text = "Oleh: " + item.optString("nama", "-")
            holder.tvIsi.text = item.optString("isi_keluhan", "-")

            val namaTeknisi = item.optString("nama_teknisi", "")
            holder.tvTeknisi.text = if (namaTeknisi.isNotEmpty() && namaTeknisi != "null") "Teknisi: $namaTeknisi" else "Teknisi: Belum ditugaskan"

            val status = item.optString("status", "baru").lowercase()
            holder.tvStatus.text = status.replace("_", " ").uppercase()

            // Pewarnaan status badge dinamis
            when (status) {
                "baru" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#F57F17"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF9C4"))
                }
                "selesai" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#C8E6C9"))
                }
                "ditolak" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#C62828"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FFCDD2"))
                }
                else -> { // diterima, teknisi_ditugaskan, teknisi_berangkat
                    holder.tvStatus.setTextColor(Color.parseColor("#0D47A1"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#BBDEFB"))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        holder.itemView.setOnLongClickListener {
            positionTerpilih = holder.adapterPosition
            false
        }
    }

    override fun getItemCount(): Int = listKeluhan.size
}