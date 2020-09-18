package il.ronmad.speedruntimer.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import il.ronmad.speedruntimer.CountdownPreference
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.getTimeFromEditTexts
import il.ronmad.speedruntimer.setEditTextsFromTime
import kotlinx.android.synthetic.main.edit_time_layout.view.*

class CountdownPreferenceDialogFragment : PreferenceDialogFragmentCompat() {

    private lateinit var editTimeView: View
    private lateinit var countdownPreference: CountdownPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        countdownPreference = (preference as CountdownPreference)
    }

    override fun onCreateDialogView(context: Context?): View {
        return View.inflate(context, R.layout.edit_time_layout, null).also {
            editTimeView = it
        }
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        view?.run {
            setEditTextsFromTime(countdownPreference.countdown)
            clearTimeButton.setOnClickListener {
                setEditTextsFromTime(0L)
            }
        }
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        builder.setPositiveButton(R.string.save, this)
                .setNegativeButton(android.R.string.cancel, this)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            countdownPreference.countdown = editTimeView.getTimeFromEditTexts()
        }
    }

    companion object {
        fun newInstance(key: String) = CountdownPreferenceDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_KEY, key) }
        }
    }
}