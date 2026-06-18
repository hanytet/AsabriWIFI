package com.polinema.asabriwifi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class PenggunaFragment : Fragment() {

    private lateinit var rvPengguna: RecyclerView
    private lateinit var etCari: EditText
    private lateinit var progress: ProgressBar
    private lateinit var adapter: PenggunaAdapter
    private val listUser = ArrayList<JSONObject>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_pengguna, container, false)

        rvPengguna = v.findViewById(R.id.rvPengguna)
        etCari = v.findViewById(R.id.etCariPengguna)
        progress = v.findViewById(R.id.progressPengguna)

        rvPengguna.layoutManager = LinearLayoutManager(requireContext())

        // Callback adapter dengan ekspresi lambda yang aman
        adapter = PenggunaAdapter(listUser) { userTerpilih ->
            bukaDialogOpsi(userTerpilih)
        }
        rvPengguna.adapter = adapter

        etCari.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                fetchPengguna()
                true
            } else false
        }

        fetchPengguna()
        return v
    }

    private fun fetchPengguna() {
        if (!isAdded) return
        progress.visibility = View.VISIBLE
        val url = "${ApiConfig.BASE_URL}pengguna?aksi=tampil&cari=${etCari.text}"

        val req = StringRequest(Request.Method.GET, url,
            { res ->
                try {
                    progress.visibility = View.GONE
                    listUser.clear()
                    val json = JSONObject(res)
                    if (json.optString("status") == "berhasil") {
                        val arr = json.getJSONArray("data")
                        for (i in 0 until arr.length()) listUser.add(arr.getJSONObject(i))
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: Exception) { e.printStackTrace() }
            },
            {
                if (isAdded) progress.visibility = View.GONE
            }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(req)
    }

    private fun bukaDialogOpsi(user: JSONObject) {
        val opsi = arrayOf("Edit Informasi Pengguna", "Reset Password ke Default", "Hapus Akun Pengguna")
        AlertDialog.Builder(requireContext())
            .setTitle(user.optString("name", "Pilih Tindakan"))
            .setItems(opsi) { _, which ->
                when (which) {
                    0 -> bukaDialogEdit(user)
                    1 -> konfirmasiAksi("reset_password", user.optString("id"), "Reset password user ini?")
                    2 -> konfirmasiAksi("hapus", user.optString("id"), "Hapus akun user ini permanen?")
                }
            }.show()
    }

    private fun bukaDialogEdit(user: JSONObject) {
        val form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_pengguna, null)
        val edNama = form.findViewById<EditText>(R.id.edEditNama)
        val spRole = form.findViewById<Spinner>(R.id.spEditRole)

        edNama.setText(user.optString("name"))

        val opsiRole = arrayOf("admin", "pelanggan", "secondadmin", "teknisi")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opsiRole)
        spRole.adapter = spinnerAdapter

        val roleSekarang = user.optString("role", "pelanggan")
        val indexRole = opsiRole.indexOf(roleSekarang)
        if (indexRole >= 0) spRole.setSelection(indexRole)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Akun Pengguna")
            .setView(form)
            .setPositiveButton("Simpan Updates") { _, _ ->
                val namaBaru = edNama.text.toString()
                val roleBaru = spRole.selectedItem.toString()

                if (namaBaru.isNotEmpty()) {
                    eksekusiSimpanEdit(
                        user.optString("id"),
                        namaBaru,
                        user.optString("email"),
                        user.optString("nik"),
                        roleBaru
                    )
                } else {
                    Toast.makeText(requireContext(), "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiSimpanEdit(id: String, nama: String, email: String, nik: String, role: String) {
        val url = "${ApiConfig.BASE_URL}pengguna?aksi=ubah"
        val post = object : StringRequest(Request.Method.POST, url,
            {
                Toast.makeText(requireContext(), "✅ Data pengguna berhasil diperbarui", Toast.LENGTH_SHORT).show()
                fetchPengguna()
            },
            {
                Toast.makeText(requireContext(), "❌ Gagal memperbarui data", Toast.LENGTH_SHORT).show()
            }
        ) {
            // 👇 FIXED: Menggunakan keyword bawaan Kotlin 'override fun' 👇
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf("id" to id, "name" to nama, "email" to email, "nik" to nik, "role" to role)
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(post)
    }

    private fun konfirmasiAksi(aksi: String, id: String, pesan: String) {
        AlertDialog.Builder(requireContext()).setMessage(pesan)
            .setPositiveButton("Ya") { _, _ ->
                val url = "${ApiConfig.BASE_URL}pengguna?aksi=$aksi"
                val post = object : StringRequest(Request.Method.POST, url,
                    {
                        Toast.makeText(requireContext(), "✅ Aksi berhasil dieksekusi", Toast.LENGTH_SHORT).show()
                        fetchPengguna()
                    },
                    {
                        Toast.makeText(requireContext(), "❌ Gagal mengeksekusi tindakan", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    // 👇 FIXED: Menggunakan keyword bawaan Kotlin 'override fun' 👇
                    override fun getParams(): MutableMap<String, String> {
                        return hashMapOf("id" to id)
                    }
                }
                VolleySingleton.getInstance(requireContext()).addToRequestQueue(post)
            }.setNegativeButton("Tidak", null).show()
    }
}