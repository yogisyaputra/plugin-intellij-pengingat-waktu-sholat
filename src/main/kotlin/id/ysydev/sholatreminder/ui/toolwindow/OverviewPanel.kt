package id.ysydev.sholatreminder.ui.toolwindow

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JEditorPane
import javax.swing.JScrollPane

class OverviewPanel : JBPanel<OverviewPanel>(BorderLayout()) {

    init {
        border = JBUI.Borders.empty(10, 14)

        // Header HTML kecil (pakai JEditorPane biar rapi)
        val html = JEditorPane(
            "text/html", """
            <html>
              <head>
                <style>
                  body { font-family: Segoe UI, sans-serif; font-size: ${JBUI.scale(12)}px; color: ${rgb(JBColor.foreground())}; }
                  h1 { font-size: ${JBUI.scale(16)}px; margin: 0 0 ${JBUI.scale(6)}px 0; }
                  ul { margin: ${JBUI.scale(6)}px 0 ${JBUI.scale(10)}px 20px; }
                  li { margin: ${JBUI.scale(2)}px 0; }
                  .cap { color: ${rgb(JBColor.GRAY)}; font-size: ${JBUI.scale(11)}px; margin-top: ${JBUI.scale(2)}px; }
                </style>
              </head>
              <body>
                <h1>Pengingat Sholat — Overview</h1>
                <h2>Pengingat Sholat adalah plugin untuk IntelliJ IDEA (dan IDE JetBrains lain) yang menampilkan jadwal sholat harian dan countdown menuju adzan.
Cocok buat developer muslim yang sering lupa waktu sholat karena fokus ngoding</h2>
                <ul>
                  <li>Ambil jadwal sholat dari API eksternal (MyQuran)</li>
                  <li>Notifikasi pengingat sholat (bisa diatur offset, misalnya 5/10 menit sebelum adzan)</li>
                  <li>Countdown di Status Bar menuju adzan berikutnya / reminder.</li>
                  <li>Tool Window khusus untuk konfigurasi cepat</li>
                  <li>Popup detail jadwal harian saat klik ikon di status bar.</li>
                  <li>Refresh jadwal langsung dari API.</li>
                  <li>Ikon adaptif sesuai tema (light/dark mode).</li>
                </ul>
              </body>
            </html>
        """.trimIndent()
        ).apply {
            isEditable = false
            isOpaque = false
            border = JBUI.Borders.empty(0, 0, 8, 0)
        }

        val images = listOf(
            "sidebar.png" to "Sidebar — ubah city/offset & refresh",
            "settings.png" to "Settings — konfigurasi lengkap",
            "statusbar.png" to "Status bar — countdown + ikon dinamis",
            "popup.png" to "Popup — jadwal hari ini + Refresh now"
        )

        val imagesPanel = JBPanel<JBPanel<*>>().apply {
            isOpaque = false
            layout = GridBagLayout()
        }

        images.forEachIndexed { i, (file, caption) ->
            val icon = IconLoader.getIcon("/screenshots/$file", javaClass) // returns Icon
            val pic = JBLabel(icon).apply { border = JBUI.Borders.empty(8, 0, 2, 0) }
            val cap = JBLabel(caption).apply {
                foreground = JBColor.GRAY
                border = JBUI.Borders.empty(0, 0, 10, 0)
            }
            val gbcImg = gbc(0, i * 2)
            val gbcCap = gbc(0, i * 2 + 1)
            imagesPanel.add(pic, gbcImg)
            imagesPanel.add(cap, gbcCap)
        }

        val scroll = JScrollPane(imagesPanel).apply {
            border = JBUI.Borders.empty()
            viewport.isOpaque = false
            isOpaque = false
        }

        add(html, BorderLayout.NORTH)
        add(scroll, BorderLayout.CENTER)
    }

    private fun gbc(x: Int, y: Int) = GridBagConstraints().apply {
        gridx = x; gridy = y
        weightx = 1.0
        anchor = GridBagConstraints.NORTHWEST
        insets = JBUI.insets(0, 0, 0, 0)
    }

    private fun rgb(c: Color) = "rgb(${c.red},${c.green},${c.blue})"
}
