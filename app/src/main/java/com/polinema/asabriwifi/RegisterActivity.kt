package com.polinema.asabriwifi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import java.util.HashMap
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var etNama: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNik: EditText
    private lateinit var etHp: EditText
    private lateinit var etAlamat: EditText
    private lateinit var etPass: EditText
    private lateinit var etConfirmPass: EditText
    private lateinit var btnDaftar: Button
    private lateinit var btnGetGPS: Button

    private lateinit var mapView: MapView
    private var mapMarker: Marker? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latSimpan = "-7.77245"
    private var lngSimpan = "111.9834"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi OSM Cache Agen Agen Agen
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_register)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etNama = findViewById(R.id.etRegNama)
        etEmail = findViewById(R.id.etRegEmail)
        etNik = findViewById(R.id.etRegNik)
        etHp = findViewById(R.id.etRegHp)
        etAlamat = findViewById(R.id.etRegAlamat)
        etPass = findViewById(R.id.etRegPassword)
        etConfirmPass = findViewById(R.id.etRegConfirmPassword)
        btnDaftar = findViewById(R.id.btnKirimDaftar)
        btnGetGPS = findViewById(R.id.btnRegGetGPS)

        mapView = findViewById(R.id.mapRegisterView)
        mapView.setMultiTouchControls(true)

        // Konfigurasi Deteksi Klik Manual Pada Objek Peta
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    latSimpan = p.latitude.toString()
                    lngSimpan = p.longitude.toString()
                    perbaruiTitikPetaVisual(p.latitude, p.longitude)

                    try {
                        val geocoder = Geocoder(this@RegisterActivity, Locale("id", "ID"))
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

        btnGetGPS.setOnClickListener {
            ambilKoordinatGPSOtomatis()
        }

        btnDaftar.setOnClickListener {
            eksekusiRegistrasiAkun()
        }

        // Jalankan pelacakan titik pertama default saat aplikasi dibuka
        ambilKoordinatGPSOtomatis()
    }

    private fun perbaruiTitikPetaVisual(lat: Double, lng: Double) {
        val titikKunci = GeoPoint(lat, lng)
        mapView.controller.setZoom(16.5)
        mapView.controller.setCenter(titikKunci)

        if (mapMarker != null) mapView.overlays.remove(mapMarker)

        mapMarker = Marker(mapView).apply {
            position = titikKunci
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Titik Rumah Pemasangan Baru"
        }
        mapView.overlays.add(mapMarker)
        mapView.invalidate()
    }

    private fun ambilKoordinatGPSOtomatis() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        btnGetGPS.text = "⏳ Mengunci Posisi..."
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                btnGetGPS.text = "📍 Kunci Koordinat via GPS Device"
                if (location != null) {
                    latSimpan = location.latitude.toString()
                    lngSimpan = location.longitude.toString()
                    perbaruiTitikPetaVisual(location.latitude, location.longitude)
                    try {
                        val geocoder = Geocoder(this, Locale("id", "ID"))
                        val listAlamat = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!listAlamat.isNullOrEmpty()) {
                            etAlamat.setText(listAlamat[0].getAddressLine(0))
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            .addOnFailureListener {
                btnGetGPS.text = "📍 Kunci Koordinat via GPS Device"
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ambilKoordinatGPSOtomatis()
        }
    }

    private fun eksekusiRegistrasiAkun() {
        val nama = etNama.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val nik = etNik.text.toString().trim()
        val hp = etHp.text.toString().trim()
        val alamat = etAlamat.text.toString().trim()
        val pass = etPass.text.toString().trim()
        val confirmPass = etConfirmPass.text.toString().trim()

        if (nama.isEmpty() || email.isEmpty() || nik.isEmpty() || hp.isEmpty() || alamat.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi seluruh field formulir!", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPass) {
            Toast.makeText(this, "Konfirmasi kata sandi tidak cocok!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = ApiConfig.BASE_URL + "register"
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optString("status") == "berhasil") {
                        Toast.makeText(this, "Pendaftaran Akun Berhasil!", Toast.LENGTH_SHORT).show()

                        val userObj = json.optJSONObject("user")
                        val idUser = userObj?.optString("id", "") ?: ""
                        val role = userObj?.optString("role", "pelanggan") ?: "pelanggan"

                        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putBoolean("IS_LOGGED_IN", true)
                            .putString("ID_USER", idUser.trim())
                            .putString("ROLE", role.trim())
                            .commit()

                        val intent = Intent(this, CustomerDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, json.optString("pesan", "Gagal mendaftar"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            { error ->
                try {
                    val responseBody = String(error.networkResponse.data, Charsets.UTF_8)
                    val data = JSONObject(responseBody)
                    Toast.makeText(this, data.optString("pesan", "Gagal mendaftar"), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["name"] = nama
                params["email"] = email
                params["nik"] = nik
                params["no_hp"] = hp
                params["alamat"] = alamat
                params["password"] = pass
                params["password_confirmation"] = confirmPass
                params["latitude"] = latSimpan
                params["longitude"] = lngSimpan
                return params
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
}