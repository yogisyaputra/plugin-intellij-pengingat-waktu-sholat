package id.ysydev.sholatreminder.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType

class TestNotifAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SholatReminderGroup")
            .createNotification(
                "Pengingat Waktu Sholat",
                "Ini notif test ✅",
                NotificationType.INFORMATION
            )
            .notify(e.project)
    }
}
