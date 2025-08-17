package id.ysydev.sholatreminder.ui.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import id.ysydev.sholatreminder.core.PrayerSchedulerService

class SholatToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setIcon(IconLoader.getIcon("/icons/icon.svg", javaClass))


        val contentFactory = ContentFactory.getInstance()

//        val overview = OverviewPanel()
//        val overviewContent = contentFactory.createContent(overview, "Overview", false)

        val settingsPanel = SholatToolWindowPanel()
        val settingsContent = contentFactory.createContent(settingsPanel, "Settings", false)

//        toolWindow.contentManager.addContent(overviewContent)
        toolWindow.contentManager.addContent(settingsContent)
    }
}
