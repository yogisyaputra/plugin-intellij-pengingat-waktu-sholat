// src/main/kotlin/id/ysydev/sholatreminder/ui/actions/DiagnoseMyQuranAction.kt
package id.ysydev.sholatreminder.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.Messages
import id.ysydev.sholatreminder.api.MyQuranApiClient

class DiagnoseMyQuranAction : AnAction("Diagnose MyQuran API") {
    private val LOG = logger<DiagnoseMyQuranAction>()
    override fun actionPerformed(e: AnActionEvent) {
        try {
            val client = MyQuranApiClient()
            val hits = client.searchCities("jakarta", 3)
            Messages.showInfoMessage("OK. Contoh hasil:\n" + hits.joinToString("\n") { "${it.lokasi} [${it.id}]" }, "MyQuran OK")
        } catch (ex: Throwable) {
            LOG.warn("Diagnose failed", ex)
            Messages.showErrorDialog("${ex.javaClass.simpleName}\n${ex.message}\n\nCek idea.log untuk detail.", "MyQuran Error")
        }
    }
}
