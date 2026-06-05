package com.polinema.asabriwifi

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject

class PaketFragment : Fragment() {

    private lateinit var rvPaket: RecyclerView
    private lateinit var fabTambah: FloatingActionButton
    private lateinit var paketAdapter: PaketAdapter
    private val listPaket = ArrayList<JSONObject>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_paket, container, false)

        rvPaket = view.findViewById(R.id.rvPaket)
        fabTambah = view.findViewById(R.id.fabTambah)

        rvPaket.layoutManager = LinearLayoutManager(requireContext())
        paketAdapter = PaketAdapter(listPaket)
        rvPaket.adapter = paketAdapter

        fabTambah.setOnClickListener {
            tampilkanDialogForm(null)
        }

        paketAdapter.onItemClick = { paketTerpilih ->
            tampilkanDialogForm(paketTerpilih)
        }

        fetchDataPaket()

        return view
    }

    private fun tampilkanDialogForm(paketLama: JSONObject?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_paket, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etNamaPaket = dialogView.findViewById<EditText>(R.id.etNamaPaket)
        val etKecepatan = dialogView.findViewById<EditText>(R.id.etKecepatan)
        val etHarga = dialogView.findViewById<EditText>(R.id.etHarga)
        val etDeskripsi = dialogView.findViewById<EditText>(R.id.etDeskripsi)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerStatus)

        // 1. SIAPKAN ISI SPINNER
        val pilihanStatus = arrayOf("aktif", "nonaktif")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, pilihanStatus)
        spinnerStatus.adapter = spinnerAdapter

        var paketId = ""
        var isEdit = false

        // 2. JIKA EDIT, MASUKKAN DATA LAMA KE FORM TERMASUK STATUSNYA
        if (paketLama != null) {
            isEdit = true
            tvDialogTitle.text = "Edit Paket"
            try {
                paketId = paketLama.getString("id")
                etNamaPaket.setText(paketLama.getString("nama_paket"))
                etKecepatan.setText(paketLama.getString("kecepatan"))
                etHarga.setText(paketLama.getString("harga"))
                if (paketLama.has("deskripsi") && !paketLama.isNull("deskripsi")) {
                    etDeskripsi.setText(paketLama.getString("deskripsi"))
                }

                // Set posisi spinner sesuai status di database
                if (paketLama.has("status") && !paketLama.isNull("status")) {
                    val statusDb = paketLama.getString("status")
                    if (statusDb == "nonaktif") {
                        spinnerStatus.setSelection(1) // Pilih urutan ke-2 ("nonaktif")
                    } else {
                        spinnerStatus.setSelection(0) // Pilih urutan ke-1 ("aktif")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNamaPaket.text.toString()
                val kecepatan = etKecepatan.text.toString()
                val harga = etHarga.text.toString()
                val deskripsi = etDeskripsi.text.toString()
                val statusPilihan = spinnerStatus.selectedItem.toString() // Ambil nilai "aktif" atau "nonaktif"

                if (nama.isEmpty() || kecepatan.isEmpty() || harga.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama, Kecepatan, Harga wajib diisi!", Toast.LENGTH_SHORT).show()
                } else {
                    kirimDataKeLaravel(isEdit, paketId, nama, kecepatan, harga, deskripsi, statusPilihan)
                }
            }
            .setNegativeButton("Batal", null)

        if (isEdit) {
            dialogBuilder.setNeutralButton("Hapus") { _, _ ->
                konfirmasiHapus(paketId)
            }
        }

        dialogBuilder.show()
    }

    private fun konfirmasiHapus(id: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Paket")
            .setMessage("Yakin ingin menghapus paket ini secara permanen?")
            .setPositiveButton("Ya, Hapus") { _, _ ->

                val url = ApiConfig.BASE_URL + "paket?aksi=hapus"
                val stringRequest = object : StringRequest(
                    Request.Method.POST, url,
                    { response ->
                        try {
                            val status = JSONObject(response).getString("status")
                            if (status == "berhasil") {
                                Toast.makeText(requireContext(), "Paket berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                fetchDataPaket()
                            } else {
                                Toast.makeText(requireContext(), "Gagal menghapus paket", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Gagal memproses data", Toast.LENGTH_SHORT).show()
                        }
                    },
                    { error ->
                        Toast.makeText(requireContext(), "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        val params = HashMap<String, String>()
                        params["id"] = id
                        return params
                    }
                }
                VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)

            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // 3. TAMBAHKAN PARAMETER STATUS SAAT MENGIRIM DATA
    private fun kirimDataKeLaravel(isEdit: Boolean, id: String, nama: String, kecepatan: String, harga: String, deskripsi: String, statusPaket: String) {
        // Cek apakah aksi ini khusus untuk mengubah status saja lewat Laravel
        // (Tapi di sini kita kirim sekaligus dengan update datanya)
        val aksi = if (isEdit) "edit" else "tambah"
        val url = ApiConfig.BASE_URL + "paket?aksi=$aksi"

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val status = jsonObject.getString("status")
                    if (status == "berhasil") {

                        // JIKA INI EDIT, KITA TEMBAK JUGA API KHUSUS STATUSNYA BIAR PASTI BERUBAH
                        if (isEdit) {
                            val aksiStatus = if (statusPaket == "aktif") "aktifkan" else "nonaktif"
                            updateStatusKhusus(id, aksiStatus)
                        } else {
                            Toast.makeText(requireContext(), "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            fetchDataPaket()
                        }

                    } else {
                        val pesan = jsonObject.getString("pesan")
                        Toast.makeText(requireContext(), "Gagal: $pesan", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal membaca balasan server", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                if (isEdit) params["id"] = id
                params["nama_paket"] = nama
                params["kecepatan"] = kecepatan
                params["harga"] = harga
                params["deskripsi"] = deskripsi
                // Di controller Anda saat tambah, statusnya otomatis 'aktif', jadi kita ikuti saja.
                // Jika ingin dinamis, controller PHP Anda harus disesuaikan sedikit.
                return params
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    // FUNGSI BANTUAN UNTUK MENEMBAK API STATUS KHUSUS DARI LARAVEL ANDA
    private fun updateStatusKhusus(id: String, aksiStatus: String) {
        val url = ApiConfig.BASE_URL + "paket?aksi=$aksiStatus"
        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { _ ->
                // Status berhasil diubah, refresh data
                Toast.makeText(requireContext(), "Data & Status berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                fetchDataPaket()
            },
            { _ ->
                fetchDataPaket() // Refresh saja meskipun gagal
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id"] = id
                return params
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun fetchDataPaket() {
        val url = ApiConfig.BASE_URL + "paket?aksi=tampil"
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    listPaket.clear()
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "berhasil") {
                        val jsonArray = jsonObject.getJSONArray("data")
                        for (i in 0 until jsonArray.length()) {
                            listPaket.add(jsonArray.getJSONObject(i))
                        }
                        paketAdapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error ->
                if (isAdded) Toast.makeText(requireContext(), "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}