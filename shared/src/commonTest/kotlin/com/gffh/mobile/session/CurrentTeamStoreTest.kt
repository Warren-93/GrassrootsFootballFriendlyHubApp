package com.gffh.mobile.session

import com.gffh.mobile.model.TeamView
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurrentTeamStoreTest {

    @Test
    fun theActiveTeamFromTeamsMineSaysWhetherTheyCanManageItsClub() {
        assertEquals(ActiveTeam("t1", "c1", "Team t1", canManage = true, canManageClub = true), team(canManageClub = true).toActiveTeam())
        assertEquals(false, team(canManageClub = false).toActiveTeam().canManageClub)
        // An older API says neither: managing the team is assumed, as before, and the club left unknown.
        val older = team(canManage = null, canManageClub = null).toActiveTeam()
        assertTrue(older.canManage)
        assertNull(older.canManageClub)
    }

    @Test
    fun canManageClubOfOnlyAnswersForTheActiveTeamsOwnClub() {
        val teamManager = ActiveTeam("t1", "c1", "Team t1", canManageClub = false)

        assertEquals(false, teamManager.canManageClubOf("c1"))
        assertEquals(true, teamManager.copy(canManageClub = true).canManageClubOf("c1"))
        assertNull(teamManager.canManageClubOf("another-club"))
        assertNull(teamManager.copy(canManageClub = null).canManageClubOf("c1"))
        assertNull((null as ActiveTeam?).canManageClubOf("c1"))
    }

    @Test
    fun aTeamManagersNoIsRememberedAcrossLaunches() {
        val settings = InMemorySettings()
        CurrentTeamStore(settings).set(ActiveTeam("t1", "c1", "Team t1", canManageClub = false))

        assertEquals(false, CurrentTeamStore(settings).active.value?.canManageClub)
    }

    @Test
    fun notKnowingIsRememberedAsNotKnowingRatherThanANo() {
        val settings = InMemorySettings()
        val store = CurrentTeamStore(settings)
        store.set(ActiveTeam("t1", "c1", "Team t1", canManageClub = true))

        // Switching to a team an older API said nothing about forgets the last team's yes.
        store.set(ActiveTeam("t2", "c2", "Team t2", canManageClub = null))

        assertNull(store.active.value?.canManageClub)
        assertNull(CurrentTeamStore(settings).active.value?.canManageClub)
    }

    @Test
    fun clearingForgetsTheClubToo() {
        val settings = InMemorySettings()
        val store = CurrentTeamStore(settings)
        store.set(ActiveTeam("t1", "c1", "Team t1", canManageClub = true))

        store.clear()

        assertNull(store.active.value)
        assertFalse(settings.keys.any { it.startsWith("active_") }, settings.keys.toString())
    }

    private fun team(canManage: Boolean? = true, canManageClub: Boolean?) = TeamView(
        id = "t1",
        clubId = "c1",
        name = "Team t1",
        clubName = "Club",
        ageGroup = "U11",
        gender = "MIXED",
        format = "SEVEN_A_SIDE",
        abilityLevel = "INTERMEDIATE",
        postcode = "AB1 2CD",
        longitude = -1.0,
        latitude = 52.0,
        travelRadiusMiles = 10,
        homeAwayPreference = "EITHER",
        verification = "VERIFIED",
        completenessPercent = 100,
        createdAt = "2026-10-01T10:00:00Z",
        canManage = canManage,
        canManageClub = canManageClub
    )
}

/** A Settings kept in a map, standing in for SharedPreferences and NSUserDefaults. */
private class InMemorySettings : Settings {
    private val values = mutableMapOf<String, Any>()

    override val keys: Set<String> get() = values.keys.toSet()
    override val size: Int get() = values.size
    override fun clear() = values.clear()
    override fun remove(key: String) {
        values.remove(key)
    }
    override fun hasKey(key: String): Boolean = key in values

    override fun putInt(key: String, value: Int) = put(key, value)
    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = values[key] as? Int

    override fun putLong(key: String, value: Long) = put(key, value)
    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) = put(key, value)
    override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue
    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putFloat(key: String, value: Float) = put(key, value)
    override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = values[key] as? Float

    override fun putDouble(key: String, value: Double) = put(key, value)
    override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean) = put(key, value)
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean

    private fun put(key: String, value: Any) {
        values[key] = value
    }
}
