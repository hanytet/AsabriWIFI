package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TeknisiMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teknisi_main) // Pastikan Anda membuat file XML layout kosong dengan FrameLayout bernama fragment_container

        // Set default fragment ke dashboard utama teknisi saat pertama kali dimuat
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TeknisiDashboardFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Gunakan menu yang sama dengan customer untuk aksi logout
        menuInflater.inflate(R.menu.menu_customer_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            Toast.makeText(this, "Sesi Teknisi Berakhir!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}