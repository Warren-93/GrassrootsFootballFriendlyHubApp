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
        assertEquals("Intermediate", codeLabel("INTERMEDIATE"))
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
}
