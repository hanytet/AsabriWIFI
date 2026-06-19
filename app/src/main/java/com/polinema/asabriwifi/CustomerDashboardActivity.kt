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
    private var idUserTerdaftar: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_dashboard)

        // 🚀 AMBIL ID DARI LOGIN: Mengambil ID_USER yang asli dan valid dari SharedPreferences hasil login
        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        idUserTerdaftar = sharedPreferences.getString("ID_USER", "") ?: ""

        bottomNav = findViewById(R.id.bottom_navigation_customer)

        // Set halaman default awal ke Home Fragment saat pertama kali dibuka
        if (savedInstanceState == null) {
            val homeFragment = HomePelangganFragment()
            homeFragment.arguments = buatBundleKirimID()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit()
        }

        // Penanganan klik perpindahan fragment menu bawah
        bottomNav.setOnItemSelectedListener { item ->
            val menu = bottomNav.menu
            for (i in 0 until menu.size()) {
                menu.getItem(i).isCheckable = true
            }

            var fragmentTerpilih: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> fragmentTerpilih = HomePelangganFragment()
                R.id.nav_paket -> fragmentTerpilih = PaketPelangganFragment()

                // Inisialisasi Tagihan Fragment
                R.id.nav_tagihan -> fragmentTerpilih = TagihanPelangganFragment()

                R.id.nav_riwayat -> fragmentTerpilih = RiwayatPembayaranFragment()
                R.id.nav_keluhan -> fragmentTerpilih = KeluhanPelangganFragment()
            }

            if (fragmentTerpilih != null) {
                // SUNTIK ID ASLI: Selipkan Bundle ID agar fragment tujuan membaca user_id akun yang sedang login
                fragmentTerpilih.arguments = buatBundleKirimID()

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragmentTerpilih)
                    .commit()
                true
            } else {
                false
            }
        }
    }
    
    private fun buatBundleKirimID(): Bundle {
        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
        val namaUser = sharedPreferences.getString("NAMA_USER", "Pelanggan") ?: "Pelanggan"

        val bundle = Bundle()
        bundle.putString("ARG_USER_ID", idUserTerdaftar)
        bundle.putString("ARG_NAMA_USER", namaUser)
        return bundle
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_customer_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_profile -> {
                val menu = bottomNav.menu
                for (i in 0 until menu.size()) {
                    menu.getItem(i).isCheckable = false
                }

                val profileFragment = ProfilePelangganFragment()
                profileFragment.arguments = buatBundleKirimID()

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .commit()
                return true
            }

            R.id.action_logout -> {
                val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().clear().apply()

                Toast.makeText(this, "Berhasil keluar sesi!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LandingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}