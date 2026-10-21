package br.com.semaforo

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var dump: TextView
    private lateinit var group: RadioGroup
    private val fields = mutableMapOf<String, EditText>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        fun label(t: String, size: Float = 16f) = TextView(this).apply {
            text = t; textSize = size; setPadding(0, pad / 2, 0, 0)
        }
        fun field(key: String, title: String, v: Float) {
            root.addView(label(title))
            val e = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(v.toString().replace('.', ','))
            }
            fields[key] = e; root.addView(e)
        }

        root.addView(label("Semáforo Corridas", 24f))
        root.addView(Button(this).apply {
            text = "1) Ativar serviço de acessibilidade"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        root.addView(label("Calcular semáforo por:"))
        group = RadioGroup(this)
        listOf("R$/km", "R$/hora", "Os dois (vale o pior)").forEachIndexed { i, t ->
            group.addView(RadioButton(this).apply { text = t; id = 100 + i })
        }
        group.check(100 + prefs.mode)
        root.addView(group)

        field("gk", "Verde a partir de (R$/km)", prefs.greenKm)
        field("yk", "Amarelo a partir de (R$/km)", prefs.yellowKm)
        field("gh", "Verde a partir de (R$/hora)", prefs.greenHr)
        field("yh", "Amarelo a partir de (R$/hora)", prefs.yellowHr)

        root.addView(Button(this).apply {
            text = "2) Salvar configuração"
            setOnClickListener { save() }
        })

        root.addView(label("Última tela de corrida lida (para ajustes):"))
        dump = TextView(this).apply { textSize = 12f; setTextIsSelectable(true) }
        root.addView(dump)
        root.addView(Button(this).apply {
            text = "Copiar texto"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("dump", prefs.lastDump))
                Toast.makeText(this@MainActivity, "Copiado", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        dump.text = prefs.lastDump.ifEmpty { "(nenhuma corrida lida ainda)" }
    }

    private fun save() {
        fun f(k: String, d: Float) = fields[k]!!.text.toString().replace(',', '.').toFloatOrNull() ?: d
        prefs.mode = group.checkedRadioButtonId - 100
        prefs.greenKm = f("gk", prefs.greenKm)
        prefs.yellowKm = f("yk", prefs.yellowKm)
        prefs.greenHr = f("gh", prefs.greenHr)
        prefs.yellowHr = f("yh", prefs.yellowHr)
        Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
    }
}
