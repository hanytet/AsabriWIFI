package com.polinema.asabriwifi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etNik: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var layoutKembali: LinearLayout
    private lateinit var tvDaftarSekarang: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etNik = findViewById(R.id.etNik)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        layoutKembali = findViewById(R.id.layoutKembali)
        tvDaftarSekarang = findViewById(R.id.tvDaftarSekarang)

        val sharedPreferences = getSharedPreferences("AsabriPrefs", Context.MODE_PRIVATE)

        layoutKembali.setOnClickListener {
            finish()
        }

        // PERBAIKAN SINKRONISASI: Menghubungkan teks daftar langsung ke RegisterActivity
        tvDaftarSekarang.setOnClickListener {
            Toast.makeText(this, "Membuka Menu Pendaftaran Pelanggan Baru", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val nik = etNik.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                setLoadingState(true)
                val url = ApiConfig.BASE_URL + "login"

                val stringRequest = object : StringRequest(
                    Request.Method.POST, url,
                    { response ->
                        setLoadingState(false)
                        try {
                            Log.d("AsabriDebug", "Respon Login Server: $response")
                            val jsonObject = JSONObject(response)
                            val status = jsonObject.optString("status")

                            if (status == "berhasil") {
                                var idUser = jsonObject.optString("id", "")
                                if (idUser.isEmpty()) {
                                    idUser = jsonObject.optString("id_user", "")
                                }

                                val role = jsonObject.optString("role", "")

                                if (idUser.isEmpty() || idUser == "null") {
                                    Toast.makeText(this@LoginActivity, "FATAL: Server tidak mengirimkan ID User!", Toast.LENGTH_LONG).show()
                                } else {
                                    sharedPreferences.edit()
                                        .putBoolean("IS_LOGGED_IN", true)
                                        .putString("ID_USER", idUser.trim())
                                        .putString("ROLE", role.trim())
                                        .commit()

                                    Toast.makeText(this@LoginActivity, "Login Sukses! (ID: $idUser)", Toast.LENGTH_SHORT).show()

                                    val intent = when {
                                        role.contains("admin", ignoreCase = true) -> {
                                            Intent(this@LoginActivity, MainActivity::class.java)
                                        }
                                        role.contains("teknisi", ignoreCase = true) -> {
                                            Intent(this@LoginActivity, TeknisiMainActivity::class.java)
                                        }
                                        else -> {
                                            Intent(this@LoginActivity, CustomerDashboardActivity::class.java)
                                        }
                                    }

                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                val pesan = jsonObject.optString("pesan", "Kombinasi akun salah!")
                                Toast.makeText(this@LoginActivity, pesan, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@LoginActivity, "Gagal mengekstrak data login", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    },
                    { error ->
                        setLoadingState(false)
                        val statusCode = error.networkResponse?.statusCode
                        Log.e("AsabriError", "Login Volley Error Code: $statusCode")
                        Toast.makeText(this@LoginActivity, "Gagal terhubung ke server (Code: $statusCode)", Toast.LENGTH_SHORT).show()
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

                stringRequest.setShouldCache(false)
                VolleySingleton.getInstance(this).addToRequestQueue(stringRequest)
            } else {
                Toast.makeText(this, "Harap isi Email dan Password Anda!", Toast.LENGTH_SHORT).show()
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