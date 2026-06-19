package com.polinema.asabriwifi

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
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

        // 🚀 Bersihkan inisialisasi tanpa parameter lambda lama
        adapter = PenggunaAdapter(listUser)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 🚀 DAFTARKAN RECYCLERVIEW KE CONTEXT MENU SYSTEM
        registerForContextMenu(rvPengguna)
    }

    // 🚀 LOGIKA MENANGKAP PILIHAN MENU: Menggantikan fungsi dialog opsi lama secara penuh
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = adapter.positionTerpilih
        if (position == -1 || position >= listUser.size) return false

        val user = listUser[position]

        return when (item.itemId) {
            201 -> { // Edit Informasi
                bukaDialogEdit(user)
                true
            }
            202 -> { // Reset Password
                konfirmasiAksi("reset_password", user.optString("id"), "Reset password user ini?")
                true
            }
            203 -> { // Hapus Akun
                konfirmasiAksi("hapus", user.optString("id"), "Hapus akun user ini permanen?")
                true
            }
            else -> super.onContextItemSelected(item)
        }
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

    private fun bukaDialogEdit(user: JSONObject) {
        val form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_pengguna, null)
        val edNama = form.findViewById<EditText>(R.id.edEditNama)
        val edEmail = form.findViewById<EditText>(R.id.edEditEmail)
        val edNik = form.findViewById<EditText>(R.id.edEditNik)
        val spRole = form.findViewById<Spinner>(R.id.spEditRole)

        // Set teks data awal dari JSON model bawaan database
        edNama.setText(user.optString("name"))
        edEmail.setText(user.optString("email"))

        val nikAwal = user.optString("nik", "")
        edNik.setText(if (nikAwal != "null") nikAwal else "")

        // 🚀 FIXED: Menghapus "secondadmin" dari pilihan array list role
        val opsiRole = arrayOf("admin", "pelanggan", "teknisi")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opsiRole)
        spRole.adapter = spinnerAdapter

        // Deteksi role saat ini (jika user lama secondadmin, otomatis default ke pelanggan atau sesuaikan)
        var roleSekarang = user.optString("role", "pelanggan").lowercase()
        if (roleSekarang == "secondadmin") {
            roleSekarang = "admin" // Fallback jika ada data lama yang tersangkut role tersebut
        }

        val indexRole = opsiRole.indexOf(roleSekarang)
        if (indexRole >= 0) spRole.setSelection(indexRole)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Akun Pengguna")
            .setView(form)
            .setPositiveButton("Simpan Updates") { _, _ ->
                val namaBaru = edNama.text.toString().trim()
                val emailBaru = edEmail.text.toString().trim()
                val nikBaru = edNik.text.toString().trim()
                val roleBaru = spRole.selectedItem.toString()

                if (namaBaru.isEmpty() || emailBaru.isEmpty()) {
                    Toast.makeText(requireContext(), "Nama dan Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                } else {
                    eksekusiSimpanEdit(
                        user.optString("id"),
                        namaBaru,
                        emailBaru,
                        nikBaru,
                        roleBaru
                    )
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
                    override fun getParams(): MutableMap<String, String> {
                        return hashMapOf("id" to id)
                    }
                }
                VolleySingleton.getInstance(requireContext()).addToRequestQueue(post)
            }.setNegativeButton("Tidak", null).show()
    }
}