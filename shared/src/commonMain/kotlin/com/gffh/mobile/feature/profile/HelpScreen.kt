package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator

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
        "From your team profile, choose Officials. They need an existing account with the email you enter. Club " +
            "admins can manage every squad the club runs; team managers can only manage the one team."
    ),
    Faq(
        "How does matching work?",
        "Search ranks nearby teams by how well your published availability, format, ability level, age group and " +
            "travel distance line up with theirs. Tap a result to see the exact factors behind its score."
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
 * human.
 */
@Composable
fun HelpScreen(navigator: Navigator) {
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
        }

        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
