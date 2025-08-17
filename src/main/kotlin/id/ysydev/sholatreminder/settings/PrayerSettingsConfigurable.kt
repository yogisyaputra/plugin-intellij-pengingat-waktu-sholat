package id.ysydev.sholatreminder.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.*
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import id.ysydev.sholatreminder.api.CityHit
import id.ysydev.sholatreminder.api.MyQuranApiClient
import id.ysydev.sholatreminder.core.CountdownMode
import id.ysydev.sholatreminder.core.PrayerSchedulerService
import id.ysydev.sholatreminder.core.PrayerSettingsState
import id.ysydev.sholatreminder.util.TestNotifier
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.max

class PrayerSettingsConfigurable : Configurable {
    private val state get() = PrayerSettingsState.getInstance().state

    private lateinit var panel: JPanel
    private lateinit var cityQueryField: JBTextField
    private lateinit var searchBtn: JButton
    private lateinit var statusLabel: JBLabel
    private lateinit var resultsList: JBList<CityHit>
    private lateinit var resultsScroll: JScrollPane
    private val resultsModel = DefaultListModel<CityHit>()
    private lateinit var cityNameField: JBTextField
    private lateinit var cityIdField: JBTextField
    private lateinit var offsetsField: JBTextField
    private lateinit var notifCheckbox: JBCheckBox

    private lateinit var modeAdhan: JBRadioButton
    private lateinit var modeReminder: JBRadioButton
    private lateinit var modeGroup: ButtonGroup

    private val client = MyQuranApiClient()
    private var debounceFuture: ScheduledFuture<*>? = null
    private var lastQuery: String = ""

    override fun getDisplayName(): String = "Pengingat Sholat"

