plugins {
    kotlin("multiplatform") version "2.4.10" apply false
    kotlin("plugin.serialization") version "2.4.10" apply false
    id("com.android.application") version "9.3.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.3.1" apply false
    // 1.10.3, not 1.11.x: 1.11 rewrote iOS text input to use a native UIView
    // via UITextInput/UIKeyInput, which pulls in UIViewLayoutRegion - a
    // UIKit class this CI runner's Xcode/SDK doesn't have. That's used
    // unconditionally by every text field, not gated behind an OS check, so
    // weak-linking around the resulting link error just traded it for a
    // crash on first render instead. 1.10.3 predates that rewrite entirely.
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
