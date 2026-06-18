package com.polinema.asabriwifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeknisiTugasAdapter(
    private val listTugas: List<KeluhanTeknisi>,
    private val onBerangkatClick: (KeluhanTeknisi) -> Unit,
    private val onSelesaiClick: (KeluhanTeknisi, String) -> Unit
) : RecyclerView.Adapter<TeknisiTugasAdapter.TugasViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TugasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tugas_teknisi, parent, false)
        return TugasViewHolder(view)
    }

    override fun onBindViewHolder(holder: TugasViewHolder, position: Int) {
        holder.bind(listTugas[position], onBerangkatClick, onSelesaiClick)
    }

    override fun getItemCount(): Int = listTugas.size

    class TugasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTanggal: TextView = itemView.findViewById(R.id.tvTugasTanggal)
        private val tvPelanggan: TextView = itemView.findViewById(R.id.tvTugasPelanggan)
        private val tvDetailKeluhan: TextView = itemView.findViewById(R.id.tvTugasDetail)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvTugasStatus)
        private val btnBerangkat: Button = itemView.findViewById(R.id.btnTugasBerangkat)
        private val btnSelesai: Button = itemView.findViewById(R.id.btnTugasSelesai)
        private val layoutSelesaiAction: LinearLayout = itemView.findViewById(R.id.layoutSelesaiAction)
        private val etCatatanSelesai: EditText = itemView.findViewById(R.id.etCatatanSelesai)
        private val tvLabelSelesai: TextView = itemView.findViewById(R.id.tvLabelSelesai)

        fun bind(
            tugas: KeluhanTeknisi,
            onBerangkat: (KeluhanTeknisi) -> Unit,
            onSelesai: (KeluhanTeknisi, String) -> Unit
        ) {
            tvTanggal.text = "${tugas.hariKeluhan}, ${tugas.tanggalKeluhan}"
            tvPelanggan.text = "${tugas.namaPelanggan}\nHp: ${tugas.noHp}\nAlamat: ${tugas.alamatTujuan}"
            tvDetailKeluhan.text = "[${tugas.kategori}]\n${tugas.isiKeluhan}"
            tvStatus.text = tugas.statusLabel

            // Sembunyikan semua komponen aksi bawaan
            btnBerangkat.visibility = View.GONE
            layoutSelesaiAction.visibility = View.GONE
            tvLabelSelesai.visibility = View.GONE

            // Kondisi visibilitas tombol berdasarkan status keluhan dari Laravel
            when (tugas.status) {
                "teknisi_ditugaskan" -> {
                    btnBerangkat.visibility = View.VISIBLE
                    layoutSelesaiAction.visibility = View.VISIBLE
                }
                "teknisi_berangkat" -> {
                    layoutSelesaiAction.visibility = View.VISIBLE
                }
                "selesai" -> {
                    tvLabelSelesai.visibility = View.VISIBLE
                }
                else -> {
                    // Jika null / menunggu penugasan admin
                    tvLabelSelesai.text = "Menunggu Penugasan Admin"
                    tvLabelSelesai.visibility = View.VISIBLE
                }
            }

            btnBerangkat.setOnClickListener { onBerangkat(tugas) }
            btnSelesai.setOnClickListener {
                val catatan = etCatatanSelesai.text.toString().trim()
                onSelesai(tugas, catatan)
            }
        }
    }
}