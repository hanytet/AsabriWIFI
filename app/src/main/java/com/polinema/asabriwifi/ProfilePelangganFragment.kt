package com.polinema.asabriwifi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class ProfilePelangganFragment : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNoHp: EditText
    private lateinit var etAlamat: EditText
    private lateinit var etCurrentPass: EditText
    private lateinit var etNewPass: EditText

    private lateinit var btnSaveProfile: Button
    private lateinit var btnSavePass: Button
    private lateinit var btnGetGPS: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var mapView: MapView
    private var mapMarker: Marker? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latSimpan = "-7.8167"
    private var lngSimpan = "112.0167"

    private var globalUserId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val ctx = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = requireActivity().packageName

        val view = inflater.inflate(R.layout.fragment_profile_pelanggan, container, false)

        // Membaca ID User yang aktif dari sesi login yang valid
        inisialisasiSesiID()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        etName = view.findViewById(R.id.etProfileName)
        etEmail = view.findViewById(R.id.etProfileEmail)
        etNoHp = view.findViewById(R.id.etProfileNoHp)
        etAlamat = view.findViewById(R.id.etProfileAlamat)
        etCurrentPass = view.findViewById(R.id.etProfileCurrentPassword)
        etNewPass = view.findViewById(R.id.etProfileNewPassword)

        btnSaveProfile = view.findViewById(R.id.btnUpdateProfile)
        btnSavePass = view.findViewById(R.id.btnUpdatePassword)
        btnGetGPS = view.findViewById(R.id.btnProfileGetGPS)
        btnDeleteAccount = view.findViewById(R.id.btnProfileDeleteAccount)
        progressBar = view.findViewById(R.id.progressBarProfile)

        mapView = view.findViewById(R.id.mapProfileView)
        mapView.setMultiTouchControls(true)

        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    latSimpan = p.latitude.toString()
                    lngSimpan = p.longitude.toString()
                    perbaruiTitikPetaVisual(p.latitude, p.longitude)

                    try {
                        val geocoder = Geocoder(requireContext(), Locale("id", "ID"))
                        val listAlamat = geocoder.getFromLocation(p.latitude, p.longitude, 1)
                        if (!listAlamat.isNullOrEmpty()) {
                            etAlamat.setText(listAlamat[0].getAddressLine(0))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }
                return false
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }

        mapView.overlays.add(MapEventsOverlay(receiver))

        btnSaveProfile.setOnClickListener { eksekusiUpdateProfil() }
        btnSavePass.setOnClickListener { eksekusiUpdatePassword() }
        btnGetGPS.setOnClickListener { ambilKoordinatGPSAkurat() }
        btnDeleteAccount.setOnClickListener { memunculkanDialogKonfirmasiHapus() }

        loadDataProfilLengkap()
        return view
    }

    private fun inisialisasiSesiID() {
        // PERBAIKAN: Selalu paksa membaca data ID_USER dari SharedPreferences terbaru untuk menghindari tumpang tindih cache akun lain
        val sharedPreferences = requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        globalUserId = sharedPreferences.getString("ID_USER", "") ?: ""

        globalUserId = globalUserId.trim()
        Log.d("AsabriDebug", "ID Pengguna Terkunci di Fragment Profile: '$globalUserId'")
    }

    private fun perbaruiTitikPetaVisual(lat: Double, lng: Double) {
        val titikKunci = GeoPoint(lat, lng)
        mapView.controller.setZoom(16.5)
        mapView.controller.setCenter(titikKunci)

        if (mapMarker != null) mapView.overlays.remove(mapMarker)

        mapMarker = Marker(mapView).apply {
            position = titikKunci
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Lokasi Rumah Anda"
        }
        mapView.overlays.add(mapMarker)
        mapView.invalidate()
    }

    private fun ambilKoordinatGPSAkurat() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        progressBar.visibility = View.VISIBLE
        btnGetGPS.text = "Mengunci Posisi..."

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                progressBar.visibility = View.GONE
                btnGetGPS.text = "Kunci Koordinat via GPS Device"
                if (location != null) {
                    latSimpan = location.latitude.toString()
                    lngSimpan = location.longitude.toString()
                    perbaruiTitikPetaVisual(location.latitude, location.longitude)
                    try {
                        val geocoder = Geocoder(requireContext(), Locale("id", "ID"))
                        val listAlamat = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!listAlamat.isNullOrEmpty()) {
                            etAlamat.setText(listAlamat[0].getAddressLine(0))
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                btnGetGPS.text = "Kunci Koordinat via GPS Device"
            }
    }

    private fun loadDataProfilLengkap() {
        if (globalUserId.isEmpty() || globalUserId == "null") {
            Toast.makeText(requireContext(), "Sesi kosong, silakan login kembali", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "profile-pelanggan?aksi=detail_profil&user_id=$globalUserId"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.optString("status") == "berhasil") {
                        val data = jsonObject.optJSONObject("data")
                        if (data != null) {
                            etName.setText(data.optString("name", "").trim())
                            etEmail.setText(data.optString("email", "").trim())
                            etNoHp.setText(data.optString("no_hp", "").trim())
                            etAlamat.setText(data.optString("alamat", "").trim())

                            val latRaw = data.optString("latitude", "").trim()
                            val lngRaw = data.optString("longitude", "").trim()

                            latSimpan = if (latRaw.isEmpty() || latRaw == "null" || latRaw == "0.0" || latRaw == "0") "-7.8167" else latRaw
                            lngSimpan = if (lngRaw.isEmpty() || lngRaw == "null" || lngRaw == "0.0" || lngRaw == "0") "112.0167" else lngRaw

                            try {
                                perbaruiTitikPetaVisual(latSimpan.toDouble(), lngSimpan.toDouble())
                            } catch (numEx: Exception) {
                                perbaruiTitikPetaVisual(-7.8167, 112.0167)
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), jsonObject.optString("pesan", "Gagal memuat profil"), Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("AsabriError", "JSON Parsing Error: ${e.message}")
                    Toast.makeText(requireContext(), "Gagal memproses data profil", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                progressBar.visibility = View.GONE
                val statusResponseCode = error.networkResponse?.statusCode
                Log.e("AsabriError", "Volley Error Code: $statusResponseCode | Msg: ${error.message}")
                Toast.makeText(requireContext(), "Gagal terhubung ke Profile API (Status: $statusResponseCode)", Toast.LENGTH_SHORT).show()
            }
        )

        // PERBAIKAN: Matikan caching Volley secara total agar response akun lama tidak menimpa akun baru
        stringRequest.setShouldCache(false)
        VolleySingleton.getInstance(requireContext()).requestQueue.cache.clear()

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun eksekusiUpdateProfil() {
        val name = etName.text.toString().trim()
        val noHp = etNoHp.text.toString().trim()
        val alamat = etAlamat.text.toString().trim()

        if (name.isEmpty() || noHp.isEmpty() || alamat.isEmpty()) {
            Toast.makeText(requireContext(), "Data input tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (globalUserId.isEmpty()) return

        if (latSimpan.isEmpty() || latSimpan == "null" || latSimpan == "0.0" || latSimpan == "0") {
            latSimpan = "-7.8167"
        }
        if (lngSimpan.isEmpty() || lngSimpan == "null" || lngSimpan == "0.0" || lngSimpan == "0") {
            lngSimpan = "112.0167"
        }

        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "profile-pelanggan"

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    Toast.makeText(requireContext(), jsonObject.optString("pesan"), Toast.LENGTH_SHORT).show()
                    if (jsonObject.optString("status") == "berhasil") loadDataProfilLengkap()
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error ->
                progressBar.visibility = View.GONE
                val statusResponseCode = error.networkResponse?.statusCode
                Log.e("AsabriError", "Update Profil Eror: $statusResponseCode")
                Toast.makeText(requireContext(), "Gagal update profil (Status: $statusResponseCode)", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "aksi" to "update_profil",
                    "user_id" to globalUserId,
                    "name" to name,
                    "no_hp" to noHp,
                    "alamat" to alamat,
                    "latitude" to latSimpan,
                    "longitude" to lngSimpan
                )
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }

    private fun eksekusiUpdatePassword() {
        val currentPass = etCurrentPass.text.toString().trim()
        val newPass = etNewPass.text.toString().trim()

        if (currentPass.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(requireContext(), "Password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (globalUserId.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "profile-pelanggan"

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    Toast.makeText(requireContext(), jsonObject.optString("pesan"), Toast.LENGTH_SHORT).show()
                    if (jsonObject.optString("status") == "berhasil") {
                        etCurrentPass.text.clear()
                        etNewPass.text.clear()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { progressBar.visibility = View.GONE }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "aksi" to "update_password",
                    "user_id" to globalUserId,
                    "current_password" to currentPass,
                    "password" to newPass
                )
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }

    private fun memunculkanDialogKonfirmasiHapus() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Konfirmasi Hapus Akun")

        val inputPassword = EditText(requireContext()).apply {
            hint = "Masukkan Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(50, 20, 50, 10)
            layoutParams = params
            addView(inputPassword)
        }

        builder.setView(containerLayout)
        builder.setPositiveButton("HAPUS AKUN") { dialog, _ ->
            val passwordConfirm = inputPassword.text.toString().trim()
            if (passwordConfirm.isEmpty()) {
                Toast.makeText(requireContext(), "Password wajib diisi!", Toast.LENGTH_SHORT).show()
            } else {
                eksekusiHapusAkunKeServer(passwordConfirm)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("BATAL") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun eksekusiHapusAkunKeServer(passwordKonfirmasi: String) {
        if (globalUserId.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        val url = ApiConfig.BASE_URL + "profile-pelanggan"

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                progressBar.visibility = View.GONE
                try {
                    val jsonObject = JSONObject(response)
                    Toast.makeText(requireContext(), jsonObject.optString("pesan"), Toast.LENGTH_LONG).show()

                    if (jsonObject.optString("status") == "berhasil") {
                        requireActivity().getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE).edit().clear().apply()
                        val intent = Intent(requireActivity(), LandingActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { progressBar.visibility = View.GONE }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "aksi" to "hapus_akun",
                    "user_id" to globalUserId,
                    "password" to passwordKonfirmasi
                )
            }
        }
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(postRequest)
    }

    override fun onResume() { super.onResume(); mapView.onResume() }

    override fun onPause() { super.onPause(); mapView.onPause() }
}