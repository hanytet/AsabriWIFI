package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    // 🚀 FIXED: Mengubah etEmail menjadi EditText biasa
    private lateinit var etEmail: EditText
    private lateinit var etNik: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inisialisasi komponen view (etEmail sekarang diikat sebagai EditText)
        etEmail = findViewById(R.id.etEmail)
        etNik = findViewById(R.id.etNik)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)

        // =========================================================================
        // LOGIKA KLIK TOMBOL LOGIN & REDIRECT BERDASARKAN ROLE
        // =========================================================================
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val nik = etNik.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && nik.isNotEmpty() && password.isNotEmpty()) {

                setLoadingState(true)

                val url = ApiConfig.BASE_URL + "login"

                val stringRequest = object : StringRequest(
                    Request.Method.POST, url,
                    { response ->
                        setLoadingState(false)
                        try {
                            val jsonObject = JSONObject(response)
                            val status = jsonObject.getString("status")

                            if (status == "berhasil") {
                                val idUser = jsonObject.getString("id_user")
                                val role = jsonObject.getString("role")

                                // Simpan status sesi ke local storage
                                sharedPreferences.edit()
                                    .putBoolean("IS_LOGGED_IN", true)
                                    .putString("ID_USER", idUser)
                                    .putString("ROLE", role)
                                    .apply()

                                Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()

                                // Logika redirect otomatis berdasarkan role backend Laravel
                                val intent = if (role.contains("admin", ignoreCase = true)) {
                                    Intent(this, MainActivity::class.java)
                                } else {
                                    Intent(this, CustomerDashboardActivity::class.java)
                                }

                                // Bersihkan tumpukan activity agar tidak bisa di-back kembali ke login
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                val pesan = jsonObject.optString("pesan", "Kombinasi akun salah!")
                                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: JSONException) {
                            Toast.makeText(this@LoginActivity, "Gagal memproses respons server", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    },
                    { error ->
                        setLoadingState(false)
                        val networkResponse = error.networkResponse
                        if (networkResponse != null) {
                            val statusCode = networkResponse.statusCode
                            val errorData = String(networkResponse.data)
                            Toast.makeText(this@LoginActivity, "Error $statusCode: $errorData", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@LoginActivity, "Gagal terhubung! Periksa kembali IP & Server Laravel", Toast.LENGTH_LONG).show()
                            error.printStackTrace()
                        }
                    }
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        return hashMapOf(
                            "email" to email,
                            "nik" to nik,
                            "password" to password
                        )
                    }
                }

                VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
            } else {
                Toast.makeText(this, "Harap isi email, NIK, dan password!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            btnLogin.alpha = 0.6f
        } else {
            progressBar.visibility = View.GONE
            btnLogin.isEnabled = true
            btnLogin.alpha = 1.0f
        }
    }
}