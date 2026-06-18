package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject

class TagihanPelangganFragment : Fragment() {

    private lateinit var rvTagihan: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvKosong: TextView
    private val listTagihan = ArrayList<JSONObject>()
    private lateinit var adapter: TagihanPelangganAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tagihan_pelanggan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTagihan = view.findViewById(R.id.rvTagihanPelanggan)
        progressBar = view.findViewById(R.id.progressBarTagihan)
        tvKosong = view.findViewById(R.id.tvTagihanKosong)

        rvTagihan.layoutManager = LinearLayoutManager(requireContext())

        adapter = TagihanPelangganAdapter(
            listTagihan,
            { tagihanObj -> tampilkanPilihanPembayaran(tagihanObj) },
            { tagihanObj -> tampilkanKonfirmasiPembatalan(tagihanObj) }
        )
        rvTagihan.adapter = adapter

        // Jalankan pemuatan data di sini agar siklus hidup komponen lebih aman
        loadDaftarTagihan()
    }

    private fun loadDaftarTagihan() {
        progressBar.visibility = View.VISIBLE
        tvKosong.visibility = View.GONE

        var idUser = arguments?.getString("ARG_USER_ID") ?: ""

        if (idUser.isEmpty() || idUser == "null") {
            val sharedPreferences = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
            idUser = sharedPreferences.getString("ID_USER", "") ?: ""
        }

        idUser = idUser.trim()
        Log.d("AsabriDebug", "ID User yang digunakan di Tagihan Fragment: '$idUser'")

        if (idUser.isEmpty() || idUser == "null" || idUser == "0") {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Sesi ID tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        // FIXED SINKRONISASI: Ubah "tagihan-pelanggan" menjadi "tagihan" agar cocok dengan route Laravel
        val url = ApiConfig.BASE_URL + "tagihan?aksi=tampil&user_id=$idUser"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listTagihan.clear()
                    val jsonObject = JSONObject(response)

                    if (jsonObject.optString("status") == "berhasil") {
                        val arr = jsonObject.optJSONArray("data")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                listTagihan.add(arr.getJSONObject(i))
                            }
                        }

                        if (listTagihan.isEmpty()) {
                            tvKosong.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(requireContext(), jsonObject.optString("pesan", "Tidak ada tagihan"), Toast.LENGTH_SHORT).show()
                        tvKosong.visibility = View.VISIBLE
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memproses data tagihan", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                val statusCode = error.networkResponse?.statusCode
                Log.e("AsabriError", "Tagihan Volley Error Status Code: $statusCode")
                Toast.makeText(requireContext(), "Gagal memuat tagihan dari server (Code: $statusCode)", Toast.LENGTH_SHORT).show()
            }
        )
        stringRequest.setShouldCache(false)
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun tampilkanPilihanPembayaran(tagihan: JSONObject) {
        val tagihanId = tagihan.optString("id")
        val namaPaket = tagihan.optString("nama_paket")
        val jumlah = tagihan.optString("jumlah_tagihan")

        val pilihan = arrayOf("Bayar Otomatis (Midtrans / QRIS)", "Batal")

        AlertDialog.Builder(requireContext())
            .setTitle("Bayar Tagihan #$tagihanId")
            .setMessage("Paket: $namaPaket\nTotal: Rp $jumlah")
            .setItems(pilihan) { dialog, item ->
                when (item) {
                    0 -> eksekusiBayarMidtrans(tagihanId)
                    1 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun tampilkanKonfirmasiPembatalan(tagihan: JSONObject) {
        val langgananId = tagihan.optString("langganan_id")
        val namaPaket = tagihan.optString("nama_paket")

        AlertDialog.Builder(requireContext())
            .setTitle("Batalkan Langganan WiFi")
            .setMessage("Apakah Tuan Rihan yakin ingin menghentikan dan membatalkan paket $namaPaket?")
            .setCancelable(false)
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                progressBar.visibility = View.VISIBLE

                // FIXED SINKRONISASI: Menggunakan rute "tagihan"
                val url = ApiConfig.BASE_URL + "tagihan"

                val postRequest = object : StringRequest(Method.POST, url,
                    { response ->
                        progressBar.visibility = View.GONE
                        try {
                            val jsonObject = JSONObject(response)
                            if (jsonObject.optString("status") == "berhasil") {
                                Toast.makeText(requireContext(), jsonObject.optString("pesan"), Toast.LENGTH_SHORT).show()
                                loadDaftarTagihan()
                            } else {
                                val pesanGagal = jsonObject.optString("pesan", "Gagal membatalkan paket")
                                Toast.makeText(requireContext(), pesanGagal, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(requireContext(), "Gagal memproses respons server", Toast.LENGTH_SHORT).show()
                        }
                    },
                    { error ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        return hashMapOf(
                            "aksi" to "batal",
                            "langganan_id" to langgananId
                        )
                    }
                }
                VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
            }
            .setNegativeButton("Kembali", null)
            .show()
    }

    private fun eksekusiBayarMidtrans(tagihanId: String) {
        var idUser = arguments?.getString("ARG_USER_ID") ?: ""
        if (idUser.isEmpty()) {
            val sharedPreferences = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
            idUser = sharedPreferences.getString("ID_USER", "") ?: ""
        }

        if (idUser.isEmpty()) return

        progressBar.visibility = View.VISIBLE

        // FIXED SINKRONISASI: Menggunakan rute "tagihan"
        val url = ApiConfig.BASE_URL + "tagihan"

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "berhasil") {
                        val redirectUrl = jsonObject.optString("redirect_url")

                        if (!redirectUrl.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), "Mengalihkan ke Midtrans...", Toast.LENGTH_SHORT).show()

                            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(redirectUrl))
                            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(browserIntent)
                        } else {
                            Toast.makeText(requireContext(), "Tautan pembayaran kosong", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val pesan = jsonObject.optString("pesan", "Gagal memproses pembayaran")
                        Toast.makeText(requireContext(), pesan, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memproses respons server", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "aksi" to "midtrans",
                    "user_id" to idUser,
                    "tagihan_id" to tagihanId
                )
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }
}