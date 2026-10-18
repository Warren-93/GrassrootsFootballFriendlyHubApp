package com.gffh.mobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrganisationTest {
    @Test
    fun youthTeamsGetYouthSchoolsAndGirlsBodies() {
        assertEquals(
            listOf(Organisation.SYFA, Organisation.SWF, Organisation.SSFA, Organisation.OTHER),
            Organisation.availableFor(AgeGroup.U12)
        )
    }

    @Test
    fun adultTeamsCannotPickSchoolsFootball() {
        assertFalse(Organisation.SSFA in Organisation.availableFor(AgeGroup.ADULT))
        assertTrue(Organisation.SWFA in Organisation.availableFor(AgeGroup.VETERANS))
    }

    @Test
    fun unknownAgeGroupOffersEverything() {
        assertEquals(Organisation.entries.toList(), Organisation.availableFor(null))
    }

    @Test
    fun womensFootballIsHiddenFromBoysAndMen() {
        assertEquals(
            listOf(Organisation.SYFA, Organisation.SSFA, Organisation.OTHER),
            Organisation.availableFor(AgeGroup.U12, Gender.BOYS)
        )
        assertFalse(Organisation.SWF in Organisation.availableFor(AgeGroup.ADULT, Gender.MEN))
        assertTrue(Organisation.SWF in Organisation.availableFor(AgeGroup.ADULT, Gender.WOMEN))
        assertTrue(Organisation.SWF in Organisation.availableFor(AgeGroup.U14, Gender.MIXED))
    }
}
