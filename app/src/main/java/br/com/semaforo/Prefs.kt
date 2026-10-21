package br.com.semaforo

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    // 0 = R$/km, 1 = R$/hora, 2 = os dois (vale o pior)
    var mode: Int
        get() = sp.getInt("mode", 2)
        set(v) = sp.edit().putInt("mode", v).apply()

    var greenKm: Float
        get() = sp.getFloat("gk", 2.0f)
        set(v) = sp.edit().putFloat("gk", v).apply()
    var yellowKm: Float
        get() = sp.getFloat("yk", 1.5f)
        set(v) = sp.edit().putFloat("yk", v).apply()
    var greenHr: Float
        get() = sp.getFloat("gh", 40f)
        set(v) = sp.edit().putFloat("gh", v).apply()
    var yellowHr: Float
        get() = sp.getFloat("yh", 30f)
        set(v) = sp.edit().putFloat("yh", v).apply()

    var lastDump: String
        get() = sp.getString("dump", "") ?: ""
        set(v) = sp.edit().putString("dump", v).apply()
}
