package id.ysydev.sholatreminder.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType

object TestNotifier {
    fun notifyInfo(title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("SholatReminderGroup")
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(null)
    }
}
