package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomerDashboardActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_dashboard)

        // Hubungkan ID navigasi bawah dari xml
        bottomNav = findViewById(R.id.bottom_navigation_customer)

        // Set halaman default awal ke Home Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomePelangganFragment())
                .commit()
        }

        // Penanganan klik perpindahan fragment menu bawah
        bottomNav.setOnItemSelectedListener { item ->
            var fragmentTerpilih: Fragment? = null
            when (item.itemId) {
                // 1. Menu Beranda Pelanggan
                R.id.nav_home -> fragmentTerpilih = HomePelangganFragment()

                // 2. Menu Paket Pelanggan
                R.id.nav_paket -> fragmentTerpilih = PaketPelangganFragment()

                // 🚀 FIXED SINKRONISASI: Sekarang mengarah langsung ke modul Tagihan Pelanggan milik Tuan
                R.id.nav_tagihan -> fragmentTerpilih = TagihanPelangganFragment()
            }

            if (fragmentTerpilih != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragmentTerpilih)
                    .commit()
                true
            } else {
                false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_customer_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            Toast.makeText(this, "Berhasil keluar sesi, Tuan Rihan!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}