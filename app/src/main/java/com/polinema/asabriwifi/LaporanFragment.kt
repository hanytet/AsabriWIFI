package com.polinema.asabriwifi

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
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
import com.google.android.material.tabs.TabLayout
import org.json.JSONObject
import java.util.Calendar

class LaporanFragment : Fragment() {

    private lateinit var tabLaporan: TabLayout
    private lateinit var layoutHarian: LinearLayout
    private lateinit var layoutBulanan: LinearLayout
    private lateinit var etTanggal: EditText
    private lateinit var spinnerBulan: Spinner
    private lateinit var spinnerTahun: Spinner

    private lateinit var tvTotal: TextView
    private lateinit var tvTransfer: TextView
    private lateinit var tvTunai: TextView
    private lateinit var rvTransaksi: RecyclerView
    private lateinit var tvKosong: TextView

    private lateinit var btnExportExcel: Button

    private lateinit var adapter: TransaksiLaporanAdapter
    private val listTransaksi = ArrayList<JSONObject>()

    private var isTabHarianActive = true
    private val namaBulan = arrayOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    private val pilihanTahun = arrayOf("2025", "2026", "2027")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_laporan, container, false)

        tabLaporan = view.findViewById(R.id.tabLaporan)
        layoutHarian = view.findViewById(R.id.layoutFilterHarian)
        layoutBulanan = view.findViewById(R.id.layoutFilterBulanan)
        etTanggal = view.findViewById(R.id.etTanggalLaporan)
        spinnerBulan = view.findViewById(R.id.spinnerBulanLaporan)
        spinnerTahun = view.findViewById(R.id.spinnerTahunLaporan)
        tvTotal = view.findViewById(R.id.tvSummaryTotal)
        tvTransfer = view.findViewById(R.id.tvSummaryTransfer)
        tvTunai = view.findViewById(R.id.tvSummaryTunai)
        rvTransaksi = view.findViewById(R.id.rvTransaksiLaporan)
        tvKosong = view.findViewById(R.id.tvLaporanKosong)

        btnExportExcel = view.findViewById(R.id.btnExportExcel)

        rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransaksiLaporanAdapter(listTransaksi, true)
        rvTransaksi.adapter = adapter

        // Setup Spinners
        spinnerBulan.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, namaBulan)
        spinnerTahun.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, pilihanTahun)

        // Set Default Date Saat Ini
        val kalender = Calendar.getInstance()
        val tglSkg = "${kalender.get(Calendar.YEAR)}-${String.format("%02d", kalender.get(Calendar.MONTH) + 1)}-${String.format("%02d", kalender.get(Calendar.DAY_OF_MONTH))}"
        etTanggal.setText(tglSkg)
        spinnerBulan.setSelection(kalender.get(Calendar.MONTH))
        spinnerTahun.setSelection(pilihanTahun.indexOf(kalender.get(Calendar.YEAR).toString()))

        // Setup Tabs
        tabLaporan.addTab(tabLaporan.newTab().setText("Harian"))
        tabLaporan.addTab(tabLaporan.newTab().setText("Bulanan"))

        tabLaporan.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    isTabHarianActive = true
                    layoutHarian.visibility = View.VISIBLE
                    layoutBulanan.visibility = View.GONE
                    adapter.setMode(true)
                    loadLaporanHarian()
                } else {
                    isTabHarianActive = false
                    layoutHarian.visibility = View.GONE
                    layoutBulanan.visibility = View.VISIBLE
                    adapter.setMode(false)
                    loadLaporanBulanan()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // On Click DatePicker untuk Harian
        etTanggal.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val res = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", dayOfMonth)}"
                etTanggal.setText(res)
                loadLaporanHarian()
            }, kalender.get(Calendar.YEAR), kalender.get(Calendar.MONTH), kalender.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Listener Spinner Bulanan
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (!isTabHarianActive) loadLaporanBulanan()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        spinnerBulan.onItemSelectedListener = spinnerListener
        spinnerTahun.onItemSelectedListener = spinnerListener

        btnExportExcel.setOnClickListener {
            jalankanEksporLaporan()
        }

        loadLaporanHarian()
        return view
    }

    private fun jalankanEksporLaporan() {
        if (isTabHarianActive) {
            // Memberitahu pengguna bahwa ekspor harian tidak tersedia sesuai konfigurasi rute server
            Toast.makeText(requireContext(), "Ekspor spreadsheet hanya tersedia untuk Laporan Bulanan", Toast.LENGTH_SHORT).show()
        } else {
            val bln = spinnerBulan.selectedItemPosition + 1
            val thn = spinnerTahun.selectedItem.toString()

            // Memanggil rute ekspor bulanan tunggal yang terdaftar pada Laravel
            val downloadUrl = "${ApiConfig.BASE_URL}laporan/bulanan/export?bulan=$bln&tahun=$thn"

            try {
                Toast.makeText(requireContext(), "Mengunduh file laporan spreadsheet...", Toast.LENGTH_SHORT).show()
                val intentEkspor = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(intentEkspor)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Perangkat gagal mengunduh berkas laporan", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadLaporanHarian() {
        listTransaksi.clear()
        adapter.notifyDataSetChanged()

        val url = "${ApiConfig.BASE_URL}laporan/harian/data?tanggal=" + etTanggal.text.toString()

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val summary = json.getJSONObject("summary")

                    tvTotal.text = "Rp " + String.format("%,d", summary.optLong("total", 0)).replace(",", ".")
                    tvTransfer.text = "Rp " + String.format("%,d", summary.optLong("transfer", 0)).replace(",", ".")
                    tvTunai.text = "Rp " + String.format("%,d", summary.optLong("tunai", 0)).replace(",", ".")

                    val arr = json.getJSONArray("transaksi")
                    for (i in 0 until arr.length()) {
                        listTransaksi.add(arr.getJSONObject(i))
                    }
                    adapter.notifyDataSetChanged()
                    tvKosong.visibility = if (listTransaksi.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) { e.printStackTrace() }
            },
            {
                Toast.makeText(requireContext(), "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun loadLaporanBulanan() {
        listTransaksi.clear()
        adapter.notifyDataSetChanged()

        val bln = spinnerBulan.selectedItemPosition + 1
        val thn = spinnerTahun.selectedItem.toString()

        val url = "${ApiConfig.BASE_URL}laporan/bulanan/data?bulan=$bln&tahun=$thn"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val totalPemasukan = json.optLong("total_pemasukan", 0)

                    tvTotal.text = "Rp " + String.format("%,d", totalPemasukan).replace(",", ".")
                    tvTransfer.text = "User Baru: " + json.optString("pelanggan_baru", "0")
                    tvTunai.text = "-"

                    val arr = json.getJSONArray("transaksi")
                    for (i in 0 until arr.length()) {
                        listTransaksi.add(arr.getJSONObject(i))
                    }
                    adapter.notifyDataSetChanged()
                    tvKosong.visibility = if (listTransaksi.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) { e.printStackTrace() }
            },
            {
                Toast.makeText(requireContext(), "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}