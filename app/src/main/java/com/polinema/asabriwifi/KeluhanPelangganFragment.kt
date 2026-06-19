package com.polinema.asabriwifi

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject

class KeluhanPelangganFragment : Fragment() {

    private lateinit var rvKeluhan: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var spinnerKategori: Spinner

    // 🚀 UBAH: Dari EditText menjadi AutoCompleteTextView
    private lateinit var etIsi: AutoCompleteTextView
    private lateinit var btnKirim: Button

    private val listKeluhan = ArrayList<JSONObject>()
    private lateinit var adapter: KeluhanPelangganAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_keluhan_pelanggan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvKeluhan = view.findViewById(R.id.rvKeluhan)
        progressBar = view.findViewById(R.id.progressBarKeluhan)
        spinnerKategori = view.findViewById(R.id.spinKategori)


        etIsi = view.findViewById(R.id.etIsiKeluhan)
        btnKirim = view.findViewById(R.id.btnKirimKeluhan)


        val daftarSaranKendala = arrayOf(
            "WiFi terhubung tapi tidak ada internet (No Internet)",
            "Lampu indikator LOS berkedip merah",
            "Sinyal WiFi tidak muncul / tidak terdeteksi",
            "Koneksi internet sangat lambat / lemot",
            "Router WiFi mati total tidak ada lampu menyala",
            "Gagal login ke jaringan WiFi Asabri",
            "Kabel FO (Fiber Optik) putus terkena pohon"
        )

        val autoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, daftarSaranKendala)
        etIsi.setAdapter(autoAdapter)

        rvKeluhan.layoutManager = LinearLayoutManager(requireContext())
        adapter = KeluhanPelangganAdapter(listKeluhan)
        rvKeluhan.adapter = adapter

        btnKirim.setOnClickListener { kirimKeluhanKeServer() }

        loadRiwayatKeluhan()
    }

    private fun loadRiwayatKeluhan() {
        progressBar.visibility = View.VISIBLE
        val idUser = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE).getString("ID_USER", "")

        val url = ApiConfig.BASE_URL + "keluhan?aksi=tampil_pelanggan&user_id=$idUser"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listKeluhan.clear()
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "berhasil") {
                        val arr = jsonObject.getJSONArray("data")
                        for (i in 0 until arr.length()) {
                            listKeluhan.add(arr.getJSONObject(i))
                        }
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Log.e("AsabriError", "Gagal memuat keluhan: ${error.message}")
            }
        )
        stringRequest.setShouldCache(false)
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun kirimKeluhanKeServer() {
        val kategori = spinnerKategori.selectedItem.toString()
        val isiKeluhan = etIsi.text.toString().trim()

        if (spinnerKategori.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Silakan pilih kategori kendala", Toast.LENGTH_SHORT).show()
            return
        }
        if (isiKeluhan.isEmpty()) {
            Toast.makeText(requireContext(), "Isi deskripsi keluhan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        val prefs = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val idUser = prefs.getString("ID_USER", "")
        val noHp = prefs.getString("NO_HP", "081234567890")
        val alamat = prefs.getString("ALAMAT", "Alamat Pelanggan Asabri")

        val url = ApiConfig.BASE_URL + "keluhan"

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    Toast.makeText(requireContext(), jsonObject.optString("pesan"), Toast.LENGTH_SHORT).show()
                    if (jsonObject.optString("status") == "berhasil") {
                        etIsi.text.clear()
                        spinnerKategori.setSelection(0)
                        loadRiwayatKeluhan()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "aksi" to "simpan_keluhan",
                    "user_id" to idUser.toString(),
                    "no_hp" to noHp.toString(),
                    "alamat_tujuan" to alamat.toString(),
                    "kategori" to kategori,
                    "isi_keluhan" to isiKeluhan
                )
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }
}