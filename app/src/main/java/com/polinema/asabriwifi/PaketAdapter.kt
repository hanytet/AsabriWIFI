package com.polinema.asabriwifi

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PaketAdapter(private val listPaket: ArrayList<JSONObject>) : RecyclerView.Adapter<PaketAdapter.PaketViewHolder>() {

    var onItemClick: ((JSONObject) -> Unit)? = null

    // Variabel penampung indeks item yang sedang ditekan lama oleh admin
    var positionTerpilih: Int = -1

    // 1. IMPLEMENTASIKAN View.OnCreateContextMenuListener UNTUK CONTEXT MENU NATIVE
    inner class PaketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        val tvNamaPaket: TextView = itemView.findViewById(R.id.tvNamaPaket)
        val tvHargaPaket: TextView = itemView.findViewById(R.id.tvHargaPaket)
        val tvDeskripsiPaket: TextView = itemView.findViewById(R.id.tvDeskripsiPaket)

        init {
            // Daftarkan komponen layout agar peka terhadap long click Context Menu
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.setHeaderTitle("Kelola Paket")
            // Parameter: (groupId, itemId, order, title) -> Ditangkap oleh onContextItemSelected di Fragment
            menu?.add(this.adapterPosition, 201, 0, "Edit Paket")
            menu?.add(this.adapterPosition, 202, 1, "Hapus Paket")
        }
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
            holder.tvDeskripsiPaket.text = "$kecepatan| $deskripsi"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. TANGKAP INDEKS POSISI SAAT ITEM MULAI DITAHAN LAMA SEBELUM MENU MUNCUL
        holder.itemView.setOnLongClickListener {
            positionTerpilih = holder.adapterPosition
            false // Kembalikan false agar event dilanjutkan ke onCreateContextMenu resmi Android
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(paket)
        }
    }

    override fun getItemCount(): Int {
        return listPaket.size
    }
}