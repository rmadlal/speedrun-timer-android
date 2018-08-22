package il.ronmad.speedruntimer

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.os.Bundle
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View

class CountdownPreference : DialogPreference {

    @TargetApi(21)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    @TargetApi(21)
    constructor(context: Context?) : super(context)

    private var countdown: Long = 0L
    private lateinit var view: View

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        view = View.inflate(context, R.layout.edit_time_layout, null)
        view.setEditTextsFromTime(countdown)
        builder.setView(view)
                .setPositiveButton(R.string.save) { _, _ ->
                    countdown = view.getTimeFromEditTexts()
                    persistLong(countdown)
                    summary = "Timer starts at ${(-countdown).getFormattedTime()}"
                }
                .setNegativeButton(R.string.pb_clear, this)
                .setNeutralButton(android.R.string.cancel, this)
    }

    override fun showDialog(state: Bundle?) {
        super.showDialog(state)
        (dialog as AlertDialog).getButton(DialogInterface.BUTTON_NEGATIVE)
                .setOnClickListener { view.setEditTextsFromTime(0L) }
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int) = a?.getInt(index, 0)?.toLong()

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        countdown = if (restorePersistedValue)
            getPersistedLong(countdown) else defaultValue as Long
    }
}