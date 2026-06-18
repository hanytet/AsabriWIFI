package com.polinema.asabriwifi

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import org.json.JSONObject

class KeluhanFragment : Fragment() {

    private lateinit var rvKeluhan: RecyclerView
    private lateinit var tabFilter: TabLayout
    private lateinit var etCari: EditText
    private lateinit var tvKosong: TextView
    private lateinit var txtBaru: TextView
    private lateinit var txtProses: TextView
    private lateinit var txtSelesai: TextView

    private lateinit var keluhanAdapter: KeluhanAdapter
    private val listKeluhan = ArrayList<JSONObject>()
    private var statusAktif = "semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_keluhan, container, false)

        rvKeluhan = view.findViewById(R.id.rvKeluhan)
        tabFilter = view.findViewById(R.id.tabFilterKeluhan)
        etCari = view.findViewById(R.id.etCariKeluhan)
        tvKosong = view.findViewById(R.id.tvKeluhanKosong)
        txtBaru = view.findViewById(R.id.boxTotalBaru)
        txtProses = view.findViewById(R.id.boxTotalProses)
        txtSelesai = view.findViewById(R.id.boxTotalSelesai)

        rvKeluhan.layoutManager = LinearLayoutManager(requireContext())
        keluhanAdapter = KeluhanAdapter(listKeluhan)
        rvKeluhan.adapter = keluhanAdapter

        registerForContextMenu(rvKeluhan)

        // Buat Bar Filter Tab
        tabFilter.addTab(tabFilter.newTab().setText("Semua"))
        tabFilter.addTab(tabFilter.newTab().setText("Baru"))
        tabFilter.addTab(tabFilter.newTab().setText("Diterima"))
        tabFilter.addTab(tabFilter.newTab().setText("Ditugaskan"))
        tabFilter.addTab(tabFilter.newTab().setText("Berangkat"))
        tabFilter.addTab(tabFilter.newTab().setText("Selesai"))
        tabFilter.addTab(tabFilter.newTab().setText("Ditolak"))

        tabFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val text = tab?.text.toString().lowercase()
                statusAktif = when (text) {
                    "ditugaskan" -> "teknisi_ditugaskan"
                    "berangkat" -> "teknisi_berangkat"
                    else -> text
                }
                fetchDataKeluhan()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Pencarian via Enter Keyboard
        etCari.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                fetchDataKeluhan()
                true
            } else false
        }

        fetchDataKeluhan()
        return view
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 401) {
            val position = keluhanAdapter.positionTerpilih
            if (position >= 0 && position < listKeluhan.size) {
                val keluhan = listKeluhan[position]
                bukaDialogAksiAlurStatus(keluhan)
            }
            return true
        }
        return super.onContextItemSelected(item)
    }

    private fun fetchDataKeluhan() {
        val paramStatus = if (statusAktif == "semua") "" else statusAktif
        val paramCari = etCari.text.toString()
        val url = "${ApiConfig.BASE_URL}keluhan?aksi=tampil&status=$paramStatus&cari=$paramCari"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    listKeluhan.clear()
                    val json = JSONObject(response)
                    if (json.getString("status") == "berhasil") {
                        // Pasang Counter Box Atas
                        txtBaru.text = json.optString("total_baru", "0")
                        txtProses.text = json.optString("total_proses", "0")
                        txtSelesai.text = json.optString("total_selesai", "0")

                        val arr = json.getJSONArray("data")
                        for (i in 0 until arr.length()) {
                            listKeluhan.add(arr.getJSONObject(i))
                        }
                    }
                    keluhanAdapter.notifyDataSetChanged()
                    tvKosong.visibility = if (listKeluhan.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) { e.printStackTrace() }
            },
            { Toast.makeText(requireContext(), "Gagal memuat keluhan", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    // DIALOG AKSI COCOK 100% DENGAN ATURAN IF-ELSE STATUS BLADE LARAVEL
    private fun bukaDialogAksiAlurStatus(keluhan: JSONObject) {
        val idKeluhan = keluhan.optString("id")
        val status = keluhan.optString("status", "baru").lowercase()

        val listOpsi = ArrayList<String>()
        if (status == "baru") listOpsi.add("Terima Keluhan")
        if (status == "baru" || status == "diterima") listOpsi.add("Tugaskan Teknisi")
        if (status == "diterima" || status == "teknisi_ditugaskan") listOpsi.add("Tandai Teknisi Berangkat")
        if (status != "selesai" && status != "ditolak") {
            listOpsi.add("Tandai Selesai")
            listOpsi.add("Tolak Keluhan")
        }

        if (listOpsi.isEmpty()) {
            Toast.makeText(requireContext(), "Keluhan ini sudah selesai diproses.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tindakan Keluhan #${keluhan.optString("nama")}")
            .setItems(listOpsi.toTypedArray()) { _, which ->
                val aksiTerpilih = listOpsi[which]
                when (aksiTerpilih) {
                    "Terima Keluhan" -> tampilkanFormCatatan("terima", idKeluhan)
                    "Tugaskan Teknisi" -> ambilListTeknisiLaravel(idKeluhan)
                    "Tandai Teknisi Berangkat" -> kirimAksiStatusKeLaravel("berangkat", idKeluhan, "", "")
                    "Tandai Selesai" -> tampilkanFormCatatan("selesai", idKeluhan)
                    "Tolak Keluhan" -> tampilkanFormCatatan("tolak", idKeluhan)
                }
            }.setNegativeButton("Batal", null).show()
    }

    private fun tampilkanFormCatatan(aksi: String, id: String) {
        val input = EditText(requireContext())
        input.hint = if (aksi == "tolak") "Alasan penolakan (Wajib)" else "Catatan admin (Opsional)"

        AlertDialog.Builder(requireContext())
            .setTitle("Masukkan Catatan / Keterangan")
            .setView(input)
            .setPositiveButton("Kirim") { _, _ ->
                val catatan = input.text.toString()
                if (aksi == "tolak" && catatan.isEmpty()) {
                    Toast.makeText(requireContext(), "Alasan penolakan wajib diisi!", Toast.LENGTH_SHORT).show()
                } else {
                    kirimAksiStatusKeLaravel(aksi, id, "", catatan)
                }
            }.setNegativeButton("Batal", null).show()
    }

    private fun ambilListTeknisiLaravel(idKeluhan: String) {
        val url = "${ApiConfig.BASE_URL}keluhan?aksi=list_teknisi"
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "berhasil") {
                        val arr = json.getJSONArray("data")
                        tampilkanDialogPilihTeknisi(idKeluhan, arr)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }, null
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun tampilkanDialogPilihTeknisi(idKeluhan: String, arrTeknisi: JSONArray) {
        val spinner = Spinner(requireContext())
        val listNama = ArrayList<String>()
        val listId = ArrayList<String>()

        for (i in 0 until arrTeknisi.length()) {
            val t = arrTeknisi.getJSONObject(i)
            listNama.add(t.getString("name") + " (" + t.getString("email") + ")")
            listId.add(t.getString("id"))
        }

        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listNama)

        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Teknisi Lapangan")
            .setView(spinner)
            .setPositiveButton("Tugaskan") { _, _ ->
                if (listId.isNotEmpty()) {
                    val idTeknisi = listId[spinner.selectedItemPosition]
                    kirimAksiStatusKeLaravel("tugaskan", idKeluhan, idTeknisi, "Ditugaskan via Android")
                }
            }.setNegativeButton("Batal", null).show()
    }

    private fun kirimAksiStatusKeLaravel(aksi: String, id: String, teknisiId: String, catatan: String) {
        val url = "${ApiConfig.BASE_URL}keluhan?aksi=$aksi"
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { _ ->
                Toast.makeText(requireContext(), "Status keluhan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                fetchDataKeluhan()
            },
            { Toast.makeText(requireContext(), "Gagal mengubah status keluhan", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id"] = id
                if (teknisiId.isNotEmpty()) params["teknisi_id"] = teknisiId
                if (catatan.isNotEmpty()) params["catatan_admin"] = catatan
                return params
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}