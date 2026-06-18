package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject

class PaketPelangganFragment : Fragment() {

    private lateinit var rvPaket: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val listPaket = ArrayList<JSONObject>()
    private lateinit var adapter: PaketPelangganAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_paket_pelanggan, container, false)

        rvPaket = view.findViewById(R.id.rvPaketPelanggan)
        progressBar = view.findViewById(R.id.progressBarPaket)

        rvPaket.layoutManager = LinearLayoutManager(requireContext())
        adapter = PaketPelangganAdapter(listPaket) { paketObj ->
            tampilkanKonfirmasiLangganan(paketObj)
        }
        rvPaket.adapter = adapter

        loadDaftarPaket()
        return view
    }

    private fun loadDaftarPaket() {
        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "paket?aksi=tampil"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listPaket.clear()
                    val jsonObject = JSONObject(response)

                    if (jsonObject.has("data")) {
                        val arr = jsonObject.getJSONArray("data")
                        for (i in 0 until arr.length()) {
                            listPaket.add(arr.getJSONObject(i))
                        }
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memproses data paket", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat paket dari server", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun tampilkanKonfirmasiLangganan(paket: JSONObject) {
        val paketId = paket.optString("id")
        val namaPaket = paket.optString("nama_paket")

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Paket Layanan")
            .setMessage("Apakah Tuan ingin menambah paket $namaPaket ke dalam daftar langganan aktif Anda?")
            .setCancelable(false)
            .setPositiveButton("Ya, Tambah & Bayar") { _, _ ->
                eksekusiBerlangganan(paketId)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiBerlangganan(paketId: String) {
        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "paket"

        val sharedPreferences = requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val idUser = sharedPreferences.getString("ID_USER", "")

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    val status = jsonObject.optString("status")
                    if (status == "berhasil") {
                        val redirectUrl = jsonObject.optString("redirect_url")

                        if (!redirectUrl.isNullOrEmpty()) {
                            Toast.makeText(requireContext(), "Mengalihkan ke Midtrans...", Toast.LENGTH_SHORT).show()

                            // 🚀 FIXED 1: Luncurkan gerbang kasir Midtrans Snap secara mandiri di Task Baru
                            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(redirectUrl))
                            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(browserIntent)

                            // 🚀 FIXED 2: JANGAN panggil finish() atau ganti activity secara instan di sini!
                            // Biarkan user menyelesaikan transaksi di browser. Ketika mereka menekan tombol back,
                            // mereka akan kembali ke aplikasi secara alami.
                        } else {
                            Toast.makeText(requireContext(), "Tautan pembayaran tidak ditemukan dari server", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val pesan = jsonObject.optString("pesan", "Gagal memproses langganan")
                        Toast.makeText(requireContext(), pesan, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memproses respons transaksi", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                val response = error.networkResponse
                if (response != null) {
                    try {
                        // 🚀 DETEKTIF EROR: Bongkar pesan penolakan asli jika token gagal digenerate oleh Laravel
                        val dataEror = String(response.data)
                        val jsonEror = JSONObject(dataEror)
                        val pesanEror = jsonEror.optString("pesan", "Eror Kode: ${response.statusCode}")

                        AlertDialog.Builder(requireContext())
                            .setTitle("Midtrans Gagal Merespons")
                            .setMessage(pesanEror)
                            .setPositiveButton("Tutup", null)
                            .show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Server Error (${response.statusCode})", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal memproses transaksi. Periksa koneksi lokal!", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "aksi" to "berlangganan",
                    "user_id" to idUser.toString(),
                    "paket_id" to paketId
                )
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }
}