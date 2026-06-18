package com.polinema.asabriwifi

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class HomePelangganFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvNotifBadge: TextView
    private lateinit var tvTotalHutang: TextView
    private lateinit var tvJatuhTempoTerdekat: TextView
    private lateinit var tvStatusPaket: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCountPaket: TextView
    private lateinit var tvCountTagihan: TextView
    private lateinit var btnKelolaLangganan: Button // 🚀 TAMBAHAN: Variabel global tombol kelola

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout spesifik fragment_home_pelanggan
        val view = inflater.inflate(R.layout.fragment_home_pelanggan, container, false)

        // Ikat Widget berdasarkan view root fragment
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvNotifBadge = view.findViewById(R.id.tvNotifBadge)
        tvTotalHutang = view.findViewById(R.id.tvTotalHutang)
        tvJatuhTempoTerdekat = view.findViewById(R.id.tvJatuhTempoTerdekat)
        tvStatusPaket = view.findViewById(R.id.tvStatusPaket)
        progressBar = view.findViewById(R.id.progressBarDashboard)
        tvCountPaket = view.findViewById(R.id.tvCountPaket)
        tvCountTagihan = view.findViewById(R.id.tvCountTagihan)
        btnKelolaLangganan = view.findViewById(R.id.btnKelolaLangganan) // 🚀 FIXED: Inisialisasi Tombol Baru

        // 🚀 ACTION KLIK: Teruskan aksi langsung ke Fragment Kelola Langganan Terpisah
        btnKelolaLangganan.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
            val idUser = sharedPreferences.getString("ID_USER", "") ?: ""

            val fragmentTujuan = LanggananPelangganFragment()
            val bundle = Bundle()
            bundle.putString("ARG_USER_ID", idUser.trim())
            fragmentTujuan.arguments = bundle

            // Dapatkan ID layout container secara dinamis dan aman
            val containerId = (view.parent as? View)?.id ?: R.id.fragment_container

            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(containerId, fragmentTujuan)
                addToBackStack(null) // Biarkan user bisa menekan tombol back HP untuk kembali ke Home
                commit()
            }
        }

        view.findViewById<Button>(R.id.btnBayar).setOnClickListener {
            // Alihkan otomatis ke menu tagihan yang ada di bottom nav
            val containerId = (view.parent as? View)?.id ?: R.id.fragment_container
            activity?.supportFragmentManager?.beginTransaction()?.replace(containerId, TagihanPelangganFragment())?.commit()
        }

        view.findViewById<Button>(R.id.btnKomplain).setOnClickListener {
            // Alihkan otomatis ke menu keluhan
            val containerId = (view.parent as? View)?.id ?: R.id.fragment_container
            activity?.supportFragmentManager?.beginTransaction()?.replace(containerId, KeluhanPelangganFragment())?.commit()
        }

        fetchDashboardData()

        return view
    }

    private fun fetchDashboardData() {
        progressBar.visibility = View.VISIBLE

        val sharedPreferences = requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val idUser = sharedPreferences.getString("ID_USER", "")

        val url = ApiConfig.BASE_URL + "pelanggan/dashboard?user_id=" + idUser

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)

                    // 1. Sinkronisasi Angka Statistik Atas
                    val totalHutang = jsonObject.optLong("tagihanBelumLunas", 0)
                    tvTotalHutang.text = String.format(Locale.US, "Rp %,d", totalHutang).replace(",", ".")
                    tvCountTagihan.text = if (totalHutang > 0) "1" else "0"

                    // 2. Pemetaan Jumlah Paket Aktif via Array
                    val arrayLangganan = jsonObject.getJSONArray("langgananAktif")
                    tvCountPaket.text = arrayLangganan.length().toString()

                    // 3. Update Status Peringatan Sisa Hari Jatuh Tempo
                    if (!jsonObject.isNull("tagihanAktif")) {
                        val tagihan = jsonObject.getJSONObject("tagihanAktif")
                        val sisaHari = tagihan.optInt("sisa_hari", 0)
                        val bulan = tagihan.optString("periode_bulan", "")
                        val tahun = tagihan.optString("periode_tahun", "")

                        tvJatuhTempoTerdekat.text = "Periode $bulan $tahun • Jatuh tempo $sisaHari hari lagi"
                    } else {
                        tvJatuhTempoTerdekat.text = "Semua Lunas! Terima kasih."
                    }

                    // 4. Looping List Konten Paket Layanan Aktif
                    if (arrayLangganan.length() > 0) {
                        var infoMekarPaket = ""
                        for (i in 0 until arrayLangganan.length()) {
                            val item = arrayLangganan.getJSONObject(i)
                            val paketLayanan = item.getJSONObject("paket_layanan")
                            val nama = paketLayanan.optString("nama_paket", "Paket Premium")
                            val speed = paketLayanan.optString("kecepatan", "0")
                            val harga = paketLayanan.optLong("harga", 0)

                            infoMekarPaket += "• $nama ($speed Mbps) - " + String.format(Locale.US, "Rp %,d/bln\n", harga).replace(",", ".")
                        }
                        tvStatusPaket.text = infoMekarPaket.trim()
                    } else {
                        tvStatusPaket.text = "Anda belum memiliki layanan internet aktif."
                    }

                    // 5. Badge Notifikasi Unread
                    val unreadNotif = jsonObject.optInt("unreadNotifCount", 0)
                    tvNotifBadge.text = "$unreadNotif Pesan Baru"
                    tvNotifBadge.visibility = if (unreadNotif > 0) View.VISIBLE else View.GONE

                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Eror parsing data sinkronisasi", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal terhubung ke dashboard web Laravel", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}