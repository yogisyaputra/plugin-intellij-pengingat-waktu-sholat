package id.ysydev.sholatreminder.ui.components

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import id.ysydev.sholatreminder.api.CityHit
import id.ysydev.sholatreminder.api.MyQuranApiClient
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CityPickerDialog(project: Project?) : DialogWrapper(project, true) {
    private val model = DefaultListModel<CityHit>()
    private val list = JBList(model)
    private val search = SearchTextField()
    private val client = MyQuranApiClient()
    private var currentQuery: String = ""

    var selected: CityHit? = null
        private set

    init {
        title = "Pilih Kota (MyQuran)"
        init()

        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): java.awt.Component {
                val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val v = value as CityHit
                comp.text = "${v.lokasi}"
                return comp
            }
        }
        list.addListSelectionListener { selected = list.selectedValue }
        list.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) { selected = list.selectedValue; close(OK_EXIT_CODE) }
            }
        })

        search.textEditor.document.addDocumentListener(object : DocumentListener {
            private var lastSubmit: java.util.concurrent.Future<*>? = null
            private fun trigger() {
                val q = search.text.trim()
                currentQuery = q
                // debounce ~250ms
                lastSubmit?.cancel(true)
                lastSubmit = AppExecutorUtil.getAppScheduledExecutorService().schedule({
                    fetch(q)
                }, 250, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
            override fun insertUpdate(e: DocumentEvent) = trigger()
            override fun removeUpdate(e: DocumentEvent) = trigger()
            override fun changedUpdate(e: DocumentEvent) = trigger()
        })

        // initial: kosong -> ambil 10 kota untuk "padang" misalnya? Lebih baik kosong
        refresh(emptyList())
    }

    override fun createCenterPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(10, 12)
            preferredSize = Dimension(JBUI.scale(380), JBUI.scale(420))
        }
        panel.add(search, BorderLayout.NORTH)
        panel.add(JScrollPane(list).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        return panel
    }

    override fun doOKAction() {
        selected = list.selectedValue
        super.doOKAction()
    }

    private fun fetch(q: String) {
        if (q.isBlank()) { refresh(emptyList()); return }
        try {
            val hits = client.searchCities(q, limit = 10)
            // hanya update jika query masih sama (hindari race)
            if (q == currentQuery) refresh(hits)
        } catch (_: Throwable) {
            // abaikan error network; jangan crash dialog
        }
    }

    private fun refresh(items: List<CityHit>) {
        javax.swing.SwingUtilities.invokeLater {
            model.removeAllElements()
            items.forEach { model.addElement(it) }
            if (!model.isEmpty) list.selectedIndex = 0
        }
    }
}
