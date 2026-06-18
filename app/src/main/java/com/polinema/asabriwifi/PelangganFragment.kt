package com.polinema.asabriwifi

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
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

class PelangganFragment : Fragment() {

    private lateinit var rvPelanggan: RecyclerView
    private lateinit var fabTambah: FloatingActionButton
    private lateinit var pelangganAdapter: PelangganAdapter
    private val listPelanggan = ArrayList<JSONObject>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pelanggan, container, false)

        rvPelanggan = view.findViewById(R.id.rvPelanggan)
        fabTambah = view.findViewById(R.id.fabTambahPelanggan)

        rvPelanggan.layoutManager = LinearLayoutManager(requireContext())
        pelangganAdapter = PelangganAdapter(listPelanggan)
        rvPelanggan.adapter = pelangganAdapter

        fabTambah.setOnClickListener { tampilkanDialogForm(null) }
        pelangganAdapter.onItemClick = { pelanggan -> tampilkanDialogForm(pelanggan) }

        fetchDataPelanggan()
        return view
    }

    private fun tampilkanDialogForm(pelangganLama: JSONObject?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pelanggan, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitlePelanggan)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etNoHp = dialogView.findViewById<EditText>(R.id.etNoHp)
        val etAlamat = dialogView.findViewById<EditText>(R.id.etAlamat)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerStatusPelanggan)
        val layoutAkunBaru = dialogView.findViewById<LinearLayout>(R.id.layoutAkunBaru)

        val pilihanStatus = arrayOf("aktif", "nonaktif")
        spinnerStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, pilihanStatus)

        var pelangganId = ""
        var isEdit = false

        if (pelangganLama != null) {
            isEdit = true
            tvDialogTitle.text = "Edit Pelanggan"
            layoutAkunBaru.visibility = View.GONE

            try {
                pelangganId = pelangganLama.getString("id")
                etName.setText(pelangganLama.getString("name"))
                etNoHp.setText(pelangganLama.getString("no_hp"))
                etAlamat.setText(pelangganLama.getString("alamat"))

                if (pelangganLama.getString("status") == "nonaktif") {
                    spinnerStatus.setSelection(1)
                } else {
                    spinnerStatus.setSelection(0)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name = etName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                val no_hp = etNoHp.text.toString().trim()
                val alamat = etAlamat.text.toString().trim()
                val statusPilihan = spinnerStatus.selectedItem.toString()

                if (name.isEmpty() || no_hp.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama dan Nomor HP wajib diisi!", Toast.LENGTH_SHORT).show()
                } else {
                    kirimDataKeLaravel(isEdit, pelangganId, name, email, password, no_hp, alamat, statusPilihan)
                }
            }
            .setNegativeButton("Batal", null)

        if (isEdit) {
            dialogBuilder.setNeutralButton("Hapus") { _, _ -> konfirmasiHapus(pelangganId) }
        }

        dialogBuilder.show()
    }

    private fun konfirmasiHapus(id: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pelanggan")
            .setMessage("Yakin ingin menghapus pelanggan ini?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                // 🚀 FIXED: Bersihkan URL murni tanpa query string ?aksi=hapus
                val url = ApiConfig.BASE_URL + "pelanggan"
                val stringRequest = object : StringRequest(Request.Method.POST, url,
                    { response ->
                        try {
                            if (JSONObject(response).optString("status") == "berhasil") {
                                Toast.makeText(requireContext(), "Pelanggan berhasil dihapus", Toast.LENGTH_SHORT).show()
                                fetchDataPelanggan()
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }, null
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        val params = HashMap<String, String>()
                        // 🚀 FIXED: Masukkan aksi hapus ke dalam body data POST
                        params["aksi"] = "hapus"
                        params["id"] = id
                        return params
                    }
                }
                VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun kirimDataKeLaravel(isEdit: Boolean, id: String, name: String, email: String, pass: String, no_hp: String, alamat: String, statusPilihan: String) {
        val aksi = if (isEdit) "edit" else "tambah"
        // 🚀 FIXED: URL bersih tanpa query string agar tidak di-strip oleh Volley
        val url = ApiConfig.BASE_URL + "pelanggan"

        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "berhasil") {
                        if (isEdit) {
                            updateStatusKhusus(id, statusPilihan)
                        } else {
                            Toast.makeText(requireContext(), "Pelanggan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            fetchDataPelanggan()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal: " + jsonObject.getString("pesan"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { Toast.makeText(requireContext(), "Gagal terhubung", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                // 🚀 FIXED: Amankan pengiriman aksi manipulasi database
                params["aksi"] = aksi
                if (isEdit) {
                    params["id"] = id
                } else {
                    params["email"] = email
                    params["password"] = pass
                }
                params["name"] = name
                params["no_hp"] = no_hp
                params["alamat"] = alamat
                return params
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun updateStatusKhusus(id: String, statusPilihan: String) {
        // 🚀 FIXED: URL bersih tanpa query string
        val url = ApiConfig.BASE_URL + "pelanggan"
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                Toast.makeText(requireContext(), "Data & Status berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                fetchDataPelanggan()
            },
            { fetchDataPelanggan() }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                // 🚀 FIXED: Set aksi toggle_status melalui form-data POST
                params["aksi"] = "toggle_status"
                params["id"] = id
                params["status"] = statusPilihan
                return params
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun fetchDataPelanggan() {
        // 🚀 AMAN: GET request diperbolehkan memakai query string secara natural
        val url = ApiConfig.BASE_URL + "pelanggan?aksi=tampil"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    listPelanggan.clear()
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "berhasil") {
                        val jsonArray = jsonObject.getJSONArray("data")
                        for (i in 0 until jsonArray.length()) {
                            listPelanggan.add(jsonArray.getJSONObject(i))
                        }
                        pelangganAdapter.notifyDataSetChanged()

                        if (isAdded && jsonArray.length() > 0) {
                            Toast.makeText(requireContext(), "Berhasil memuat ${jsonArray.length()} pelanggan", Toast.LENGTH_SHORT).show()
                        } else if (isAdded) {
                            Toast.makeText(requireContext(), "Data pelanggan masih kosong", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (isAdded) Toast.makeText(requireContext(), "Gagal: ${jsonObject.getString("pesan")}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isAdded) Toast.makeText(requireContext(), "Format salah. Cek Logcat!", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                if (isAdded) Toast.makeText(requireContext(), "Gagal konek ke server", Toast.LENGTH_SHORT).show()

                val networkResponse = error.networkResponse
                if (networkResponse != null && networkResponse.data != null) {
                    try {
                        val htmlError = String(networkResponse.data)
                        println("Status Code Server: ${networkResponse.statusCode}")
                        println("Pesan Error Server:\n$htmlError")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}