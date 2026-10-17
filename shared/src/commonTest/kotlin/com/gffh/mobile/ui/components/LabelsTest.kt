package com.gffh.mobile.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelsTest {

    @Test
    fun teamCodesReadAsPeopleSayThem() {
        assertEquals("U12", codeLabel("U12"))
        assertEquals("Veterans", codeLabel("VETERANS"))
        assertEquals("Boys", codeLabel("BOYS"))
        assertEquals("7v7", codeLabel("SEVEN_A_SIDE"))
        assertEquals("11v11", codeLabel("ELEVEN_A_SIDE"))
        assertEquals("Division 2", codeLabel("DIVISION_2"))
        assertEquals("No preference", codeLabel("EITHER"))
        assertEquals("Not verified", codeLabel("NOT_STARTED"))
        assertEquals("Changes requested", codeLabel("CHANGES_REQUESTED"))
    }

    @Test
    fun anUnknownCodeIsTidiedRatherThanShownRaw() {
        assertEquals("Some new status", codeLabel("SOME_NEW_STATUS"))
    }

    @Test
    fun noneSaysWhatThereIsNoneOf() {
        assertEquals("No referee", refereeLabel("NONE"))
        assertEquals("Club supplied", refereeLabel("CLUB_SUPPLIED"))
        assertEquals("No costs", costShareLabel("NONE"))
        assertEquals("Host pays", costShareLabel("HOST_PAYS"))
        assertEquals("Visitor pays", costShareLabel("VISITOR_PAYS"))
        assertEquals("Split", costShareLabel("SPLIT"))
    }

    @Test
    fun aDivisionReadsTheSameFromItsCodeOrItsLabel() {
        assertEquals("Division 1 / Championship", divisionLabel("DIVISION_1"))
        // Match results already send the label - it mustn't be re-cased.
        assertEquals("Division 1 / Championship", divisionLabel("Division 1 / Championship"))
        assertEquals("Some new division", divisionLabel("SOME_NEW_DIVISION"))
    }

    @Test
    fun anOrganisationReadsTheSameFromItsCodeOrItsLabel() {
        assertEquals("Scottish Youth FA (SYFA)", organisationLabel("SYFA"))
        assertEquals("Scottish Women's Football (SWF)", organisationLabel("Scottish Women's Football (SWF)"))
        assertEquals("Other / not affiliated", organisationLabel("OTHER"))
    }
}
