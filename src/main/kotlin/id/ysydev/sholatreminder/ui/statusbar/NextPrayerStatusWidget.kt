package id.ysydev.sholatreminder.ui.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.IconUtil
import com.intellij.openapi.util.IconLoader
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import id.ysydev.sholatreminder.core.PrayerSchedulerService
import id.ysydev.sholatreminder.core.PrayerSettingsState
import id.ysydev.sholatreminder.util.TestNotifier
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/** Factory untuk menampilkan widget di Customize Status Bar */
class NextPrayerStatusWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "NextPrayerCountdown"
    override fun getDisplayName(): String = "Next Prayer Countdown"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = NextPrayerStatusWidget()
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class NextPrayerStatusWidget : CustomStatusBarWidget {

    private var statusBar: StatusBar? = null
    private val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0))
    private val iconLabel = JBLabel()
    private val textLabel = JBLabel("Next: —")

    private val timer = Timer(1000) { updateText() } // update per detik
    private val fmtTime = DateTimeFormatter.ofPattern("HH:mm")

    init {
        panel.isOpaque = false
        // ikon default (mis. fajr)
        val baseIcon = IconLoader.getIcon("/icons/icon.svg", javaClass)
        iconLabel.icon = IconUtil.scale(baseIcon, null, 1.0f)
        textLabel.foreground = JBColor.foreground()

        panel.add(iconLabel)
        panel.add(textLabel)

        // klik → buka popup
        panel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        val clicker = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = showTodayPopup(e)
        }
        panel.addMouseListener(clicker)
        iconLabel.addMouseListener(clicker)
        textLabel.addMouseListener(clicker)
    }

    override fun ID(): String = "NextPrayerCountdown"
    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        timer.start()
        updateText()
    }
    override fun dispose() { timer.stop(); statusBar = null }
    override fun getComponent() = panel

    /** Hitung label + update ikon & tooltip */
    private fun updateText() {
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                val svc = PrayerSchedulerService.instance()
                val target = svc.nextCountdownTarget()
                if (target == null) {
                    setText("Next: —", "No data yet", baseIconPath = "/icons/icon.svg")
                    return@submit
                }
                val (label, zdt) = target
                val now = ZonedDateTime.now(zdt.zone)
                val dur = Duration.between(now, zdt)
                val remaining = if (!dur.isNegative) dur else Duration.ZERO
                val hh = remaining.toHours()
                val mm = (remaining.toMinutes() % 60)
                val ss = (remaining.seconds % 60)

                val city = PrayerSettingsState.getInstance().state.cityName
                val text = "%s in %02d:%02d:%02d • %s".format(label, hh, mm, ss, city)
                val tip = "Target: %s pukul %s".format(label, fmtTime.format(zdt))

                // set ikon sesuai nama sholat ("Dhuhr -10m" → "Dhuhr")
                val prayerName = label.substringBefore(" ").trim()
                val iconPath = iconPathFor(prayerName)

                setText(text, tip, baseIconPath = iconPath)
            } catch (_: Throwable) {
                setText("Next: —", "Error computing next prayer", baseIconPath = "/icons/icon.svg")
            }
        }
    }

    private fun setText(label: String, tooltip: String, baseIconPath: String) {
        SwingUtilities.invokeLater {
            textLabel.text = label
            textLabel.toolTipText = tooltip
            panel.toolTipText = tooltip
            try {
                val icon = IconLoader.getIcon(baseIconPath, javaClass)
                iconLabel.icon = IconUtil.scale(icon, null, 1.0f)
            } catch (_: Throwable) { /* ignore icon errors */ }
            statusBar?.updateWidget(ID())
        }
    }

    /** Map nama sholat ke ikon (IntelliJ otomatis pakai *_dark.svg jika theme dark) */
    private fun iconPathFor(name: String): String = when (name) {
        "Subuh"    -> "/icons/fajr.svg"
        "Dhuhr"   -> "/icons/dhuhr.svg"
        "Asr"     -> "/icons/asr.svg"
        "Maghrib" -> "/icons/maghrib.svg"
        "Isha"    -> "/icons/isha.svg"
        else      -> "/icons/fajr.svg"
    }

    // ==========================
    // ===== Popup Detail UI ====
    // ==========================
    private fun showTodayPopup(e: MouseEvent) {
        val svc = PrayerSchedulerService.instance()
        val settings = PrayerSettingsState.getInstance().state
        val times = svc.todayTimes()

        val main = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(12, 16)
            isOpaque = false
        }

        // Header
        val header = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            val title = JBLabel("Jadwal Sholat — ${settings.cityName}").apply {
                font = font.deriveFont(Font.BOLD)
            }
            val dateStr = java.time.LocalDate.now(
                if (settings.timezoneId.isNotBlank()) java.time.ZoneId.of(settings.timezoneId) else java.time.ZoneId.systemDefault()
            ).format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", java.util.Locale("id")))
            val subtitle = JBLabel(dateStr).apply { foreground = JBColor.GRAY }
            val col = JBPanel<JBPanel<*>>(GridLayout(2, 1)).apply {
                isOpaque = false
                add(title); add(subtitle)
            }
            add(col, BorderLayout.CENTER)
            add(JSeparator(), BorderLayout.SOUTH)
            border = JBUI.Borders.emptyBottom(6)
        }
        main.add(header, BorderLayout.NORTH)

        // Konten jadwal
        val content = JBPanel<JBPanel<*>>().apply {
            isOpaque = false
            layout = GridBagLayout()
            border = JBUI.Borders.empty(4, 0, 8, 0)
        }

        fun addRow(y: Int, label: String, value: String) {
            val gbcL = GridBagConstraints().apply {
                gridx = 0; gridy = y
                anchor = GridBagConstraints.EAST
                insets = JBUI.insets(2, 0, 2, 10)
            }
            val gbcV = GridBagConstraints().apply {
                gridx = 1; gridy = y
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(2, 0, 2, 0)
            }
            val name = JBLabel(label)
            val time = JBLabel(value).apply { font = font.deriveFont(Font.BOLD) }
            content.add(name, gbcL)
            content.add(time, gbcV)
        }

        val order = listOf("Subuh","Dhuhr","Asr","Maghrib","Isha")
        order.forEachIndexed { idx, name -> addRow(idx, name, times[name] ?: "—") }

        main.add(JScrollPane(content).apply {
            border = JBUI.Borders.empty()
            viewport.isOpaque = false
            isOpaque = false
        }, BorderLayout.CENTER)

        // Footer actions
        val footer = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            add(com.intellij.ui.TitledSeparator(""), BorderLayout.NORTH)
            border = JBUI.Borders.emptyTop(8)
        }
        val actions = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0)).apply { isOpaque = false }
        val btnSettings = JButton("Settings…").apply {
            addActionListener {
                com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                    .showSettingsDialog(null, "Pengingat Sholat")
            }
        }
        val btnRefresh = JButton("Refresh now")
        val btnClose = JButton("Close")
        actions.add(btnSettings); actions.add(btnRefresh); actions.add(btnClose)
        footer.add(actions, BorderLayout.SOUTH)
        main.add(footer, BorderLayout.SOUTH)

        // Build popup
        val popup: JBPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(main, null)
            .setTitle("Jadwal Sholat Hari Ini")
            .setResizable(false)
            .setMovable(true)
            .setRequestFocus(false)
            .createPopup()

        btnRefresh.addActionListener {
            try {
                svc.reschedule()
                TestNotifier.notifyInfo("Jadwal di‑refresh", "Jadwal sholat berhasil diambil ulang")
                popup.cancel()
            } catch (ex: Throwable) {
                TestNotifier.notifyInfo("Gagal refresh", ex.message ?: ex.toString())
            }
        }
        btnClose.addActionListener { popup.cancel() }

        popup.show(RelativePoint.getSouthWestOf(panel))
    }
}
