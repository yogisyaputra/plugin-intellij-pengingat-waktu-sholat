package id.ysydev.sholatreminder.core

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

enum class CountdownMode { ADHAN, REMINDER }

data class PrayerSettings(
    var cityId: String = "",            // NEW: id kota MyQuran (mis. "0314")
    var cityName: String = "",   // NEW: nama kota (lokasi)
    var apiBaseUrl: String = "https://api.myquran.com/v2",
    var timezoneId: String = "",

    var preAdhanOffsetsCsv: String = "10,5",
    var enableNotifications: Boolean = true,
    var countdownMode: CountdownMode = CountdownMode.ADHAN
)

@State(name = "SholatReminderSettings", storages = [Storage("SholatReminder.xml")])
class PrayerSettingsState : PersistentStateComponent<PrayerSettings> {
    private var state = PrayerSettings()
    override fun getState(): PrayerSettings = state
    override fun loadState(s: PrayerSettings) { state = s }
    companion object { fun getInstance() = service<PrayerSettingsState>() }
}
