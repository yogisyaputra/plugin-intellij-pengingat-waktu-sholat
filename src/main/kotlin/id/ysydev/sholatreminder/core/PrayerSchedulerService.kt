package id.ysydev.sholatreminder.core

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.concurrency.AppExecutorUtil
import id.ysydev.sholatreminder.api.MyQuranApiClient
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class PrayerSchedulerService : Disposable {

    private val exec = AppExecutorUtil.getAppScheduledExecutorService()
    private var dailyRefresh: ScheduledFuture<*>? = null
    private val reminders = mutableListOf<ScheduledFuture<*>>()
    private val settings get() = PrayerSettingsState.getInstance().state

    // Jadwal harian terakhir (dipakai status bar & popup)
    private var latestTimes: Map<String, String> = emptyMap() // keys: Fajr, Dhuhr, Asr, Maghrib, Isha

    companion object {
        fun instance(): PrayerSchedulerService =
            ApplicationManager.getApplication().getService(PrayerSchedulerService::class.java)
    }

    /** Dipanggil saat IDE start */
    fun start() {
        scheduleForToday()
        scheduleDailyRefresh()
    }

    /** Dipanggil saat Settings di-apply / city berubah */
    fun reschedule() {
        cancelReminders()
        scheduleForToday()
    }

    /** Refresh otomatis tiap hari (sekitar 00:05) */
    private fun scheduleDailyRefresh() {
        dailyRefresh?.cancel(false)
        val now = ZonedDateTime.now(zoneId())
        val next = now.with(LocalTime.of(0, 5)).let { if (it.isBefore(now)) it.plusDays(1) else it }
        val initialDelay = Duration.between(now, next).seconds
        dailyRefresh = exec.scheduleAtFixedRate(
            { safe { reschedule() } },
            initialDelay,
            24 * 3600,
            TimeUnit.SECONDS
        )
    }

    /** Ambil jadwal MyQuran untuk hari ini lalu jadwalkan reminder sesuai offsets */
    private fun scheduleForToday() {
        try {
            if (settings.cityId.isBlank()) {
                notifyInfo("Pengingat Sholat", "Pilih kota dulu di Settings/Sidebar.")
                latestTimes = emptyMap()
                return
            }

            val client = MyQuranApiClient(settings.apiBaseUrl)
            val today = LocalDate.now(zoneId())
            val data = client.fetchDailySchedule(settings.cityId, today)

            // Peta nama Indonesia → key EN konsisten
            val times = mapOf(
                "Subuh" to data.jadwal.subuh,
                "Dhuhr" to data.jadwal.dzuhur,
                "Asr" to data.jadwal.ashar,
                "Maghrib" to data.jadwal.maghrib,
                "Isha" to data.jadwal.isya
            )
            latestTimes = times

            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            val offsets = parsedOffsets()
            val zone = zoneId()
            val now = ZonedDateTime.now(zone)

            listOf("Subuh", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
                val hhmm = times[name] ?: return@forEach
                val adhan = ZonedDateTime.of(today, LocalTime.parse(hhmm, fmt), zone)

                // Jadwalkan semua reminder untuk sholat ini
                offsets.forEach { off ->
                    val remindAt = adhan.minusMinutes(off.toLong())
                    val delaySec = Duration.between(now, remindAt).seconds
                    if (delaySec > 0) {
                        reminders += exec.schedule({
                            notifyInfo(
                                "$name dalam $off menit",
                                "Adzan $name ${adhan.toLocalTime()} • ${settings.cityName}"
                            )
                        }, delaySec, TimeUnit.SECONDS)
                    }
                }
            }

        } catch (e: Throwable) {
            notifyErr("Gagal jadwalkan sholat", e.message ?: e.toString())
            latestTimes = emptyMap()
        }
    }

    private fun cancelReminders() {
        reminders.forEach { it.cancel(false) }
        reminders.clear()
    }

    private fun parsedOffsets(): List<Int> =
        settings.preAdhanOffsetsCsv
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 0 }
            .sorted()

    private fun zoneId(): ZoneId =
        if (settings.timezoneId.isNotBlank()) ZoneId.of(settings.timezoneId) else ZoneId.systemDefault()

    private fun notifyInfo(title: String, content: String) {
        if (!settings.enableNotifications) return
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SholatReminderGroup")
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(null)
    }

    private fun notifyErr(title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SholatReminderGroup")
            .createNotification(title, content, NotificationType.ERROR)
            .notify(null)
    }

    private inline fun safe(block: () -> Unit) {
        try { block() } catch (t: Throwable) {
            notifyErr("Error", t.message ?: t.toString())
        }
    }

    override fun dispose() {
        dailyRefresh?.cancel(true)
        cancelReminders()
    }

    // =======================
    // ==== Dipakai oleh UI ==
    // =======================

    /** Kembalikan jadwal harian terakhir yang di-fetch */
    fun todayTimes(): Map<String, String> = latestTimes

    /** Hitung target countdown berikutnya: ke Adzan atau ke Reminder (offset) */
    fun nextCountdownTarget(): Pair<String, ZonedDateTime>? {
        if (latestTimes.isEmpty()) return null
        val zone = zoneId()
        val today = LocalDate.now(zone)
        val now = ZonedDateTime.now(zone)
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val order = listOf("Subuh", "Dhuhr", "Asr", "Maghrib", "Isha")

        val candidates = mutableListOf<Pair<String, ZonedDateTime>>()
        for (name in order) {
            val hhmm = latestTimes[name] ?: continue
            val adhan = ZonedDateTime.of(today, LocalTime.parse(hhmm, fmt), zone)
            when (settings.countdownMode) {
                CountdownMode.ADHAN -> if (adhan.isAfter(now)) candidates += name to adhan
                CountdownMode.REMINDER -> parsedOffsets().forEach { m ->
                    val t = adhan.minusMinutes(m.toLong())
                    if (t.isAfter(now)) candidates += "${name} -${m}m" to t
                }
            }
        }

        if (candidates.isNotEmpty()) return candidates.minByOrNull { it.second }

        // Semua target hari ini sudah lewat → fallback ke Fajr besok
        val subuh = latestTimes["Subuh"] ?: return null
        val base = ZonedDateTime.of(today.plusDays(1), LocalTime.parse(subuh, fmt), zone)
        return when (settings.countdownMode) {
            CountdownMode.ADHAN -> "Subuh" to base
            CountdownMode.REMINDER -> {
                val offs = parsedOffsets()
                offs.firstOrNull()?.let { "Subuh -${it}m" to base.minusMinutes(it.toLong()) } ?: ("Subuh" to base)
            }
        }
    }
}
