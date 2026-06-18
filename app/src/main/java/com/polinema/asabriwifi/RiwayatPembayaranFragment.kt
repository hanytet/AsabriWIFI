package com.polinema.asabriwifi

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class RiwayatPembayaranFragment : Fragment() {

    private lateinit var rvRiwayat: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvKosong: TextView
    private val listRiwayat = ArrayList<JSONObject>()
    private lateinit var adapter: RiwayatPembayaranAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_riwayat_pembayaran, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvRiwayat = view.findViewById(R.id.rvRiwayatPembayaran)
        progressBar = view.findViewById(R.id.progressBarRiwayat)
        tvKosong = view.findViewById(R.id.tvRiwayatKosong)

        rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        adapter = RiwayatPembayaranAdapter(listRiwayat)
        rvRiwayat.adapter = adapter

        // Jalankan pemuatan data di sini setelah view benar-benar siap dan menempel pada activity
        loadRiwayatPembayaran()
    }

    private fun loadRiwayatPembayaran() {
        progressBar.visibility = View.VISIBLE
        tvKosong.visibility = View.GONE

        var idUser = arguments?.getString("ARG_USER_ID") ?: ""

        if (idUser.isEmpty() || idUser == "null") {
            val sharedPreferences = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
            idUser = sharedPreferences.getString("ID_USER", "") ?: ""
        }

        idUser = idUser.trim()

        if (idUser.isEmpty() || idUser == "null" || idUser == "0") {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Sesi ID tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        val url = ApiConfig.BASE_URL + "tagihan?aksi=riwayat&user_id=$idUser"
        Log.d("AsabriError", "Menembak URL Riwayat: $url")

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listRiwayat.clear()
                    val jsonObject = JSONObject(response)

                    if (jsonObject.optString("status") == "berhasil") {
                        val arr = jsonObject.optJSONArray("data")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                listRiwayat.add(arr.getJSONObject(i))
                            }
                        }

                        if (listRiwayat.isEmpty()) {
                            tvKosong.visibility = View.VISIBLE
                        }
                    } else {
                        val pesan = jsonObject.optString("pesan", "Tidak ada riwayat")
                        Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
                        tvKosong.visibility = View.VISIBLE
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal mengurai data riwayat", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                val statusCode = error.networkResponse?.statusCode
                Log.e("AsabriError", "Riwayat Volley Error Status Code: $statusCode | Msg: ${error.message}")
                Toast.makeText(requireContext(), "Gagal terhubung ke server (Code: $statusCode)", Toast.LENGTH_SHORT).show()
            }
        )
        stringRequest.setShouldCache(false)
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}