    override fun createComponent(): JComponent {
        cityQueryField = JBTextField().apply { emptyText.text = "Ketik keyword kota (mis. padang)" }
        searchBtn = JButton("Search")
        statusLabel = JBLabel("").apply {
            foreground = JBColor.GRAY
            border = JBUI.Borders.empty(4, 2, 2, 2)
        }

        resultsList = JBList(resultsModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            visibleRowCount = 5
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
                ): Component {
                    val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                    val v = value as CityHit
                    c.text = "${v.lokasi}"
                    return c
                }
            }
        }

        cityNameField = JBTextField(state.cityName).apply { isEditable = false }
        cityIdField = JBTextField(state.cityId).apply { isEditable = false }
        offsetsField = JBTextField(state.preAdhanOffsetsCsv)
        notifCheckbox = JBCheckBox("Aktifkan notifikasi", state.enableNotifications)

        modeAdhan = JBRadioButton("Countdown ke Adzan", state.countdownMode == CountdownMode.ADHAN)
        modeReminder = JBRadioButton("Countdown ke Reminder (offset)", state.countdownMode == CountdownMode.REMINDER)
        modeGroup = ButtonGroup().apply { add(modeAdhan); add(modeReminder) }

        fun setStatusInfo(text: String) {
            statusLabel.text = text
            statusLabel.foreground = JBColor.GRAY
        }

        fun setStatusError(text: String) {
            statusLabel.text = text
            statusLabel.foreground = JBColor.RED
        }

        fun launchSearch(q: String) {
            val query = q.trim()
            resultsModel.removeAllElements()
            adjustResultsViewport(0)
            if (query.isBlank()) {
                setStatusInfo("Masukkan keyword kota, lalu klik Search atau tekan Enter.")
                return
            }
            setStatusInfo("Searching “$query”…")

            AppExecutorUtil.getAppExecutorService().submit {
                try {
                    val hits = client.searchCities(query, limit = 10)
                    SwingUtilities.invokeLater {
                        resultsModel.removeAllElements()
                        hits.forEach { resultsModel.addElement(it) }
                        if (!resultsModel.isEmpty) {
                            resultsList.selectedIndex = 0
                            setStatusInfo("Menampilkan ${hits.size} hasil untuk “$query”.")
                        } else {
                            setStatusInfo("Tidak ada hasil untuk “$query”.")
                        }
                        adjustResultsViewport(hits.size)
                    }
                } catch (ex: Throwable) {
                    SwingUtilities.invokeLater {
                        resultsModel.removeAllElements()
                        adjustResultsViewport(0)
                        setStatusError("Gagal Mendapatkan List Kota (${ex.javaClass.simpleName}). Lihat Help → Show Log.")
                        TestNotifier.notifyInfo("MyQuran API error", ex.message ?: ex.toString())
                    }
                }
            }
        }

        fun debounceSearch() {
            val q = cityQueryField.text.trim()
            lastQuery = q
            debounceFuture?.cancel(true)
            debounceFuture = AppExecutorUtil.getAppScheduledExecutorService()
                .schedule({ if (lastQuery == q) launchSearch(q) }, 350, TimeUnit.MILLISECONDS)
        }

        searchBtn.addActionListener { launchSearch(cityQueryField.text) }
        cityQueryField.addActionListener { launchSearch(cityQueryField.text) } // Enter
        cityQueryField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = debounceSearch()
            override fun removeUpdate(e: DocumentEvent) = debounceSearch()
            override fun changedUpdate(e: DocumentEvent) = debounceSearch()
        })

        resultsList.addListSelectionListener {
            val sel = resultsList.selectedValue ?: return@addListSelectionListener
            cityNameField.text = sel.lokasi
            cityIdField.text = sel.id
        }
        resultsList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val sel = resultsList.selectedValue ?: return
                    cityNameField.text = sel.lokasi
                    cityIdField.text = sel.id
                }
            }
        })

        panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10, 12)
        }

        fun row(label: String, comp: JComponent) = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JBLabel("$label: "))
            add(Box.createHorizontalStrut(8))
            add(comp)
        }

        val searchRow = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(cityQueryField, BorderLayout.CENTER)
            add(searchBtn, BorderLayout.EAST)
        }
        panel.add(row("Cari Kota", searchRow))
        panel.add(statusLabel)

        resultsScroll = JScrollPane(resultsList).apply {
            border = JBUI.Borders.empty(6, 16, 6, 0)
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }
        panel.add(resultsScroll)

        panel.add(row("Kota", cityNameField))
        panel.add(JSeparator())
        panel.add(row("Notif sebelum adzan (CSV)", offsetsField))

        panel.add(JSeparator())
        panel.add(JBLabel("Mode Countdown:"))
        panel.add(modeAdhan)
        panel.add(modeReminder)

        panel.add(JSeparator())
        panel.add(notifCheckbox)

        // Status & ukuran awal
        setStatusInfo("Masukkan keyword kota, lalu klik Search atau tekan Enter.")
        adjustResultsViewport(resultsModel.size)
        return panel
    }

    override fun isModified(): Boolean = true // sederhanakan: izinkan Apply kapan saja

    override fun apply() {
        state.cityName = cityNameField.text
        state.cityId = cityIdField.text
        state.preAdhanOffsetsCsv = offsetsField.text
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .map { max(0, it) }
            .sorted()
            .joinToString(",")
            .ifEmpty { "10,5" }

        state.enableNotifications = notifCheckbox.isSelected
        state.countdownMode = if (modeReminder.isSelected) CountdownMode.REMINDER else CountdownMode.ADHAN

        PrayerSchedulerService.instance().reschedule()
        TestNotifier.notifyInfo("Settings tersimpan", "Jadwal & reminder diperbarui • ${state.cityName}")
    }

    private fun adjustResultsViewport(count: Int) {
        val rows = when {
            count <= 0 -> 1
            count < 5 -> count
            else -> 5
        }
        resultsList.visibleRowCount = rows

        val cellH = if (resultsList.fixedCellHeight > 0) {
            resultsList.fixedCellHeight
        } else {
            val fm = resultsList.getFontMetrics(resultsList.font)
            fm.height + 17 + JBUI.scale(7)
        }

        val height = cellH * rows + JBUI.scale(6)
        val width = JBUI.scale(420)

        resultsScroll.preferredSize = Dimension(width, height)
        resultsScroll.minimumSize = Dimension(JBUI.scale(200), JBUI.scale(36))
        resultsScroll.maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(400))
        resultsScroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        resultsScroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

        resultsScroll.revalidate()
        resultsScroll.repaint()
    }


}
