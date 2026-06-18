package com.polinema.asabriwifi

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
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
    private val onBayarClick: (JSONObject) -> Unit,
    private val onBatalkanClick: (JSONObject) -> Unit // 🚀 TAMBAHAN: Callback untuk pembatalan
) : RecyclerView.Adapter<TagihanPelangganAdapter.TagihanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagihanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tagihan_pelanggan, parent, false)
        return TagihanViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagihanViewHolder, position: Int) {
        val tagihan = listTagihan[position]
        holder.bind(tagihan, onBayarClick, onBatalkanClick)
    }

    override fun getItemCount(): Int = listTagihan.size

    class TagihanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {
        private val tvPeriode: TextView = itemView.findViewById(R.id.tvItemPeriode)
        private val tvNamaPaket: TextView = itemView.findViewById(R.id.tvItemNamaPaket)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvItemStatus)
        private val tvJumlah: TextView = itemView.findViewById(R.id.tvItemJumlahTagihan)
        private val btnBayar: Button = itemView.findViewById(R.id.btnItemBayarTagihan)

        private lateinit var currentTagihan: JSONObject
        private lateinit var onBatalkanCallback: (JSONObject) -> Unit

        init {
            // 🚀 Daftarkan listener long click (Context Menu) ke root view item ini
            itemView.setOnCreateContextMenuListener(this)
        }

        fun bind(tagihan: JSONObject, onBayar: (JSONObject) -> Unit, onBatalkan: (JSONObject) -> Unit) {
            this.currentTagihan = tagihan
            this.onBatalkanCallback = onBatalkan

            val namaPaket = tagihan.optString("nama_paket")
            val kecepatan = tagihan.optString("kecepatan")
            val bulan = tagihan.optString("periode_bulan")
            val tahun = tagihan.optString("periode_tahun")
            val status = tagihan.optString("status")
            val jumlah = tagihan.optDouble("jumlah_tagihan", 0.0)

            tvPeriode.text = "Periode: $bulan / $tahun"
            tvNamaPaket.text = "$namaPaket ($kecepatan)"

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

            btnBayar.setOnClickListener { onBayar(tagihan) }
        }

        // 🚀 MEMBUAT POP-UP CONTEXT MENU SAAT DITAHAN LAMA
        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            val status = currentTagihan.optString("status")

            // Batalkan langganan hanya boleh muncul jika statusnya memang belum dibayar
            if (status == "belum_lunas") {
                menu?.setHeaderTitle("Opsi Tagihan")
                val menuBatalkan = menu?.add(Menu.NONE, 1, 1, "Batalkan Langganan")

                menuBatalkan?.setOnMenuItemClickListener {
                    onBatalkanCallback(currentTagihan)
                    true
                }
            }
        }
    }
}