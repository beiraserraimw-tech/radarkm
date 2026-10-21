package br.com.semaforo

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class RideAccessibilityService : AccessibilityService() {

    // Confira os pacotes no seu aparelho se algum app não for detectado.
    private val packages = setOf("com.ubercab.driver", "com.app99.driver")

    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var box: LinearLayout? = null
    private var dots: List<View> = emptyList()
    private var label: TextView? = null

    private val hideRunnable = Runnable { hide() }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Prefs(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg !in packages) return

        val texts = mutableListOf<String>()
        collect(root, texts)
        if (texts.any { it.contains("R$") }) {
            val dump = texts.joinToString("\n").take(2000)
            if (dump != prefs.lastDump) prefs.lastDump = dump
        }
        val offer = RideParser.parse(texts) ?: return
        show(offer)
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 4000)
    }

    override fun onInterrupt() { hide() }

    private fun collect(n: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (n == null) return
        n.text?.let { out.add(it.toString()) }
        n.contentDescription?.let { out.add(it.toString()) }
        for (i in 0 until n.childCount) collect(n.getChild(i), out)
    }

    private fun level(v: Double, green: Float, yellow: Float): Int =
        if (v >= green) 2 else if (v >= yellow) 1 else 0

    private fun show(o: Offer) {
        val perKm = o.price / o.km
        val perHr = o.price / (o.min / 60.0)
        val lk = level(perKm, prefs.greenKm, prefs.yellowKm)
        val lh = level(perHr, prefs.greenHr, prefs.yellowHr)
        val lv = when (prefs.mode) { 0 -> lk; 1 -> lh; else -> minOf(lk, lh) }

        if (box == null) build()
        // dots: 0 = vermelho, 1 = amarelo, 2 = verde
        dots.forEachIndexed { i, d -> d.alpha = if (i == lv) 1f else 0.15f }
        label?.text = String.format(Locale("pt", "BR"), "R$ %.2f/km\nR$ %.0f/h", perKm, perHr)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun build() {
        val colors = listOf(0xFFE53935.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt())
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val ds = colors.map { c ->
            View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c) }
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(4), 0, dp(4), 0) }
            }
        }
        ds.forEach { row.addView(it) }
        val tv = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.CENTER
        }
        val b = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(0xDD000000.toInt()) }
            addView(row); addView(tv)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(80) }
        wm.addView(b, lp)
        box = b; dots = ds; label = tv
    }

    private fun hide() {
        box?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        box = null; dots = emptyList(); label = null
    }
}
