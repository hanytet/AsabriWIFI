package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject

class PilihPaketActivity : AppCompatActivity() {

    private lateinit var rvPaket: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val listPaket = ArrayList<JSONObject>()
    private lateinit var adapter: PaketPelangganAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pilih_paket)

        rvPaket = findViewById(R.id.rvPaketPelanggan)
        progressBar = findViewById(R.id.progressBarPaket)

        rvPaket.layoutManager = LinearLayoutManager(this)
        adapter = PaketPelangganAdapter(listPaket) { paketObj ->
            tampilkanKonfirmasiLangganan(paketObj)
        }
        rvPaket.adapter = adapter

        loadDaftarPaket()
    }

    private fun loadDaftarPaket() {
        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "paket" // Menembak endpoint route::paket Tuan

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listPaket.clear()
                    // Menangkap struktur data model web
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("pakets")) {
                        val arr = jsonObject.getJSONArray("pakets")
                        for (i in 0 until arr.length()) {
                            listPaket.add(arr.getJSONObject(i))
                        }
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Gagal memproses data paket", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat list paket dari server", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
    }

    private fun tampilkanKonfirmasiLangganan(paket: JSONObject) {
        val paketId = paket.optString("id")
        val namaPaket = paket.optString("nama_paket")

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Berlangganan")
            .setMessage("Yakin ingin berlangganan paket $namaPaket?\nTagihan pertama akan langsung dibuat otomatis oleh sistem.")
            .setCancelable(false)
            .setPositiveButton("Ya, Langganan") { _, _ ->
                eksekusiBerlangganan(paketId)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiBerlangganan(paketId: String) {
        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "berlangganan" // Sesuai endpoint POST web Tuan

        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val idUser = sharedPreferences.getString("ID_USER", "")

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    val status = jsonObject.optString("status", "berhasil")

                    if (status == "berhasil" || jsonObject.optBoolean("success", true)) {
                        Toast.makeText(this, "Berhasil berlangganan paket!", Toast.LENGTH_LONG).show()

                        // Kembalikan ke dashboard pelanggan & segarkan state
                        val intent = Intent(this, CustomerDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val pesanError = jsonObject.optString("pesan", "Gagal memproses langganan baru.")
                        Toast.makeText(this, pesanError, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    // Fallback jika backend melakukan redirect html dashboard
                    Toast.makeText(this, "Sukses mendaftarkan paket wifi baru!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                val response = error.networkResponse
                if (response != null && response.statusCode == 422) {
                    Toast.makeText(this, "Anda masih memiliki paket aktif!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Koneksi terputus atau server sibuk", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "user_id" to idUser.toString(),
                    "paket_id" to paketId
                )
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(postRequest)
    }
}