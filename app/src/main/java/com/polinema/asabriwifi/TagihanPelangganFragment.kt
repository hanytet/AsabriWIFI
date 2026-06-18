package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    private lateinit var adapter: TagihanPelangganAdapter // Pastikan Tuan sudah membuat ViewHolder Adapter-nya

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tagihan_pelanggan, container, false)

        rvTagihan = view.findViewById(R.id.rvTagihanPelanggan)
        progressBar = view.findViewById(R.id.progressBarTagihan)
        tvKosong = view.findViewById(R.id.tvTagihanKosong)

        rvTagihan.layoutManager = LinearLayoutManager(requireContext())

        // Inisialisasi adapter dengan callback klik item untuk bayar
        adapter = TagihanPelangganAdapter(listTagihan) { tagihanObj ->
            tampilkanPilihanPembayaran(tagihanObj)
        }
        rvTagihan.adapter = adapter

        loadDaftarTagihan()
        return view
    }

    private fun loadDaftarTagihan() {
        progressBar.visibility = View.VISIBLE
        tvKosong.visibility = View.GONE

        val sharedPreferences = requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val idUser = sharedPreferences.getString("ID_USER", "")

        // Menggunakan endpoint tunggal API yang baru kita buat
        val url = ApiConfig.BASE_URL + "tagihan-pelanggan?aksi=tampil&user_id=$idUser"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listTagihan.clear()
                    val jsonObject = JSONObject(response)

                    if (jsonObject.optString("status") == "berhasil") {
                        val arr = jsonObject.getJSONArray("data")
                        for (i in 0 until arr.length()) {
                            listTagihan.add(arr.getJSONObject(i))
                        }

                        if (listTagihan.isEmpty()) {
                            tvKosong.visibility = View.VISIBLE
                        }
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memproses data tagihan", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat tagihan dari server", Toast.LENGTH_SHORT).show()
            }
        )
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

    private fun eksekusiBayarMidtrans(tagihanId: String) {
        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "tagihan-pelanggan"

        val sharedPreferences = requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val idUser = sharedPreferences.getString("ID_USER", "")

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
                    "user_id" to idUser.toString(),
                    "tagihan_id" to tagihanId
                )
            }
        }
        // 🚀 Hanya panggil satu antrean request yang valid di sini
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }
}