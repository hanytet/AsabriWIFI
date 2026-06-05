package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomNav = findViewById(R.id.bottomNavigationView)

        // 1. Tampilkan DashboardFragment sebagai halaman pertama saat aplikasi dibuka
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        // 2. Atur sistem ganti layar saat tab bawah diklik
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_paket -> {
                    // Tampilkan PaketFragment saat diklik!
                    loadFragment(PaketFragment())
                    true
                }
                R.id.nav_pelanggan -> {
                    Toast.makeText(this, "Halaman Pelanggan belum dibuat", Toast.LENGTH_SHORT).show()
                    loadFragment(PelangganFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi canggih untuk menempelkan Fragment ke FrameLayout yang kosong tadi
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
    }

    // Menu Logout (Tidak berubah)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().clear().apply()

                Toast.makeText(this, "Berhasil Logout", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}