package il.ronmad.speedruntimer.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.databinding.GameInfoItemBinding
import il.ronmad.speedruntimer.databinding.GameInfoItemHeaderBinding
import il.ronmad.speedruntimer.getColorCpt
import il.ronmad.speedruntimer.getFormattedTime
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getCategoryByName
import il.ronmad.speedruntimer.toOrdinal
import il.ronmad.speedruntimer.web.SrcLeaderboard

class InfoListAdapter(
    val context: Context?,
    val game: Game,
    private var initExpandedGroups: List<Int>
) : BaseExpandableListAdapter() {

    var data: List<SrcLeaderboard> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getGroup(groupPosition: Int) = data[groupPosition]

    override fun isChildSelectable(groupPosition: Int, childPosition: Int) = false

    override fun hasStableIds() = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val itemHeaderViewBinding = GameInfoItemHeaderBinding.bind(
            convertView ?: LayoutInflater.from(context).inflate(R.layout.game_info_item_header, parent, false)
        )

        val leaderboard = getGroup(groupPosition)
        itemHeaderViewBinding.title.text = if (leaderboard.subcategories.isEmpty()) {
            leaderboard.categoryName
        } else "${leaderboard.categoryName} - ${leaderboard.subcategories.joinToString(" ")}"

        itemHeaderViewBinding.expandImg.setImageResource(
            if (isExpanded) R.drawable.ic_expand_less_black_24dp
            else R.drawable.ic_expand_more_black_24dp
        )

        if (groupPosition in initExpandedGroups) {
            (parent as? ExpandableListView)?.expandGroup(groupPosition)
            initExpandedGroups -= groupPosition
        }

        return itemHeaderViewBinding.root
    }

    override fun getChildrenCount(groupPosition: Int) = 1

    override fun getChild(groupPosition: Int, childPosition: Int) = getGroup(groupPosition)

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    @SuppressLint("SetTextI18n")
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val listItemViewBinding = GameInfoItemBinding.bind(
            convertView ?: LayoutInflater.from(context).inflate(R.layout.game_info_item, parent, false)
        )

        val leaderboard = getGroup(groupPosition)
        if (leaderboard.runs.isEmpty()) {
            listItemViewBinding.numOfRunsText.text = context?.getString(R.string.empty_leaderboard)
            context?.let {
                listItemViewBinding.numOfRunsText.setTextColor(it.getColorCpt(android.R.color.secondary_text_light_nodisable))
            }
            listItemViewBinding.numOfRunsText.setTypeface(listItemViewBinding.numOfRunsText.typeface, Typeface.ITALIC)
            listItemViewBinding.wrText.visibility = View.GONE
            listItemViewBinding.placeText.visibility = View.GONE
            listItemViewBinding.buttonWRLink.visibility = View.GONE
        } else {
            context?.let {
                listItemViewBinding.numOfRunsText.setTextColor(it.getColorCpt(android.R.color.primary_text_light))
            }
            listItemViewBinding.numOfRunsText.setTypeface(listItemViewBinding.numOfRunsText.typeface, Typeface.NORMAL)
            listItemViewBinding.wrText.visibility = View.VISIBLE
            listItemViewBinding.placeText.visibility = View.VISIBLE
            listItemViewBinding.buttonWRLink.visibility = View.VISIBLE

            val wrRun = leaderboard.runs[0]
            listItemViewBinding.numOfRunsText.text = "No. of runs on the leaderboard: ${leaderboard.runs.size}"
            listItemViewBinding.wrText.text = "WR is ${wrRun.time.getFormattedTime()}" +
                    " by ${leaderboard.wrRunners} on ${leaderboard.wrPlatform}"

            listItemViewBinding.placeText.visibility = View.GONE
            val category = if (leaderboard.subcategories.isEmpty()) {
                game.getCategoryByName(leaderboard.categoryName)
            } else {
                val extendedCategoryName =
                    "${leaderboard.categoryName} - ${leaderboard.subcategories.joinToString(" ")}"
                game.getCategoryByName(extendedCategoryName)
            }
            category?.let {
                val pb = it.bestTime
                if (pb > 0) {
                    val bopped = leaderboard.runs.find { run -> run.time >= pb }
                    val place = bopped?.place ?: leaderboard.runs[leaderboard.runs.size - 1].place + 1

                    listItemViewBinding.placeText.text = "Your PB would put you at ${place.toOrdinal()} place!"
                    listItemViewBinding.placeText.visibility = View.VISIBLE
                }
            }

            if (wrRun.videoLink != null) {
                listItemViewBinding.buttonWRLink.setOnClickListener {
                    val wrIntent = Intent(Intent.ACTION_VIEW, Uri.parse(wrRun.videoLink.uri))
                    context?.startActivity(wrIntent)
                }
            } else listItemViewBinding.buttonWRLink.visibility = View.GONE
        }

        listItemViewBinding.buttonLBLink.setOnClickListener {
            val lbIntent = Intent(Intent.ACTION_VIEW, Uri.parse(leaderboard.weblink))
            context?.startActivity(lbIntent)
        }

        return listItemViewBinding.root
    }

    override fun getChildId(groupPosition: Int, childPosition: Int) = getGroupId(groupPosition)

    override fun getGroupCount() = data.size

    // TODO: unused
    fun clear() {
        data = listOf()
    }
}
