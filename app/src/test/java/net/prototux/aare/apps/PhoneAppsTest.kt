package net.prototux.aare.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAppsTest {
    private fun app(pkg: String, label: String, activity: String = "$pkg.Main") = LaunchableApp(pkg, activity, label)

    private fun labels(apps: List<LaunchableApp>) = apps.map { it.label }

    @Test
    fun pinnedFirstInTheirOrderThenByLabel() {
        val apps = listOf(
            app("org.zoo", "Zoo"),
            app("ru.yandex.music", "Yandex Music"),
            app("com.alpha", "alpha"),
            app("ru.yandex.yandexmaps", "Yandex Maps"),
            app("com.beta", "Beta"),
        )
        assertEquals(listOf("Yandex Maps", "Yandex Music", "alpha", "Beta", "Zoo"), labels(PhoneApps.arrange(apps)))
    }

    @Test
    fun navigatorBeforeMapsBeforeMusic() {
        val apps = listOf(app("ru.yandex.music", "Music"), app("ru.yandex.yandexmaps", "Maps"), app("ru.yandex.yandexnavi", "Navigator"))
        assertEquals(listOf("Navigator", "Maps", "Music"), labels(PhoneApps.arrange(apps)))
    }

    @Test
    fun accentsDontChangeTheOrder() {
        val apps = listOf(app("b", "Eta"), app("a", "Échecs"), app("c", "Dé"))
        assertEquals(listOf("Dé", "Échecs", "Eta"), labels(PhoneApps.arrange(apps)))
    }

    @Test
    fun oneEntryPerActivityButSeveralPerPackage() {
        val apps = listOf(app("p", "One", "p.A"), app("p", "One", "p.A"), app("p", "Two", "p.B"))
        assertEquals(listOf("One", "Two"), labels(PhoneApps.arrange(apps)))
    }

    private fun search(apps: List<LaunchableApp>, q: String) = labels(PhoneApps.filter(apps, q, { it.label }, { it.packageName }))

    @Test
    fun searchMatchesWordStartsAndPackage() {
        val apps = listOf(app("ru.yandex.music", "Yandex Music"), app("ru.yandex.yandexmaps", "Yandex Maps"), app("com.spotify.music", "Spotify"))
        assertEquals(listOf("Yandex Music", "Yandex Maps", "Spotify"), search(apps, ""))
        assertEquals(listOf("Yandex Music", "Yandex Maps"), search(apps, "yan"))
        assertEquals(listOf("Yandex Music"), search(apps, "yan mus"))
        // "music" is in Spotify's package name
        assertEquals(listOf("Yandex Music", "Spotify"), search(apps, "MUSIC"))
        // every word must match: "yandex" does, "zzz" doesn't
        assertEquals(emptyList<String>(), search(apps, "yandex zzz"))
    }

    @Test
    fun searchIgnoresAccentsAndCase() {
        val apps = listOf(app("a", "Météo"), app("b", "Карты"))
        assertEquals(listOf("Météo"), search(apps, "METEO"))
        assertEquals(listOf("Карты"), search(apps, "кар"))
    }

    @Test
    fun drivingAllowsNavigationAndMusicOnly() {
        assertTrue(PhoneApps.drivingAllowed("ru.yandex.yandexmaps"))
        assertTrue(PhoneApps.drivingAllowed("com.waze"))
        assertTrue(PhoneApps.drivingAllowed("com.example.player", extra = setOf("com.example.player")))
        assertFalse(PhoneApps.drivingAllowed("com.google.android.youtube"))
    }
}
