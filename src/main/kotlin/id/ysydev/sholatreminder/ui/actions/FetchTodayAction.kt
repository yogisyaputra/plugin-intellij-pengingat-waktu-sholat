package id.ysydev.sholatreminder.ui.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import id.ysydev.sholatreminder.api.MyQuranApiClient
import id.ysydev.sholatreminder.core.PrayerSettingsState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FetchTodayAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val settings = PrayerSettingsState.getInstance().state
        if (settings.cityId.isBlank()) {
            notifyErr("Pilih kota dulu", "Buka Settings/Sidebar → pilih kota (MyQuran).", e)
            return
        }

        object : Task.Backgroundable(e.project, "Fetching today's prayer times…", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val zone = zoneId(settings.timezoneId)
                    val client = MyQuranApiClient(settings.apiBaseUrl)
                    val data = client.fetchDailySchedule(settings.cityId, LocalDate.now(zone))

                    val content = buildString {
                        appendLine("${data.lokasi} • ${data.daerah}")
                        appendLine("Tanggal: ${data.jadwal.tanggal}")
                        appendLine()
                        // tampilkan lengkap (pakai label EN agar konsisten dengan UI lain)
                        appendLine("Subuh    : ${data.jadwal.subuh}")
                        appendLine("Dhuhr   : ${data.jadwal.dzuhur}")
                        appendLine("Asr     : ${data.jadwal.ashar}")
                        appendLine("Maghrib : ${data.jadwal.maghrib}")
                        appendLine("Isha    : ${data.jadwal.isya}")
                        // info tambahan (opsional, jika ada)
                        data.jadwal.imsak?.let   { appendLine("Imsak  : $it") }
                        data.jadwal.terbit?.let  { appendLine("Terbit : $it") }
                        data.jadwal.dhuha?.let   { appendLine("Dhuha  : $it") }
                    }

                    notifyInfo("Jadwal Sholat — ${settings.cityName}", content.trim(), e)
                } catch (ex: Throwable) {
                    notifyErr("Gagal ambil jadwal", ex.message ?: ex.toString(), e)
                }
            }
        }.queue()
    }

    private fun zoneId(tz: String?): ZoneId =
        if (!tz.isNullOrBlank()) ZoneId.of(tz) else ZoneId.systemDefault()

    private fun notifyInfo(title: String, content: String, e: AnActionEvent) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SholatReminderGroup")
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(e.project)
    }

    private fun notifyErr(title: String, content: String, e: AnActionEvent) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SholatReminderGroup")
            .createNotification(title, content, NotificationType.ERROR)
            .notify(e.project)
    }
}
