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
import il.ronmad.speedruntimer.getColorCpt
import il.ronmad.speedruntimer.getFormattedTime
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getCategoryByName
import il.ronmad.speedruntimer.toOrdinal
import il.ronmad.speedruntimer.web.SrcLeaderboard
import kotlinx.android.synthetic.main.game_info_item.view.*
import kotlinx.android.synthetic.main.game_info_item_header.view.*

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
        val itemHeader = convertView
                ?: LayoutInflater.from(context)
                        .inflate(R.layout.game_info_item_header, parent, false)

        val leaderboard = getGroup(groupPosition)
        itemHeader.title.text = if (leaderboard.subcategories.isEmpty()) {
            leaderboard.categoryName
        } else "${leaderboard.categoryName} - ${leaderboard.subcategories.joinToString(" ")}"

        itemHeader.expandImg.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less_black_24dp
                else R.drawable.ic_expand_more_black_24dp)

        if (groupPosition in initExpandedGroups) {
            (parent as? ExpandableListView)?.expandGroup(groupPosition)
            initExpandedGroups -= groupPosition
        }

        return itemHeader
    }

    override fun getChildrenCount(groupPosition: Int) = 1

    override fun getChild(groupPosition: Int, childPosition: Int) = getGroup(groupPosition)

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    @SuppressLint("SetTextI18n")
    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val listItem = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.game_info_item, parent, false)

        val leaderboard = getGroup(groupPosition)
        if (leaderboard.runs.isEmpty()) {
            listItem.numOfRunsText.text = context?.getString(R.string.empty_leaderboard)
            context?.let {
                listItem.numOfRunsText.setTextColor(it.getColorCpt(android.R.color.secondary_text_light_nodisable))
            }
            listItem.numOfRunsText.setTypeface(listItem.numOfRunsText.typeface, Typeface.ITALIC)
            listItem.wrText.visibility = View.GONE
            listItem.placeText.visibility = View.GONE
            listItem.buttonWRLink.visibility = View.GONE
        } else {
            context?.let {
                listItem.numOfRunsText.setTextColor(it.getColorCpt(android.R.color.primary_text_light))
            }
            listItem.numOfRunsText.setTypeface(listItem.numOfRunsText.typeface, Typeface.NORMAL)
            listItem.wrText.visibility = View.VISIBLE
            listItem.placeText.visibility = View.VISIBLE
            listItem.buttonWRLink.visibility = View.VISIBLE

            val wrRun = leaderboard.runs[0]
            listItem.numOfRunsText.text = "No. of runs on the leaderboard: ${leaderboard.runs.size}"
            listItem.wrText.text = "WR is ${wrRun.time.getFormattedTime()}" +
                    " by ${leaderboard.wrRunners} on ${leaderboard.wrPlatform}"

            listItem.placeText.visibility = View.GONE
            val category = if (leaderboard.subcategories.isEmpty()) {
                game.getCategoryByName(leaderboard.categoryName)
            } else {
                val extendedCategoryName = "${leaderboard.categoryName} - ${leaderboard.subcategories.joinToString(" ")}"
                game.getCategoryByName(extendedCategoryName)
            }
            category?.let {
                val pb = it.bestTime
                if (pb > 0) {
                    val bopped = leaderboard.runs.find { run -> run.time >= pb }
                    val place = bopped?.place ?: leaderboard.runs[leaderboard.runs.size - 1].place+1

                    listItem.placeText.text = "Your PB would put you at ${place.toOrdinal()} place!"
                    listItem.placeText.visibility = View.VISIBLE
                }
            }

            if (wrRun.videoLink != null) {
                listItem.buttonWRLink.setOnClickListener {
                    val wrIntent = Intent(Intent.ACTION_VIEW, Uri.parse(wrRun.videoLink.uri))
                    context?.startActivity(wrIntent)
                }
            } else listItem.buttonWRLink.visibility = View.GONE
        }

        listItem.buttonLBLink.setOnClickListener {
            val lbIntent = Intent(Intent.ACTION_VIEW, Uri.parse(leaderboard.weblink))
            context?.startActivity(lbIntent)
        }

        return listItem
    }

    override fun getChildId(groupPosition: Int, childPosition: Int) = getGroupId(groupPosition)

    override fun getGroupCount() = data.size

    fun clear() {
        data = listOf()
    }
}
