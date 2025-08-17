package id.ysydev.sholatreminder.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// ===== Models =====
data class CityHit(
    @Json(name = "id") val id: String,
    @Json(name = "lokasi") val lokasi: String
)
data class CitySearchResponse(
    @Json(name = "status") val status: Boolean,
    @Json(name = "data") val data: List<CityHit>?
)

data class Jadwal(
    @Json(name = "tanggal") val tanggal: String,
    @Json(name = "imsak") val imsak: String?,
    @Json(name = "subuh") val subuh: String,
    @Json(name = "terbit") val terbit: String?,
    @Json(name = "dhuha") val dhuha: String?,
    @Json(name = "dzuhur") val dzuhur: String,
    @Json(name = "ashar") val ashar: String,
    @Json(name = "maghrib") val maghrib: String,
    @Json(name = "isya") val isya: String,
    @Json(name = "date") val date: String
)
data class JadwalData(
    @Json(name = "id") val id: Int,
    @Json(name = "lokasi") val lokasi: String,
    @Json(name = "daerah") val daerah: String,
    @Json(name = "jadwal") val jadwal: Jadwal
)
data class JadwalResponse(
    @Json(name = "status") val status: Boolean,
    @Json(name = "data") val data: JadwalData?
)

class MyQuranApiClient(
    private val baseUrl: String = "https://api.myquran.com/v2",
    private val http: OkHttpClient = OkHttpClient(),
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())   // <— TAMBAHKAN
        .build()
) {
    fun searchCities(keyword: String, limit: Int = 10): List<CityHit> {
        val url = "$baseUrl/sholat/kota/cari/${keyword}"
        val req = Request.Builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            val adapter = moshi.adapter(CitySearchResponse::class.java)
            val parsed = adapter.fromJson(body) ?: error("Invalid JSON")
            if (!parsed.status) error("API status=false")
            return (parsed.data ?: emptyList()).take(limit)
        }
    }

    fun fetchDailySchedule(cityId: String, date: LocalDate): JadwalData {
        val yyyyMMdd = date.format(DateTimeFormatter.ISO_DATE) // 2025-08-15
        val url = "$baseUrl/sholat/jadwal/$cityId/$yyyyMMdd"
        val req = Request.Builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            val adapter = moshi.adapter(JadwalResponse::class.java)
            val parsed = adapter.fromJson(body) ?: error("Invalid JSON")
            if (!parsed.status || parsed.data == null) error("Invalid data")
            return parsed.data
        }
    }
}
