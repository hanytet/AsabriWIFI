package com.polinema.asabriwifi

import android.app.AlertDialog
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class PembayaranFragment : Fragment() {

    private lateinit var rvPembayaran: RecyclerView
    private lateinit var rgFilterPembayaran: RadioGroup
    private lateinit var fabCatatTunai: FloatingActionButton
    private lateinit var tvDataKosong: TextView
    private lateinit var pembayaranAdapter: PembayaranAdapter
    private val listPembayaran = ArrayList<JSONObject>()

    private var statusAktif = "semua"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pembayaran, container, false)

        rvPembayaran = view.findViewById(R.id.rvPembayaran)
        rgFilterPembayaran = view.findViewById(R.id.rgFilterPembayaran)
        fabCatatTunai = view.findViewById(R.id.fabCatatTunai)
        tvDataKosong = view.findViewById(R.id.tvDataKosong)

        rvPembayaran.layoutManager = LinearLayoutManager(requireContext())
        pembayaranAdapter = PembayaranAdapter(listPembayaran)
        rvPembayaran.adapter = pembayaranAdapter

        // Registrasi Context Menu
        registerForContextMenu(rvPembayaran)

        // Filter menggunakan RadioGroup
        rgFilterPembayaran.setOnCheckedChangeListener { _, checkedId ->
            statusAktif = when (checkedId) {
                R.id.rbSemuaPembayaran -> "semua"
                R.id.rbPembayaranPending -> "menunggu_verifikasi"
                R.id.rbPembayaranLunas -> "lunas"
                R.id.rbPembayaranDitolaK -> "ditolak"
                else -> "semua"
            }
            fetchDataPembayaran()
        }

        // Klik Item Biasa
        pembayaranAdapter.onItemClick = { bayar ->
            val status = bayar.optString("status", "").uppercase()
            Toast.makeText(requireContext(), "Status: $status (Tahan lama untuk opsi detail)", Toast.LENGTH_SHORT).show()
        }

        // Tombol FAB Catat Tunai
        fabCatatTunai.setOnClickListener {
            ambilDataTagihanBelumLunas()
        }

        fetchDataPembayaran()
        return view
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 101) {
            val position = pembayaranAdapter.positionTerpilih
            if (position >= 0 && position < listPembayaran.size) {
                val dataTerpilih = listPembayaran[position]
                bukaModalDetailContext(dataTerpilih)
            }
            return true
        }
        return super.onContextItemSelected(item)
    }

    private fun fetchDataPembayaran() {
        val parameterStatus = if (statusAktif == "semua") "" else statusAktif
        val url = ApiConfig.BASE_URL + "pembayaran?aksi=tampil&status=$parameterStatus"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    listPembayaran.clear()
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "berhasil") {
                        val jsonArray = jsonObject.getJSONArray("data")
                        for (i in 0 until jsonArray.length()) {
                            listPembayaran.add(jsonArray.getJSONObject(i))
                        }
                        pembayaranAdapter.notifyDataSetChanged()
                    }
                    tvDataKosong.visibility = if (listPembayaran.isEmpty()) View.VISIBLE else View.GONE
                } catch (e: Exception) { e.printStackTrace() }
            },
            { Toast.makeText(requireContext(), "Gagal terhubung ke server", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun bukaModalDetailContext(bayar: JSONObject) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_detail_pembayaran, null)
        val dtPelanggan = dialogView.findViewById<TextView>(R.id.dtPelanggan)
        val dtWaktu = dialogView.findViewById<TextView>(R.id.dtWaktu)
        val dtMetode = dialogView.findViewById<TextView>(R.id.dtMetode)
        val dtTotal = dialogView.findViewById<TextView>(R.id.dtTotal)
        val ivBukti = dialogView.findViewById<ImageView>(R.id.ivBuktiTransfer)
        val tvLabelBukti = dialogView.findViewById<TextView>(R.id.tvLabelBukti)
        val tvNotifMidtrans = dialogView.findViewById<TextView>(R.id.tvNotifMidtrans)

        val idBayar = bayar.optString("id", "")
        val userObj = bayar.optJSONObject("user")
        val namaUser = userObj?.optString("name", "-") ?: "-"
        val metode = bayar.optString("metode", "manual").lowercase()
        val status = bayar.optString("status", "pending").lowercase()
        val jumlah = bayar.optString("jumlah_bayar", "0")
        val buktiPath = bayar.optString("bukti_transfer", "")

        dtPelanggan.text = "Pelanggan: $namaUser"
        dtWaktu.text = "Waktu Bayar: " + (bayar.optString("tanggal_bayar") ?: bayar.optString("created_at", "-"))
        dtMetode.text = "Sistem / Metode: " + metode.uppercase()
        dtTotal.text = "Total Dibayar: Rp $jumlah"

        val midtransMethods = listOf("qris", "bank_transfer", "gopay", "shopeepay", "bca_va", "bni_va", "bri_va")
        if (midtransMethods.contains(metode) && status == "lunas") {
            tvNotifMidtrans.visibility = View.VISIBLE
            ivBukti.visibility = View.GONE
            tvLabelBukti.visibility = View.GONE
        } else if (buktiPath.isNotEmpty() && buktiPath != "null") {
            tvNotifMidtrans.visibility = View.GONE
            ivBukti.visibility = View.VISIBLE
            tvLabelBukti.visibility = View.VISIBLE
        } else {
            ivBukti.visibility = View.GONE
            tvLabelBukti.visibility = View.GONE
        }

        val builder = AlertDialog.Builder(requireContext()).setView(dialogView)

        if (status == "pending" || status == "menunggu_verifikasi") {
            builder.setPositiveButton("Verifikasi Lunas") { _, _ ->
                prosesAksiPembayaran("verifikasi", idBayar, "")
            }
            builder.setNegativeButton("Tolak") { _, _ ->
                tampilkanDialogTolak(idBayar)
            }
        }
        builder.setNeutralButton("Tutup", null)
        builder.show()
    }

    private fun tampilkanDialogTolak(idBayar: String) {
        val input = EditText(requireContext())
        input.hint = "Misal: Bukti transfer buram."

        AlertDialog.Builder(requireContext())
            .setTitle("Alasan Penolakan")
            .setView(input)
            .setPositiveButton("Kirim Penolakan") { _, _ ->
                val catatan = input.text.toString()
                prosesAksiPembayaran("tolak", idBayar, catatan)
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun prosesAksiPembayaran(aksi: String, id: String, catatan: String) {
        val url = ApiConfig.BASE_URL + "pembayaran?aksi=$aksi"
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                Toast.makeText(requireContext(), "Aksi $aksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                fetchDataPembayaran()
            }, null
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id"] = id
                if (aksi == "tolak") params["catatan"] = catatan
                return params
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun ambilDataTagihanBelumLunas() {
        val url = ApiConfig.BASE_URL + "pembayaran?aksi=tampil"
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "berhasil") {
                        val arrPembayaran = jsonObject.getJSONArray("data")
                        tampilkanDialogTunai(arrPembayaran)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { Toast.makeText(requireContext(), "Gagal memuat daftar tagihan", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun tampilkanDialogTunai(arrPembayaran: JSONArray) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_catat_tunai, null)
        val spinnerTagihan = dialogView.findViewById<Spinner>(R.id.spinnerTagihan)
        val etTanggalBayar = dialogView.findViewById<EditText>(R.id.etTanggalBayar)

        etTanggalBayar.isFocusable = false
        etTanggalBayar.isClickable = true
        etTanggalBayar.setOnClickListener {
            val kalender = java.util.Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(requireContext(),
                { _, year, month, dayOfMonth ->
                    android.app.TimePickerDialog(requireContext(),
                        { _, hourOfDay, minute ->
                            val res = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", dayOfMonth)} ${String.format("%02d", hourOfDay)}:${String.format("%02d", minute)}:00"
                            etTanggalBayar.setText(res)
                        }, kalender.get(java.util.Calendar.HOUR_OF_DAY), kalender.get(java.util.Calendar.MINUTE), true
                    ).show()
                }, kalender.get(java.util.Calendar.YEAR), kalender.get(java.util.Calendar.MONTH), kalender.get(java.util.Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val listTeksTagihan = ArrayList<String>()
        val listIdTagihan = ArrayList<String>()

        for (i in 0 until arrPembayaran.length()) {
            val p = arrPembayaran.getJSONObject(i)
            val userObj = p.optJSONObject("user")
            val tagihanObj = p.optJSONObject("tagihan")

            if (tagihanObj != null && userObj != null) {
                val statusTagihan = tagihanObj.optString("status", "").lowercase()
                val statusBayar = p.optString("status", "").lowercase()

                if (statusTagihan == "belum_lunas" || statusBayar == "pending" || statusBayar == "menunggu_verifikasi") {
                    val info = "${userObj.optString("name")} - Bln ${tagihanObj.optString("periode_bulan")}/${tagihanObj.optString("periode_tahun")} (Rp ${tagihanObj.optString("jumlah_tagihan")})"
                    if (!listIdTagihan.contains(tagihanObj.optString("id"))) {
                        listTeksTagihan.add(info)
                        listIdTagihan.add(tagihanObj.optString("id"))
                    }
                }
            }
        }

        if (listTeksTagihan.isEmpty()) {
            Toast.makeText(requireContext(), "Semua tagihan pelanggan sudah berstatus lunas!", Toast.LENGTH_LONG).show()
            return
        }

        spinnerTagihan.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listTeksTagihan)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Simpan Lunas") { _, _ ->
                val posisiTerpilih = spinnerTagihan.selectedItemPosition
                val idTagihanTerpilih = listIdTagihan[posisiTerpilih]
                val tglBayar = etTanggalBayar.text.toString()
                kirimTunaiKeLaravel(idTagihanTerpilih, tglBayar)
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun kirimTunaiKeLaravel(idTagihan: String, tanggal: String) {
        val url = ApiConfig.BASE_URL + "pembayaran?aksi=tunai"
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                Toast.makeText(requireContext(), "Pembayaran tunai berhasil dicatat!", Toast.LENGTH_SHORT).show()
                fetchDataPembayaran()
            }, null
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["tagihan_id"] = idTagihan
                if (tanggal.isNotEmpty()) params["tanggal_bayar"] = tanggal
                return params
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }
}


