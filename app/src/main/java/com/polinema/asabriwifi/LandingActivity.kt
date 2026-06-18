package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class LandingActivity : AppCompatActivity() {

    private lateinit var actvWilayah: AutoCompleteTextView
    private lateinit var btnCekJaringan: Button
    private lateinit var btnMasuk: Button
    private lateinit var rvPaket: RecyclerView
    private lateinit var mapView: MapView

    private lateinit var videoPromo: VideoView
    private lateinit var videoOverlay: View

    private lateinit var adapter: PaketLandingAdapter
    private val listPaket = ArrayList<JSONObject>()
    private val listRawWilayah = ArrayList<JSONObject>()
    private val listNamaWilayah = ArrayList<String>()

    private var idWilayahTerpilih: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =========================================================================
        // 1. SINKRONISASI PROTEKSI SESI (Pencegah Mental & Auto-Logout Multi-Role)
        // =========================================================================
        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("IS_LOGGED_IN", false)) {
            val role = sharedPreferences.getString("ROLE", "") ?: ""

            val intentBypass = when {
                role.contains("admin", ignoreCase = true) -> {
                    Intent(this, MainActivity::class.java)
                }
                role.contains("teknisi", ignoreCase = true) -> {
                    Intent(this, TeknisiMainActivity::class.java)
                }
                else -> {
                    Intent(this, CustomerDashboardActivity::class.java)
                }
            }

            intentBypass.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intentBypass)
            finish()
            return
        }

        // Inisialisasi OSM Cache
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        setContentView(R.layout.activity_landing)

        actvWilayah = findViewById(R.id.actvWilayah)
        btnCekJaringan = findViewById(R.id.btnCekJaringan)
        btnMasuk = findViewById(R.id.btnMasukLanding)
        rvPaket = findViewById(R.id.rvPaketLanding)
        mapView = findViewById(R.id.mapView)

        videoPromo = findViewById(R.id.videoPromo)
        videoOverlay = findViewById(R.id.videoOverlay)

        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)

        rvPaket.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = PaketLandingAdapter(listPaket) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        rvPaket.adapter = adapter

        // =========================================================================
        // 2. KONTROL ENGINE PEMUTAR VIDEO PROMO (Auto-Play, Pause-Resume Toggle, & Infinite Loop)
        // =========================================================================
        try {
            val pathVideo = "android.resource://" + packageName + "/" + R.raw.promo_asabri
            videoPromo.setVideoURI(Uri.parse(pathVideo))

            videoPromo.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true

                // Mengaktifkan output suara ke volume penuh (1f, 1f) sejak awal pemutaran
                mediaPlayer.setVolume(1f, 1f)

                // Hilangkan filter overlay gelap karena video diputar dengan suara normal
                videoOverlay.visibility = View.GONE
                videoPromo.start()
            }

            // IMPLEMENTASI TOGGLE PLAY / PAUSE VIDEO
            val videoClickListener = View.OnClickListener {
                if (videoPromo.isPlaying) {
                    videoPromo.pause()
                    Toast.makeText(this, "Video Di-Pause", Toast.LENGTH_SHORT).show()
                } else {
                    videoPromo.start()
                    Toast.makeText(this, "Memutar Video Profil AsabriWiFi", Toast.LENGTH_SHORT).show()
                }
            }
            videoPromo.setOnClickListener(videoClickListener)
            videoOverlay.setOnClickListener(videoClickListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnMasuk.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        btnCekJaringan.setOnClickListener { eksekusiCekJaringan() }

        actvWilayah.setOnItemClickListener { parent, view, position, id ->
            val namaTerpilih = parent.getItemAtPosition(position).toString()
            val indexAsli = listNamaWilayah.indexOf(namaTerpilih)
            if (indexAsli != -1) {
                val obj = listRawWilayah[indexAsli]
                idWilayahTerpilih = obj.optString("id")
                updateMapLocation(obj)
            }
        }

        loadLandingData()
    }

    private fun loadLandingData() {
        val url = ApiConfig.BASE_URL + "landing-data"

        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    listPaket.clear()
                    if (response.has("pakets")) {
                        val arr = response.getJSONArray("pakets")
                        for (i in 0 until arr.length()) listPaket.add(arr.getJSONObject(i))
                    }
                    adapter.notifyDataSetChanged()

                    listRawWilayah.clear()
                    listNamaWilayah.clear()

                    if (response.has("wilayahs")) {
                        val arrWilayah = response.getJSONArray("wilayahs")

                        for (i in 0 until arrWilayah.length()) {
                            val obj = arrWilayah.getJSONObject(i)
                            listRawWilayah.add(obj)

                            val namaWilayah = obj.optString("nama_wilayah", "Wilayah")
                            val kecamatan = obj.optString("kecamatan", "")
                            listNamaWilayah.add(if (kecamatan.isNotEmpty()) "$namaWilayah - $kecamatan" else namaWilayah)
                        }

                        val textAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listNamaWilayah)
                        actvWilayah.setAdapter(textAdapter)

                        if (listRawWilayah.isNotEmpty()) {
                            actvWilayah.setText(listNamaWilayah[0], false)
                            idWilayahTerpilih = listRawWilayah[0].optString("id")
                            updateMapLocation(listRawWilayah[0])
                        }
                    }

                } catch (e: Exception) { e.printStackTrace() }
            },
            { error -> error.printStackTrace() }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun updateMapLocation(wilayahObj: JSONObject) {
        val lat = wilayahObj.optDouble("latitude", -7.77245)
        val lng = wilayahObj.optDouble("longitude", 111.9834)
        val radiusInMeters = wilayahObj.optDouble("radius", 5000.0)
        val namaWilayah = wilayahObj.optString("nama_wilayah", "Lokasi")

        val targetPoint = GeoPoint(lat, lng)
        mapView.controller.animateTo(targetPoint)

        mapView.overlays.clear()

        val marker = Marker(mapView)
        marker.position = targetPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Jangkauan: $namaWilayah"
        mapView.overlays.add(marker)

        val circlePoints = Polygon.pointsAsCircle(targetPoint, radiusInMeters)
        val circleOverlay = Polygon()
        circleOverlay.points = circlePoints
        circleOverlay.fillPaint.color = Color.parseColor("#224F46E5")
        circleOverlay.outlinePaint.color = Color.parseColor("#4F46E5")
        circleOverlay.outlinePaint.strokeWidth = 3f

        mapView.overlays.add(circleOverlay)
        mapView.invalidate()
    }

    private fun eksekusiCekJaringan() {
        if (idWilayahTerpilih.isEmpty()) {
            Toast.makeText(this, "Silakan pilih atau ketik nama wilayah dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = ApiConfig.BASE_URL + "landing-cek-jaringan"
        val post = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val pesan = JSONObject(response).optString("pesan")
                    AlertDialog.Builder(this).setTitle("Hasil Cek Jangkauan").setMessage(pesan).setPositiveButton("OK", null).show()
                } catch (e: Exception) { e.printStackTrace() }
            },
            { Toast.makeText(this, "Gagal memverifikasi", Toast.LENGTH_SHORT).show() }
        ) {
            override fun getParams(): MutableMap<String, String> = hashMapOf("wilayah_id" to idWilayahTerpilih)
        }
        VolleySingleton.getInstance(this).addToRequestQueue(post)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // PENTING: Jangan paksa auto-start kembali di sini jika user sengaja melakukan pause sebelum meminimalkan aplikasi
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        videoPromo.pause()
    }
}