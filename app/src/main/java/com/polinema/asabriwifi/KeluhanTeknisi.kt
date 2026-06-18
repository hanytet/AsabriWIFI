package com.polinema.asabriwifi

data class KeluhanTeknisi(
    val id: String,
    val hariKeluhan: String,
    val tanggalKeluhan: String,
    val namaPelanggan: String,
    val noHp: String,
    val alamatTujuan: String,
    val kategori: String,
    val isiKeluhan: String,
    val catatanAdmin: String,
    val status: String,
    val statusLabel: String,
    val teknisiId: String?
)