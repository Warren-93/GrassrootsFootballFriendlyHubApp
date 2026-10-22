package com.gffh.mobile.feature.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The emoji menu's tabs: a tab label (itself an emoji) and its emojis. */
internal val EMOJI_CATEGORIES: List<Pair<String, List<String>>> = listOf(
    "😀" to emojis(
        "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥸 " +
            "🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 " +
            "🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 💩"
    ),
    "👍" to emojis(
        "👍 👎 👌 🤌 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✋ 🤚 🖐️ 🖖 👋 🤝 👏 🙌 👐 🤲 🙏 ✍️ 💪 🦵 🦶 👀 " +
            "🧠 🫡 🤷 🤦 🙋 🙆 🙅 💁 🏃 🚶 🧍 🧎 🕺 💃 👯 🧑‍🤝‍🧑 👨‍👩‍👧‍👦 👶 🧒 👦 👧 🧑 👨 👩 🧓 👴 👵"
    ),
    "⚽" to emojis(
        "⚽ 🥅 🏆 🥇 🥈 🥉 🏅 🎖️ 👟 🧤 🦺 🎽 🏟️ 📣 🟨 🟥 ⏱️ ⏰ 📅 🗓️ 📍 🗺️ 🚗 🚌 🚐 🅿️ 🌧️ ☀️ ⛅ 🌦️ " +
            "❄️ 🌬️ ☔ 🌈 🏃‍♂️ 🏃‍♀️ 🤸 🧊 🩹 🚑 🥤 💧 🍊 🍌 🍕 🍔 🍟 🌭 🥪 🍰 ☕ 🍺 🥂 🧃"
    ),
    "🎉" to emojis(
        "🎉 🎊 🥳 🎈 🎂 🎁 ✨ 🔥 💯 ⭐ 🌟 💫 ⚡ 💥 💢 💤 💬 💭 🗯️ ❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ " +
            "💕 💞 💓 💗 💖 💘 💝 ✅ ☑️ ✔️ ❌ ❎ ❓ ❗ ‼️ ⁉️ ⚠️ 🚫 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🆗 🆒 🆕 🆙"
    ),
    "🏴" to emojis(
        "🏴󠁧󠁢󠁳󠁣󠁴󠁿 🏴󠁧󠁢󠁥󠁮󠁧󠁿 🏴󠁧󠁢󠁷󠁬󠁳󠁿 🇬🇧 🇮🇪 🇪🇺 🇫🇷 🇩🇪 🇪🇸 🇮🇹 🇳🇱 🇧🇪 🇵🇹 🇵🇱 🇺🇸 🇧🇷 🇦🇷 🏳️ 🏴 🏁 🚩 🏳️‍🌈"
    )
)

private fun emojis(spaced: String): List<String> = spaced.split(" ").filter { it.isNotBlank() }

/**
 * The emoji menu under the chat composer: a few tabs of common emojis
 * (faces, hands, football and match-day, celebrations, flags). Tapping one
 * puts it in the message at the cursor; the panel stays open so several can
 * be picked in a row. Mirrors the web's emoji button, though the web's has
 * the full set with search.
 */
@Composable
fun EmojiPanel(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(0) }
    Column(modifier.fillMaxWidth().height(280.dp)) {
        TabRow(selectedTabIndex = selected) {
            EMOJI_CATEGORIES.forEachIndexed { index, (label, _) ->
                Tab(
                    selected = selected == index,
                    onClick = { selected = index },
                    text = { Text(label, fontSize = 20.sp) }
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(EMOJI_CATEGORIES[selected].second) { emoji ->
                Box(
                    Modifier.size(44.dp).clickable { onPick(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 26.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
