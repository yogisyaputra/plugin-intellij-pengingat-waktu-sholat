package id.ysydev.sholatreminder.ui.toolwindow

import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import id.ysydev.sholatreminder.core.CountdownMode
import id.ysydev.sholatreminder.core.PrayerSchedulerService
import id.ysydev.sholatreminder.core.PrayerSettingsState
import id.ysydev.sholatreminder.ui.components.CityPickerDialog
import id.ysydev.sholatreminder.util.TestNotifier
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel

class SholatToolWindowPanel : JPanel(BorderLayout()) {

    private val state = PrayerSettingsState.getInstance().state

    // Fields
    private val cityNameField = JBTextField(state.cityName).apply { isEditable = false }
    private val cityIdField   = JBTextField(state.cityId).apply   { isEditable = false }
    private val apiField      = JBTextField(state.apiBaseUrl)
    private val tzField       = JBTextField(state.timezoneId)
    private val offsetsField  = JBTextField(state.preAdhanOffsetsCsv)
    private val notifCheck    = JBCheckBox("Enable notifications", state.enableNotifications)

    init {
        border = JBUI.Borders.empty(10, 12)
        add(buildForm(), BorderLayout.CENTER)
        add(buildActions(), BorderLayout.SOUTH)
    }

    private fun buildForm(): JPanel {
        val panel = JPanel(GridBagLayout())

        // Row 0: Tombol "Pilih Kota…" — full width, dirapikan ke kanan
        val btnPickCity = JButton("Pilih Kota…").apply {
            addActionListener {
                val dlg = CityPickerDialog(null)
                if (dlg.showAndGet()) {
                    dlg.selected?.let { hit ->
                        cityNameField.text = hit.lokasi
                        cityIdField.text = hit.id
                    }
                }
            }
            // biar tombol mau melar horizontal mengikuti grid
            val pref = preferredSize
            maximumSize = Dimension(Int.MAX_VALUE, pref.height)
        }
        panel.add(
            btnPickCity,
            gbc(
                x = 0, y = 0,
                anchor = GridBagConstraints.EAST,
                gridwidth = 2,
                weightx = 1.0,
                fill = GridBagConstraints.HORIZONTAL,
                insetsTop = 0
            )
        )

        // Row 1: Nama kota (readonly)
        panel.add(JBLabel("Kota"), gbc(0, 1, GridBagConstraints.EAST, insetsRight = 8))
        panel.add(cityNameField, gbc(1, 1, GridBagConstraints.WEST, weightx = 1.0, fill = GridBagConstraints.HORIZONTAL))

        // Row 2: Timezone
        panel.add(JBLabel("Timezone ID"), gbc(0, 2, GridBagConstraints.EAST, insetsRight = 8))
        panel.add(tzField, gbc(1, 2, GridBagConstraints.WEST, weightx = 1.0, fill = GridBagConstraints.HORIZONTAL))

        // Row 3: Offsets
        panel.add(JBLabel("Offsets (CSV)"), gbc(0, 3, GridBagConstraints.EAST, insetsRight = 8))
        panel.add(offsetsField, gbc(1, 3, GridBagConstraints.WEST, weightx = 1.0, fill = GridBagConstraints.HORIZONTAL))

        // Row 4: Notifications
        panel.add(notifCheck, gbc(0, 4, GridBagConstraints.WEST, gridwidth = 2, insetsTop = 8))

        return panel
    }

    private fun buildActions(): JPanel {
        val wrap = JPanel(BorderLayout())
        val row = JPanel().apply { border = JBUI.Borders.emptyTop(8) }

        val btnOpenSettings = JButton("Open Settings…").apply {
            addActionListener {
                com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                    .showSettingsDialog(null, "Pengingat Sholat")
            }
        }

        val btnRefresh = JButton("Refresh now").apply {
            addActionListener {
                PrayerSchedulerService.instance().reschedule()
                TestNotifier.notifyInfo("Jadwal di-refresh", "Ambil ulang dari API (MyQuran).")
            }
        }

        val btnApply = JButton("Apply").apply {
            addActionListener { onApply() }
        }

        row.add(btnOpenSettings)
        row.add(btnRefresh)
        row.add(btnApply)
        wrap.add(row, BorderLayout.EAST)
        return wrap
    }

    private fun onApply() {
        val api = apiField.text.trim().ifEmpty { "https://api.myquran.com/v2" }
        val tz  = tzField.text.trim()
        val cityId = cityIdField.text.trim()
        val cityName = cityNameField.text.ifBlank { "Jakarta" }

        val offsets = offsetsField.text
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 0 }
            .sorted()
            .joinToString(",")
            .ifEmpty { "10,5" }

        state.apiBaseUrl = api
        state.timezoneId = tz
        state.cityId = cityId
        state.cityName = cityName
        state.preAdhanOffsetsCsv = offsets
        state.enableNotifications = notifCheck.isSelected

        PrayerSchedulerService.instance().reschedule()
        TestNotifier.notifyInfo("Settings tersimpan", "Jadwal & reminder diperbarui • $cityName")
    }

    // Helper GridBagConstraints
    private fun gbc(
        x: Int,
        y: Int,
        anchor: Int,
        gridwidth: Int = 1,
        weightx: Double = 0.0,
        fill: Int = GridBagConstraints.NONE,
        insetsTop: Int = 4,
        insetsRight: Int = 0
    ) = GridBagConstraints().apply {
        gridx = x; gridy = y
        this.anchor = anchor
        this.gridwidth = gridwidth
        this.weightx = weightx
        this.fill = fill
        insets = JBUI.insets(insetsTop, 0, 4, insetsRight)
    }
}
