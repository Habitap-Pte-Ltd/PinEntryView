package crypto.rexmtorres.android.pinentry

import androidx.annotation.IntDef

/**
 * Specifies how the accent indicator is drawn.
 */
@IntDef(
    flag = true,
    value = [PinEntryView.ACCENT_NONE, PinEntryView.ACCENT_ALL, PinEntryView.ACCENT_CHARACTER]
)
@Retention(AnnotationRetention.SOURCE)
annotation class AccentType()
