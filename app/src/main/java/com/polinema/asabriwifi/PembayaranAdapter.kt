package com.polinema.asabriwifi

import android.graphics.Color
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PembayaranAdapter(private val listPembayaran: ArrayList<JSONObject>) : RecyclerView.Adapter<PembayaranAdapter.PembayaranViewHolder>() {

    var onItemClick: ((JSONObject) -> Unit)? = null

    // Properti global untuk menyimpan indeks item yang sedang ditekan lama oleh admin
    var positionTerpilih: Int = -1

    // Implementasi View.OnCreateContextMenuListener agar mendukung Context Menu bawaan Android
    inner class PembayaranViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        val tvNama = itemView.findViewById<TextView>(R.id.tvNamaPelangganBayar)
        val tvMetode = itemView.findViewById<TextView>(R.id.tvMetodeBayar)
        val tvJumlah = itemView.findViewById<TextView>(R.id.tvJumlahBayar)
        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatusBayar)

        init {
            // Daftarkan listener penahan sentuhan ke view item kartu
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            // Membuat struktur menu melayang saat kartu ditekan lama
            menu?.setHeaderTitle("Pilihan Aksi")
            // Parameter: (groupId, itemId, order, title) -> itemId 101 akan ditangkap oleh Fragment
            menu?.add(this.adapterPosition, 101, 0, "Lihat Detail Pembayaran")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PembayaranViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pembayaran, parent, false)
        return PembayaranViewHolder(view)
    }

    override fun onBindViewHolder(holder: PembayaranViewHolder, position: Int) {
        val bayar = listPembayaran[position]

        try {
            // Ambil nama user dari relasi ("with('user')") di Laravel
            val userObj = bayar.optJSONObject("user")
            holder.tvNama.text = userObj?.optString("name", "Pelanggan Dihapus") ?: "Pelanggan Dihapus"

            holder.tvMetode.text = "Metode: " + bayar.optString("metode", "Tunai").uppercase()
            holder.tvJumlah.text = "Rp " + bayar.optString("jumlah_bayar", "0")

            val status = bayar.optString("status", "menunggu").uppercase()
            holder.tvStatus.text = status

            // Mewarnai status transaksi
            when (status) {
                "LUNAS" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#C8E6C9"))
                }
                "DITOLAK" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#C62828"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FFCDD2"))
                }
                else -> { // Menunggu / Pending
                    holder.tvStatus.setTextColor(Color.parseColor("#F57F17"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF9C4"))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Tangkap posisi indeks item saat admin mulai menekan lama kartu sebelum menu muncul
        holder.itemView.setOnLongClickListener {
            positionTerpilih = holder.adapterPosition
            false // Biarkan return false agar event diteruskan ke onCreateContextMenu
        }

        holder.itemView.setOnClickListener { onItemClick?.invoke(bayar) }
    }

    override fun getItemCount(): Int = listPembayaran.size
}