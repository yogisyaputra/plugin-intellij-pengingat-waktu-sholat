package id.ysydev.sholatreminder

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import id.ysydev.sholatreminder.core.PrayerSchedulerService
import id.ysydev.sholatreminder.util.TestNotifier

class AppStartup : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {

        PrayerSchedulerService.instance().start()
        TestNotifier.notifyInfo("Pengingat Waktu Sholat aktif", "Scheduler dijalankan")
//        NotificationGroupManager.getInstance()
//            .getNotificationGroup("SholatReminderGroup")
//            .createNotification(
//                "Pengingat Waktu Sholat",
//                "Plugin aktif. 🎉 (kerangka siap)",
//                NotificationType.INFORMATION
//            )
//            .notify(project)
    }
}
