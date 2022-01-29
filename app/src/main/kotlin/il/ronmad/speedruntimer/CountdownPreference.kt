package il.ronmad.speedruntimer

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.SummaryProvider

class CountdownPreference : DialogPreference {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    var countdown: Long = 0L
        set(value) {
            if (callChangeListener(value)) {
                field = value
                persistLong(value)
                notifyChanged()
            }
        }

    init {
        onPreferenceChangeListener = OnPreferenceChangeListener { _: Preference, value: Any ->
            (value as Long) != countdown
        }
        summaryProvider = SummaryProvider { _: DialogPreference ->
            "Timer starts at ${(-countdown).getFormattedTime()}"
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int) = a.getInt(index, 0).toLong()

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        countdown = if (restorePersistedValue)
            getPersistedLong(countdown) else ((defaultValue as? Long) ?: 0)
    }
}
