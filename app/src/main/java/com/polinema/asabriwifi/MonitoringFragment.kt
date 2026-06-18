package com.polinema.asabriwifi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONObject

class MonitoringFragment : Fragment() {

    private lateinit var rvMonitoring: RecyclerView
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner
    private lateinit var btnExportPdf: Button
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutEmptyState: LinearLayout

    private lateinit var monitoringAdapter: MonitoringAdapter
    private val listTunggakan = ArrayList<JSONObject>()

    private val namaBulan = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    private val pilihanTahun = arrayOf("2025", "2026", "2027")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_monitoring, container, false)

        rvMonitoring = view.findViewById(R.id.rvMonitoring)
        spinnerBulan = view.findViewById(R.id.spinnerBulan)
        spinnerTahun = view.findViewById(R.id.spinnerTahun)
        btnExportPdf = view.findViewById(R.id.btnExportPdf)
        progressLoading = view.findViewById(R.id.progressLoading)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        rvMonitoring.layoutManager = LinearLayoutManager(requireContext())
        monitoringAdapter = MonitoringAdapter(listTunggakan)
        rvMonitoring.adapter = monitoringAdapter

        registerForContextMenu(rvMonitoring)

        // Setup Isi Spinner Filter
        spinnerBulan.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, namaBulan)
        spinnerTahun.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, pilihanTahun)

        // Set default filter ke bulan & tahun sekarang (Juni 2026)
        val kalender = java.util.Calendar.getInstance()
        spinnerBulan.setSelection(kalender.get(java.util.Calendar.MONTH))
        spinnerTahun.setSelection(pilihanTahun.indexOf(kalender.get(java.util.Calendar.YEAR).toString()))

        val listenerFilter = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fetchDataTunggakan()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerBulan.onItemSelectedListener = listenerFilter
        spinnerTahun.onItemSelectedListener = listenerFilter

        // Aksi Buka File PDF Laporan dari Laravel DomPDF
        btnExportPdf.setOnClickListener {
            val bln = spinnerBulan.selectedItemPosition + 1
            val thn = spinnerTahun.selectedItem.toString()

            // 👇 FIXED: Langsung menembak endpoint rute API publik khusus PDF 👇
            val urlPdf = "${ApiConfig.BASE_URL}monitoring/pdf?bulan=$bln&tahun=$thn"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlPdf))
            startActivity(intent)
        }

        return view
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 301) {
            val position = monitoringAdapter.positionTerpilih
            if (position >= 0 && position < listTunggakan.size) {
                val userTerpilih = listTunggakan[position]
                val userId = userTerpilih.optString("user_id", "")

                AlertDialog.Builder(requireContext())
                    .setTitle("Kirim Pengingat")
                    .setMessage("Kirim notifikasi peringatan tagihan ke pelanggan ini?")
                    .setPositiveButton("Ya, Kirim") { _, _ ->
                        eksekusiKirimNotif(userId)
                    }
                    .setNegativeButton("Batal", null).show()
            }
            return true
        }
        return super.onContextItemSelected(item)
    }

    private fun fetchDataTunggakan() {
        progressLoading.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        listTunggakan.clear()
        monitoringAdapter.notifyDataSetChanged()

        val bln = spinnerBulan.selectedItemPosition + 1
        val thn = spinnerTahun.selectedItem.toString()

        // SINKRONISASI 1: Endpoint disesuaikan dengan rute GET api/monitoring/data
        val url = "${ApiConfig.BASE_URL}monitoring/data?bulan=$bln&tahun=$thn"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    progressLoading.visibility = View.GONE

                    // Laravel mereturn response()->json($data) berupa array murni [...]
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        listTunggakan.add(jsonArray.getJSONObject(i))
                    }
                    monitoringAdapter.notifyDataSetChanged()
                    layoutEmptyState.visibility = if (listTunggakan.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal memproses struktur data server", Toast.LENGTH_SHORT).show()
                }
            },
            {
                progressLoading.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat data dari server", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun eksekusiKirimNotif(userId: String) {
        // SINKRONISASI 2: Endpoint disesuaikan dengan POST api/monitoring/{userId}/kirim-pengingat
        val url = "${ApiConfig.BASE_URL}monitoring/$userId/kirim-pengingat"

        val stringRequest = StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success", false)) {
                        Toast.makeText(requireContext(), "✅ Notifikasi berhasil dikirim!", Toast.LENGTH_SHORT).show()
                    } else {
                        val pesan = json.optString("message", "Gagal mengirimkan pengingat")
                        Toast.makeText(requireContext(), "❌ $pesan", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { Toast.makeText(requireContext(), "❌ Gagal mengirimkan pengingat", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}