package com.polinema.asabriwifi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var tvPelangganAktif: TextView
    private lateinit var tvTagihanPending: TextView
    private lateinit var tvPemasukanHariIni: TextView
    private lateinit var tvPemasukanBulanIni: TextView
    private lateinit var btnRefresh: Button

    // Inisialisasi variabel menu Tindakan Cepat 3 kolom sesuai layout XML terbaru
    private lateinit var btnMenuLaporan: CardView
    private lateinit var btnMenuPelanggan: CardView
    private lateinit var btnMenuPengguna: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Hubungkan dengan layout fragment_dashboard.xml
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Inisialisasi komponen summary ringkasan data
        tvPelangganAktif = view.findViewById(R.id.tvPelangganAktif)
        tvTagihanPending = view.findViewById(R.id.tvTagihanPending)
        tvPemasukanHariIni = view.findViewById(R.id.tvPemasukanHariIni)
        tvPemasukanBulanIni = view.findViewById(R.id.tvPemasukanBulanIni)
        btnRefresh = view.findViewById(R.id.btnRefresh)

        // Inisialisasi komponen menu Tindakan Cepat kustom 3 kolom
        btnMenuLaporan = view.findViewById(R.id.btnMenuLaporan)
        btnMenuPelanggan = view.findViewById(R.id.btnMenuPelanggan)
        btnMenuPengguna = view.findViewById(R.id.btnMenuPengguna)

        // Set Listener untuk pindah halaman ke Laporan Kas Keuangan
        btnMenuLaporan.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(LaporanFragment())
        }

        // Set Listener untuk pindah halaman ke Kelola Pelanggan
        btnMenuPelanggan.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(PelangganFragment())
        }

        // Set Listener untuk pindah halaman ke Kelola User / Pengguna Sistem
        btnMenuPengguna.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(PenggunaFragment())
        }

        btnRefresh.setOnClickListener {
            if (isAdded) Toast.makeText(requireContext(), "Memperbarui data...", Toast.LENGTH_SHORT).show()
            fetchDashboardData()
        }

        // Ambil data pertama kali saat fragment dimuat
        fetchDashboardData()

        return view
    }

    private fun fetchDashboardData() {
        val url = ApiConfig.BASE_URL + "dashboard/admin"

        val jsonObjectRequest = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            Response.Listener { response ->
                try {
                    val pelangganAktif = response.optString("pelanggan_aktif", "0")
                    val tagihanBelumLunas = response.optString("tagihan_belum_lunas", "0")
                    val pemasukanHariIniRaw = response.optLong("pemasukan_hari_ini", 0)
                    val langgananAktif = response.optString("langganan_aktif", "0")

                    // Format nominal rupiah dengan ribuan titik (.)
                    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
                    val stringPemasukan = "Rp " + formatter.format(pemasukanHariIniRaw)

                    tvPelangganAktif.text = pelangganAktif
                    tvTagihanPending.text = tagihanBelumLunas
                    tvPemasukanHariIni.text = stringPemasukan
                    tvPemasukanBulanIni.text = langgananAktif

                } catch (e: JSONException) {
                    e.printStackTrace()
                    if (isAdded) Toast.makeText(requireContext(), "Gagal memproses data JSON", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                if (isAdded) Toast.makeText(requireContext(), "Gagal mengambil data Dashboard.", Toast.LENGTH_SHORT).show()
            }
        )

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
    }
}