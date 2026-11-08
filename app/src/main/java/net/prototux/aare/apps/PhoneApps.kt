package net.prototux.aare.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.Normalizer

/** An app of the phone's own launcher, which the car can run on a display of its own (see [AppDisplay]). */
data class LaunchableApp(val packageName: String, val activity: String, val label: String, val icon: Bitmap? = null) {
    val component: ComponentName get() = ComponentName(packageName, activity)
}

/**
 * The phone's launchable apps (ACTION_MAIN + CATEGORY_LAUNCHER), for the car's "Phone apps" grid.
 * The ones the car is mostly for (Yandex navigation and music) come first when installed.
 */
object PhoneApps {
    /** Pinned first, in this order. */
    val PINNED = listOf("ru.yandex.yandexnavi", "ru.yandex.yandexmaps", "ru.yandex.music")

    /** Every launcher activity but AARE's own, arranged by [arrange]. Package manager work: not on the main thread. */
    fun query(context: Context, iconSizePx: Int = 112): List<LaunchableApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val found = runCatching { pm.queryIntentActivities(launcher, 0) }.getOrDefault(emptyList())
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != context.packageName && it.enabled && it.exported }
            .map { info ->
                LaunchableApp(
                    info.packageName, info.name,
                    runCatching { info.loadLabel(pm).toString() }.getOrDefault(info.packageName),
                    runCatching { info.loadIcon(pm).toBitmap(iconSizePx, iconSizePx) }.getOrNull(),
                )
            }
        return arrange(found)
    }

    /** One entry per activity, [pinned] packages first (in that order), then the rest by label. */
    fun arrange(apps: List<LaunchableApp>, pinned: List<String> = PINNED): List<LaunchableApp> {
        val rank = pinned.withIndex().associate { (i, p) -> p to i }
        return apps.distinctBy { it.packageName to it.activity }
            .sortedWith(compareBy<LaunchableApp>({ rank[it.packageName] ?: Int.MAX_VALUE }, { fold(it.label) }, { it.packageName }))
    }

    /**
     * The apps matching [query]: every word of it starts a word of the label, or is in the package
     * name. Case and accents are ignored ("yan mus" finds "Yandex Music", "e" finds "É…").
     */
    fun <T> filter(apps: List<T>, query: String, label: (T) -> String, packageName: (T) -> String): List<T> {
        val words = fold(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return apps
        return apps.filter { app ->
            val labelWords = fold(label(app)).split(' ', '-', '.', '_').filter { it.isNotEmpty() }
            val pkg = packageName(app).lowercase()
            words.all { w -> labelWords.any { it.startsWith(w) } || w in pkg }
        }
    }

    /**
     * After Back was sent to [app] on its display, whether the app went away: the display is empty
     * ([now] ""), or something other than the app and what was in front before is there (the app's
     * task went behind another phone app). [before] and [now] are the packages in front, "" for none.
     */
    fun backedOut(app: String, before: String?, now: String): Boolean =
        now.isEmpty() || (now != app && now != before)

    /**
     * Apps the car may show while driving: navigation and music, as the phone screen mirror
     * allows. [extra] adds the packages AARE knows as navigation or media apps.
     */
    fun drivingAllowed(packageName: String, extra: Set<String> = emptySet()): Boolean =
        packageName in PINNED || packageName in KNOWN_NAVIGATION || packageName in extra

    /** Navigation apps without a car UI of their own: the phone screen mirror's list, plus open-source and Russian ones. */
    val KNOWN_NAVIGATION = setOf(
        "com.google.android.apps.maps",
        "com.waze",
        "com.here.app.maps",
        "com.sygic.aura",
        "com.tomtom.gplay.navapp",
        "net.osmand",
        "net.osmand.plus",
        "app.organicmaps",
        "app.comaps",
        "ru.dublgis.dgismobile",
    )

    private fun fold(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(COMBINING, "")

    private val COMBINING = Regex("\\p{Mn}+")
}
