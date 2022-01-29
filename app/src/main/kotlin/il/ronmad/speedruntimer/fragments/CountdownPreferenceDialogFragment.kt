package il.ronmad.speedruntimer.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat
import il.ronmad.speedruntimer.CountdownPreference
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.databinding.EditTimeLayoutBinding
import il.ronmad.speedruntimer.getTimeFromEditTexts
import il.ronmad.speedruntimer.setEditTextsFromTime

class CountdownPreferenceDialogFragment : PreferenceDialogFragmentCompat() {

    private var _editTimeViewBinding: EditTimeLayoutBinding? = null
    private val editTimeViewBinding get() = _editTimeViewBinding!!

    private val countdownPreference get() = preference as CountdownPreference

    override fun onCreateDialogView(context: Context): View {
        _editTimeViewBinding = EditTimeLayoutBinding.inflate(layoutInflater)
        return editTimeViewBinding.root
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        editTimeViewBinding.setEditTextsFromTime(countdownPreference.countdown)
        editTimeViewBinding.clearTimeButton.setOnClickListener {
            editTimeViewBinding.setEditTextsFromTime(0L)
        }
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        builder.setPositiveButton(R.string.save, this)
            .setNegativeButton(android.R.string.cancel, this)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            countdownPreference.countdown = editTimeViewBinding.getTimeFromEditTexts()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _editTimeViewBinding = null
    }

    companion object {
        fun newInstance(key: String) = CountdownPreferenceDialogFragment().apply {
            arguments = Bundle().also { it.putString(ARG_KEY, key) }
        }
    }
}