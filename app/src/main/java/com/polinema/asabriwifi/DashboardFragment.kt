package com.polinema.asabriwifi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException

class DashboardFragment : Fragment() {

    private lateinit var tvPelangganAktif: TextView
    private lateinit var tvTagihanPending: TextView
    private lateinit var tvPemasukanHariIni: TextView
    private lateinit var tvPemasukanBulanIni: TextView
    private lateinit var btnRefresh: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Hubungkan dengan layout fragment_dashboard.xml
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvPelangganAktif = view.findViewById(R.id.tvPelangganAktif)
        tvTagihanPending = view.findViewById(R.id.tvTagihanPending)
        tvPemasukanHariIni = view.findViewById(R.id.tvPemasukanHariIni)
        tvPemasukanBulanIni = view.findViewById(R.id.tvPemasukanBulanIni)
        btnRefresh = view.findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener {
            Toast.makeText(requireContext(), "Memperbarui data...", Toast.LENGTH_SHORT).show()
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
                    val pelangganAktif = response.getString("pelanggan_aktif")
                    val tagihanBelumLunas = response.getString("tagihan_belum_lunas")
                    val pemasukanHariIni = response.getString("pemasukan_hari_ini")
                    val langgananAktif = response.getString("langganan_aktif")

                    tvPelangganAktif.text = pelangganAktif
                    tvTagihanPending.text = tagihanBelumLunas
                    tvPemasukanHariIni.text = "Rp $pemasukanHariIni"
                    tvPemasukanBulanIni.text = langgananAktif

                } catch (e: JSONException) {
                    e.printStackTrace()
                    // Fragment menggunakan requireContext() bukan 'this'
                    if (isAdded) Toast.makeText(requireContext(), "Gagal memproses data JSON", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                if (isAdded) Toast.makeText(requireContext(), "Gagal mengambil data Dashboard.", Toast.LENGTH_SHORT).show()
            }
        )

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
    }
}