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
    private lateinit var btnKelolaLangganan: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_pelanggan, container, false)

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvNotifBadge = view.findViewById(R.id.tvNotifBadge)
        tvTotalHutang = view.findViewById(R.id.tvTotalHutang)
        tvJatuhTempoTerdekat = view.findViewById(R.id.tvJatuhTempoTerdekat)
        tvStatusPaket = view.findViewById(R.id.tvStatusPaket)
        progressBar = view.findViewById(R.id.progressBarDashboard)
        tvCountPaket = view.findViewById(R.id.tvCountPaket)
        tvCountTagihan = view.findViewById(R.id.tvCountTagihan)
        btnKelolaLangganan = view.findViewById(R.id.btnKelolaLangganan)

        // 🚀 AMBIL DATA DARI SHREDPREFS YANG AKURAT
        val sharedPreferences = requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val namaUser = sharedPreferences.getString("NAMA_USER", "Pelanggan") ?: "Pelanggan"

        if (namaUser != "Pelanggan" && namaUser.isNotEmpty()) {
            tvWelcome.text = "Halo, $namaUser!"
        } else {
            tvWelcome.text = "Halo, Pelanggan!"
        }

        btnKelolaLangganan.setOnClickListener {
            val idUser = sharedPreferences.getString("ID_USER", "") ?: ""

            val fragmentTujuan = LanggananPelangganFragment()
            val bundle = Bundle()
            bundle.putString("ARG_USER_ID", idUser.trim())
            fragmentTujuan.arguments = bundle

            val containerId = (view.parent as? View)?.id ?: R.id.fragment_container

            activity?.supportFragmentManager?.beginTransaction()?.apply {
                replace(containerId, fragmentTujuan)
                addToBackStack(null)
                commit()
            }
        }

        view.findViewById<Button>(R.id.btnBayar).setOnClickListener {
            val containerId = (view.parent as? View)?.id ?: R.id.fragment_container
            activity?.supportFragmentManager?.beginTransaction()?.replace(containerId, TagihanPelangganFragment())?.commit()
        }

        view.findViewById<Button>(R.id.btnKomplain).setOnClickListener {
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

                    // 🚀 FORCE FIX: Ekstraksi nama user dari struktur array data dashboard Laravel secara menyeluruh
                    if (jsonObject.has("user")) {
                        val userObj = jsonObject.getJSONObject("user")
                        val namaDb = userObj.optString("name", userObj.optString("nama", ""))
                        if (namaDb.isNotEmpty() && namaDb != "null") {
                            tvWelcome.text = "Halo, $namaDb!"
                            sharedPreferences.edit().putString("NAMA_USER", namaDb).apply()
                        }
                    } else if (jsonObject.has("pelanggan")) {
                        val pelObj = jsonObject.getJSONObject("pelanggan")
                        val namaDb = pelObj.optString("name", pelObj.optString("nama", ""))
                        if (namaDb.isNotEmpty() && namaDb != "null") {
                            tvWelcome.text = "Halo, $namaDb!"
                            sharedPreferences.edit().putString("NAMA_USER", namaDb).apply()
                        }
                    }

                    // JIKA SANGKUT: Coba deteksi nama dari relasi langgananAktif yang membawa objek user
                    if (tvWelcome.text.contains("Pelanggan")) {
                        val arrayLangganan = jsonObject.optJSONArray("langgananAktif")
                        if (arrayLangganan != null && arrayLangganan.length() > 0) {
                            val firstItem = arrayLangganan.getJSONObject(0)
                            if (firstItem.has("pelanggan")) {
                                val userRelasi = firstItem.getJSONObject("pelanggan")
                                val namaRelasi = userRelasi.optString("name", userRelasi.optString("nama", ""))
                                if (namaRelasi.isNotEmpty() && namaRelasi != "null") {
                                    tvWelcome.text = "Halo, $namaRelasi!"
                                    sharedPreferences.edit().putString("NAMA_USER", namaRelasi).apply()
                                }
                            }
                        }
                    }

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