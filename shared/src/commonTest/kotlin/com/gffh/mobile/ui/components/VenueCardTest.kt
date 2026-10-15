package com.gffh.mobile.ui.components

import com.gffh.mobile.model.VenueSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VenueCardTest {

    private val base = "https://www.google.com/maps/dir/?api=1&destination="

    @Test
    fun directionsUseExactCoordinatesWhenKnown() {
        val venue = VenueSummary(id = "v1", name = "Rec Ground", postcode = "SW1A 1AA", latitude = 51.5074, longitude = -0.1278)

        assertEquals("${base}51.5074,-0.1278", directionsUrl(venue))
    }

    @Test
    fun coordinatesNearTheMeridianStayPlainDecimals() {
        val venue = VenueSummary(id = "v1", name = "Greenwich Park", latitude = 51.4769, longitude = -0.0005)

        assertEquals("${base}51.4769,-0.0005", directionsUrl(venue))
    }

    @Test
    fun wholeNumberCoordinatesDropTheDecimalPoint() {
        val venue = VenueSummary(id = "v1", name = "Somewhere", latitude = 53.0, longitude = 2.0)

        assertEquals("${base}53,2", directionsUrl(venue))
    }

    @Test
    fun directionsFallBackToTheEncodedPostcode() {
        val venue = VenueSummary(id = "v1", name = "Rec Ground", address = "1 Park Road", postcode = "SW1A 1AA")

        assertEquals("${base}SW1A%201AA", directionsUrl(venue))
    }

    @Test
    fun directionsFallBackToTheAddressWithoutAPostcode() {
        val venue = VenueSummary(id = "v1", name = "Rec Ground", address = "1 Park Road")

        assertEquals("${base}1%20Park%20Road", directionsUrl(venue))
    }

    @Test
    fun noDirectionsWithNothingToNavigateTo() {
        assertNull(directionsUrl(VenueSummary(id = "v1", name = "Rec Ground", postcode = "  ")))
    }

    @Test
    fun addressLineJoinsWhicheverPartsAreKnown() {
        assertEquals("1 Park Road, SW1A 1AA", venueAddressLine(VenueSummary("v1", "Rec", address = "1 Park Road", postcode = "SW1A 1AA")))
        assertEquals("SW1A 1AA", venueAddressLine(VenueSummary("v1", "Rec", postcode = "SW1A 1AA")))
        assertNull(venueAddressLine(VenueSummary("v1", "Rec")))
    }

    @Test
    fun pitchSurfacesReadAsPeopleSayThem() {
        assertEquals("3G", pitchSurfaceLabel("THREE_G"))
        assertEquals("Grass", pitchSurfaceLabel("GRASS"))
        assertEquals("CLAY", pitchSurfaceLabel("CLAY"))
    }
}
