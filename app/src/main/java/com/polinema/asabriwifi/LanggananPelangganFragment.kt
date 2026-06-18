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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class LanggananPelangganFragment : Fragment() {

    private lateinit var rvLangganan: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvKosong: TextView
    private val listLangganan = ArrayList<JSONObject>()
    private lateinit var adapter: LanggananPelangganAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_langganan_pelanggan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvLangganan = view.findViewById(R.id.rvLanggananPelanggan)
        progressBar = view.findViewById(R.id.progressBarLangganan)
        tvKosong = view.findViewById(R.id.tvLanggananKosong)

        rvLangganan.layoutManager = LinearLayoutManager(requireContext())
        adapter = LanggananPelangganAdapter(listLangganan) { obj -> tampilkanDialogKonfirmasi(obj) }
        rvLangganan.adapter = adapter

        loadPaketAktifPelanggan()
    }

    private fun loadPaketAktifPelanggan() {
        progressBar.visibility = View.VISIBLE
        tvKosong.visibility = View.GONE

        val idUser = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE).getString("ID_USER", "") ?: ""
        val url = "${ApiConfig.BASE_URL}tagihan?aksi=tampil_langganan_aktif&user_id=${idUser.trim()}"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listLangganan.clear()
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "berhasil") {
                        val arr = jsonObject.optJSONArray("data")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                listLangganan.add(arr.getJSONObject(i))
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                    tvKosong.visibility = if (listLangganan.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) { e.printStackTrace() }
            },
            { progressBar.visibility = View.GONE }
        )
        stringRequest.setShouldCache(false)
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun tampilkanDialogKonfirmasi(obj: JSONObject) {
        val langgananId = obj.optString("langganan_id")
        val namaPaket = obj.optString("nama_paket")
        val idUser = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE).getString("ID_USER", "") ?: ""

        AlertDialog.Builder(requireContext())
            .setTitle("Berhenti Berlangganan")
            .setMessage("Apakah Anda yakin ingin menghentikan paket internet berjalan: $namaPaket?")
            .setPositiveButton("Ya, Putuskan") { _, _ ->
                progressBar.visibility = View.VISIBLE
                val url = ApiConfig.BASE_URL + "tagihan"

                val postRequest = object : StringRequest(Method.POST, url,
                    { response ->
                        progressBar.visibility = View.GONE
                        try {
                            val json = JSONObject(response)
                            Toast.makeText(requireContext(), json.optString("pesan"), Toast.LENGTH_SHORT).show()
                            if (json.optString("status") == "berhasil") loadPaketAktifPelanggan()
                        } catch (e: Exception) { e.printStackTrace() }
                    },
                    { progressBar.visibility = View.GONE }
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        return hashMapOf(
                            "aksi" to "batal",
                            "user_id" to idUser.trim(),
                            "langganan_id" to langgananId
                        )
                    }
                }
                VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
            }
            .setNegativeButton("Kembali", null).show()
    }
}