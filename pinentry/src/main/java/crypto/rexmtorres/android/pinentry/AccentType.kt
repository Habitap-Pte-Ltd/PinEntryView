package crypto.rexmtorres.android.pinentry

import androidx.annotation.IntDef

@IntDef(
    flag = true,
    value = [PinEntryView.ACCENT_NONE, PinEntryView.ACCENT_ALL, PinEntryView.ACCENT_CHARACTER]
)
@Retention(AnnotationRetention.SOURCE)
annotation class AccentType()
