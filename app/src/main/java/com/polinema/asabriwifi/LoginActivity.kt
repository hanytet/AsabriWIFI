package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)

        // 1. PERBAIKAN: Cek status "IS_LOGGED_IN", bukan mencari token lagi
        if (sharedPreferences.getBoolean("IS_LOGGED_IN", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                btnLogin.text = "Memproses..."
                btnLogin.isEnabled = false

                val url = "http://192.168.4.66/api/login"

                val stringRequest = object : StringRequest(
                    Request.Method.POST, url,
                    { response ->
                        btnLogin.text = "MASUK"
                        btnLogin.isEnabled = true
                        try {
                            val jsonObject = JSONObject(response)
                            val status = jsonObject.getString("status")

                            if (status == "berhasil") {
                                // 2. PERBAIKAN: Hapus pencarian token.
                                // Cukup simpan status login, ID user, dan rolenya
                                val idUser = jsonObject.getString("id_user")
                                val role = jsonObject.getString("role")

                                sharedPreferences.edit()
                                    .putBoolean("IS_LOGGED_IN", true)
                                    .putString("ID_USER", idUser)
                                    .putString("ROLE", role)
                                    .apply()

                                Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()

                                // Pindah ke MainActivity (Dashboard)
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                val pesan = jsonObject.getString("pesan")
                                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: JSONException) {
                            Toast.makeText(this@LoginActivity, "Gagal memproses JSON", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    },
                    { error ->
                        btnLogin.text = "MASUK"
                        btnLogin.isEnabled = true

                        // Menangkap pesan error asli dari Volley / Laravel
                        val networkResponse = error.networkResponse
                        if (networkResponse != null) {
                            val statusCode = networkResponse.statusCode
                            val errorData = String(networkResponse.data)
                            Toast.makeText(this@LoginActivity, "Error $statusCode: $errorData", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@LoginActivity, "Gagal konek! Pastikan IP benar & Laravel jalan", Toast.LENGTH_LONG).show()
                            error.printStackTrace()
                        }
                    }
                ) {
                    // Mengirimkan Email dan Password ke Laravel
                    override fun getParams(): MutableMap<String, String> {
                        val params = HashMap<String, String>()
                        params["email"] = email
                        params["password"] = password
                        return params
                    }
                }

                VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
            } else {
                Toast.makeText(this, "Isi email dan password!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}