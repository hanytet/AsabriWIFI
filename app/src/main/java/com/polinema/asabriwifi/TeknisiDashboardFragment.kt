package com.polinema.asabriwifi

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class TeknisiDashboardFragment : Fragment() {

    private lateinit var tvTotalTugas: TextView
    private lateinit var tvTotalProses: TextView
    private lateinit var tvTotalSelesai: TextView
    private lateinit var rvTugas: RecyclerView
    private lateinit var progressBar: ProgressBar

    // PERBAIKAN WIDGET FILTER: Tambahan penampung widget dropdown filter dari layout
    private lateinit var btnFilterStatus: LinearLayout
    private lateinit var tvLabelFilterSelected: TextView

    // Master list menampung semua data asli dari server, listTugas menampung data hasil filter untuk adapter
    private val listTugasMaster = ArrayList<KeluhanTeknisi>()
    private val listTugas = ArrayList<KeluhanTeknisi>()
    private lateinit var adapter: TeknisiTugasAdapter
    private var idTeknisi: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Beritahu sistem bahwa fragment ini mengontrol siklus hidup Action Bar Menu
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teknisi_dashboard, container, false)

        tvTotalTugas = view.findViewById(R.id.tvStatTotalTugas)
        tvTotalProses = view.findViewById(R.id.tvStatTotalProses)
        tvTotalSelesai = view.findViewById(R.id.tvStatTotalSelesai)
        rvTugas = view.findViewById(R.id.rvTugasTeknisi)
        progressBar = view.findViewById(R.id.progressBarTeknisi)

        // Inisialisasi komponen pemicu filter
        btnFilterStatus = view.findViewById(R.id.btnFilterStatus)
        tvLabelFilterSelected = view.findViewById(R.id.tvLabelFilterSelected)

        rvTugas.layoutManager = LinearLayoutManager(requireContext())

        adapter = TeknisiTugasAdapter(listTugas,
            { tugas -> eksekusiAksiTeknisi("berangkat", tugas.id, "") },
            { tugas, catatan -> eksekusiAksiTeknisi("selesai", tugas.id, catatan) }
        )
        rvTugas.adapter = adapter

        // LOGIKA KLIK FILTER POPUP MENU (Disinkronkan dengan state web)
        btnFilterStatus.setOnClickListener { viewPemicu ->
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), viewPemicu)
            popup.menuInflater.inflate(R.menu.menu_filter_status, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                tvLabelFilterSelected.text = item.title

                when (item.itemId) {
                    R.id.status_semua -> saringDataStatusLokal("Semua Status")
                    R.id.status_baru -> saringDataStatusLokal("Keluhan Baru")
                    R.id.status_diterima -> saringDataStatusLokal("Diterima Admin")
                    R.id.status_ditugaskan -> saringDataStatusLokal("Ditugaskan")
                    R.id.status_berangkat -> saringDataStatusLokal("Berangkat")
                    R.id.status_selesai -> saringDataStatusLokal("Selesai")
                }
                true
            }
            popup.show()
        }

        val sharedPreferences = requireContext().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        idTeknisi = sharedPreferences.getString("ID_USER", "") ?: ""

        loadDataTugasTeknisi()
        return view
    }

    // PERBAIKAN AMAN: Sembunyikan ikon profil menggunakan indeks agar tidak menyebabkan Unresolved Reference
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menu.size() > 1) {
            // Menyembunyikan menu pertama (biasanya Profil), menu kedua (Logout) tetap muncul
            menu.getItem(0).isVisible = false
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    // FUNGSI UTAMA PENYARING DATA RECYCLERVIEW SECARA LOKAL
    private fun saringDataStatusLokal(statusDicari: String) {
        listTugas.clear()
        if (statusDicari == "Semua Status") {
            listTugas.addAll(listTugasMaster)
        } else {
            for (item in listTugasMaster) {
                if (item.statusLabel.trim().equals(statusDicari.trim(), ignoreCase = true)) {
                    listTugas.add(item)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadDataTugasTeknisi() {
        progressBar.visibility = View.VISIBLE
        val url = "${ApiConfig.BASE_URL}teknisi/dashboard?aksi=tampil_tugas&teknisi_id=$idTeknisi"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    listTugasMaster.clear()
                    listTugas.clear()
                    val json = JSONObject(response)
                    if (json.optString("status") == "berhasil") {
                        val stat = json.getJSONObject("statistik")
                        tvTotalTugas.text = stat.optString("total_tugas", "0")
                        tvTotalProses.text = stat.optString("total_proses", "0")
                        tvTotalSelesai.text = stat.optString("total_selesai", "0")

                        val arr = json.optJSONArray("data")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val modelKeluhan = KeluhanTeknisi(
                                    obj.optString("id"),
                                    obj.optString("hari_keluhan"),
                                    obj.optString("tanggal_keluhan"),
                                    obj.optString("nama_pelanggan"),
                                    obj.optString("no_hp"),
                                    obj.optString("alamat_tujuan"),
                                    obj.optString("kategori"),
                                    obj.optString("isi_keluhan"),
                                    obj.optString("catatan_admin"),
                                    obj.optString("status"),
                                    obj.optString("status_label"),
                                    obj.optString("teknisi_id")
                                )
                                listTugasMaster.add(modelKeluhan)
                            }
                        }
                    }

                    tvLabelFilterSelected.text = "Semua Status"
                    listTugas.addAll(listTugasMaster)
                    adapter.notifyDataSetChanged()
                } catch (e: Exception) { e.printStackTrace() }
            },
            { progressBar.visibility = View.GONE }
        )
        stringRequest.setShouldCache(false)
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun eksekusiAksiTeknisi(aksi: String, keluhanId: String, catatan: String) {
        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "teknisi/dashboard"

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val json = JSONObject(response)
                    Toast.makeText(requireContext(), json.optString("pesan"), Toast.LENGTH_SHORT).show()
                    if (json.optString("status") == "berhasil") {
                        loadDataTugasTeknisi()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { progressBar.visibility = View.GONE }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "aksi" to aksi,
                    "teknisi_id" to idTeknisi,
                    "keluhan_id" to keluhanId,
                    "catatan_admin" to catatan
                )
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }
}