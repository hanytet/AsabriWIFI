package com.polinema.asabriwifi

import android.graphics.Color
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class MonitoringAdapter(private val listTunggakan: ArrayList<JSONObject>) : RecyclerView.Adapter<MonitoringAdapter.MonitoringViewHolder>() {

    var positionTerpilih: Int = -1

    inner class MonitoringViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        val tvNama = itemView.findViewById<TextView>(R.id.tvNamaPelanggan)
        val tvPaket = itemView.findViewById<TextView>(R.id.tvNamaPaket)
        val tvJumlah = itemView.findViewById<TextView>(R.id.tvJumlahTagihan)
        val tvJatuhTempo = itemView.findViewById<TextView>(R.id.tvJatuhTempo)
        val tvKeterlambatan = itemView.findViewById<TextView>(R.id.tvKeterlambatan)

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.setHeaderTitle("Aksi Tunggakan")
            // ItemId 301 untuk memicu kirim notifikasi pengingat
            menu?.add(this.adapterPosition, 301, 0, "Kirim Notif Pengingat")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitoringViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_monitoring, parent, false)
        return MonitoringViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonitoringViewHolder, position: Int) {
        val item = listTunggakan[position]

        try {
            holder.tvNama.text = item.optString("nama", "-")
            holder.tvPaket.text = item.optString("paket", "-")
            holder.tvJumlah.text = "Rp " + item.optString("jumlah", "0")
            holder.tvJatuhTempo.text = item.optString("jatuh_tempo", "-")

            val late = item.optInt("keterlambatan", 0)
            if (late > 0) {
                holder.tvKeterlambatan.text = "$late Hari"
                holder.tvKeterlambatan.visibility = View.VISIBLE
            } else {
                holder.tvKeterlambatan.visibility = View.GONE
            }

            // Atur warna badge dinamis berdasar kiriman JSON Laravel Anda
            when (item.optString("badge", "green")) {
                "red" -> {
                    holder.tvJatuhTempo.setBackgroundColor(Color.parseColor("#FFCDD2"))
                    holder.tvJatuhTempo.setTextColor(Color.parseColor("#C62828"))
                }
                "yellow" -> {
                    holder.tvJatuhTempo.setBackgroundColor(Color.parseColor("#FFF9C4"))
                    holder.tvJatuhTempo.setTextColor(Color.parseColor("#F57F17"))
                }
                else -> {
                    holder.tvJatuhTempo.setBackgroundColor(Color.parseColor("#C8E6C9"))
                    holder.tvJatuhTempo.setTextColor(Color.parseColor("#2E7D32"))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        holder.itemView.setOnLongClickListener {
            positionTerpilih = holder.adapterPosition
            false
        }
    }

    override fun getItemCount(): Int = listTunggakan.size
}