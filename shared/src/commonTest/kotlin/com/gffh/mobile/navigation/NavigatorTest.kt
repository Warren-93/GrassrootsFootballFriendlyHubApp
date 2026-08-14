package com.gffh.mobile.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {

    @Test
    fun startsAtTheGivenRouteWithNothingToPopBackTo() {
        val navigator = Navigator(Route.Welcome)

        assertEquals(Route.Welcome, navigator.current)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun pushAddsToTheTopOfTheStackAndAllowsGoingBack() {
        val navigator = Navigator(Route.Welcome)

        navigator.push(Route.SignIn)

        assertEquals(Route.SignIn, navigator.current)
        assertTrue(navigator.canGoBack)
    }

    @Test
    fun popReturnsToThePreviousRoute() {
        val navigator = Navigator(Route.Welcome)
        navigator.push(Route.SignIn)
        navigator.push(Route.ForgotPassword())

        val popped = navigator.pop()

        assertTrue(popped)
        assertEquals(Route.SignIn, navigator.current)
    }

    @Test
    fun popOnAStackOfOneDoesNothingAndReportsFalse() {
        val navigator = Navigator(Route.Welcome)

        val popped = navigator.pop()

        assertFalse(popped)
        assertEquals(Route.Welcome, navigator.current)
    }

    @Test
    fun replaceSwapsTheTopOfTheStackWithoutGrowingIt() {
        val navigator = Navigator(Route.Welcome)
        navigator.push(Route.SearchEntry)

        navigator.replace(Route.Results)

        assertEquals(Route.Results, navigator.current)
        navigator.pop()
        assertEquals(Route.Welcome, navigator.current)
    }

    @Test
    fun resetToClearsTheWholeStackSoBackDoesNotReturnToWhatCameBefore() {
        val navigator = Navigator(Route.Welcome)
        navigator.push(Route.SignIn)
        navigator.push(Route.Home)

        navigator.resetTo(Route.Welcome)

        assertEquals(Route.Welcome, navigator.current)
        assertFalse(navigator.canGoBack)
    }
}
