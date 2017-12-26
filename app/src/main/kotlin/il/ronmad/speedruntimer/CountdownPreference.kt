package il.ronmad.speedruntimer

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.os.Bundle
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.edit_time_layout.view.*

class CountdownPreference : DialogPreference {

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    private var countdown: Long = 0L
    private lateinit var view: View

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        view = View.inflate(context, R.layout.edit_time_layout, null)
        countdown.setEditTextsFromTime(view.hours, view.minutes, view.seconds, view.milliseconds)
        builder.setView(view)
                .setPositiveButton(R.string.save) { _, _ ->
                    countdown = Util.getTimeFromEditTexts(view.hours, view.minutes, view.seconds, view.milliseconds)
                    persistLong(countdown)
                    summary = "Timer starts at ${(-1 * countdown).getFormattedTime()}"
                }
                .setNegativeButton(R.string.pb_clear, this)
                .setNeutralButton(android.R.string.cancel, this)
    }

    override fun showDialog(state: Bundle?) {
        super.showDialog(state)
        (dialog as AlertDialog).getButton(DialogInterface.BUTTON_NEGATIVE)
                .setOnClickListener {
                    view.hours.setText("")
                    view.minutes.setText("")
                    view.seconds.setText("")
                    view.milliseconds.setText("")
                }
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int) = a?.getInt(index, 0)?.toLong()

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        countdown = if (restorePersistedValue)
            getPersistedLong(countdown) else defaultValue as Long
    }
}