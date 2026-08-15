package com.gffh.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.theme.displayHeadlineMedium
import com.gffh.mobile.ui.components.heroGradientBrush

/**
 * SCR-AU-02 Welcome. Purpose: state the proposition and offer the two entry
 * paths. Social sign-in (Apple/Google) is specified but has no backend
 * counterpart yet - the Technical Specification lists OAuth as a "recommended
 * technical decision" but only email/password is implemented server-side, so
 * those buttons are intentionally omitted rather than shown and non-functional.
 *
 * Full-bleed navy gradient treatment, matching gffh-web's WelcomePage /
 * SignInPage marketing feel (see [heroGradientBrush]).
 */
@Composable
fun WelcomeScreen(navigator: Navigator) {
    Box(
        modifier = Modifier.fillMaxSize().background(heroGradientBrush()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp).widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Find your next friendly.".uppercase(),
                    style = displayHeadlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    "We're free on this date. Find us a suitable friendly.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 40.dp)
                )

                Button(
                    onClick = { navigator.push(Route.Register) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create account") }

                OutlinedButton(
                    onClick = { navigator.push(Route.SignIn) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("Sign in") }

                Row(modifier = Modifier.padding(top = 32.dp)) {
                    TextButton(onClick = { /* open in-app browser: terms */ }) { Text("Terms") }
                    TextButton(onClick = { /* open in-app browser: privacy */ }) { Text("Privacy") }
                }
            }
        }
    }
}
