package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.platform.composeEmail
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.ConfigRepository

private data class Faq(val question: String, val answer: String)

private val FAQS = listOf(
    Faq(
        "How do I get my team verified?",
        "Open your team profile and choose Verification. Submit your league/FA affiliation number (if you have " +
            "one), a way for an admin to reach you, and a couple of evidence links. An admin reviews it and " +
            "you'll see the outcome on the same screen."
    ),
    Faq(
        "How do I add another manager or club admin?",
        "From your team profile, choose Members and enter their email. If they don't have an account yet, we " +
            "email them an invitation and they join the team once they've signed up and verified that address. " +
            "Club admins can manage every squad the club runs; team managers can only manage the one team."
    ),
    Faq(
        "How does matching work?",
        "Discover shows every team with the same age group and gender as yours, at every ability level. They are " +
            "ranked by how well your published availability, format, ability level and travel distance line up " +
            "with theirs. Use Filters on the results to narrow by ability level, format, distance or verified " +
            "teams. Tap a result to see the exact factors behind its score."
    ),
    Faq(
        "What happens after I send a friendly request?",
        "The other team can confirm it as-is, suggest changes to the date, time or venue, or decline it. You'll " +
            "see the current status and any message from them on the request itself."
    ),
    Faq(
        "How do I report a concern or block a team?",
        "From that team's profile, choose Report. Safeguarding concerns are flagged for immediate review. You " +
            "can also block a team from the same screen, which cancels any open requests between you."
    ),
    Faq(
        "Can I see or delete the data held on my account?",
        "Yes - Settings > Privacy and data shows a summary of what's held on you and lets you delete your account."
    ),
)

/**
 * SCR-PR-12 Help and support. Purpose: self-serve answers, plus a clear
 * pointer to the real escalation route (Report) for anything that needs a
 * human. "Email support" uses the API's configured support address and is
 * left out entirely when there isn't one.
 */
@Composable
fun HelpScreen(configRepository: ConfigRepository, navigator: Navigator) {
    val config by configRepository.config.collectAsState()
    var mailFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { configRepository.get() }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Help and support", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FAQS.forEach { faq ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(faq.question, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(faq.answer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    "If there's an immediate risk to a child, contact the police and your safeguarding officer " +
                        "directly - not only through the app. For anything else, open the team's profile and " +
                        "choose Report.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            config?.supportEmail?.takeIf { it.isNotBlank() }?.let { supportEmail ->
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Still stuck?", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Email the Kickabout support team at $supportEmail.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { mailFailed = !composeEmail(supportEmail, "Kickabout support") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Email support")
                        }
                        if (mailFailed) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "There's no email app on this device - write to $supportEmail from any email account.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
