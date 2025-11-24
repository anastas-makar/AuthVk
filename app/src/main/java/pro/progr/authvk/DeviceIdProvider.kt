package pro.progr.authvk

import android.content.Context
import java.util.UUID

object DeviceIdProvider {
    @Volatile private var cached: String? = null
    private const val PREFS = "app_meta"
    private const val KEY = "device_id"

    fun init(context: Context): String = synchronized(this) {
        cached ?: run {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val id = p.getString(KEY, null) ?: UUID.randomUUID().toString().also {
                p.edit().putString(KEY, it).apply()
            }
            cached = id
            id
        }
    }

    fun set(id: String, context: Context) { // чтобы записать из Room onCreate
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putString(KEY, id).apply()
        cached = id
    }

    fun get(): String =
        cached ?: error("DeviceIdProvider not initialized — call init(context) earlier")
}
