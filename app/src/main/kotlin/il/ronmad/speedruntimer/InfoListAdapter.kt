package il.ronmad.speedruntimer

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import io.realm.Case
import kotlinx.android.synthetic.main.game_info_list_item.view.*

class InfoListAdapter(val context: Context?, val game: Game) : BaseAdapter(), TimeExtensions {

    var data: List<SrcLeaderboard> = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getView(position: Int, convertView: View?, container: ViewGroup?): View {
        val listItem = convertView ?:
        LayoutInflater.from(context).inflate(R.layout.game_info_list_item, container, false)

        val leaderboard = getItem(position)

        listItem.infoLayout.visibility = View.GONE
        listItem.showMoreButton.scaleY = Math.abs(listItem.showMoreButton.scaleY)
        listItem.title.text = if (leaderboard.subcategories.isEmpty()) {
            leaderboard.categoryName
        } else "${leaderboard.categoryName} - ${leaderboard.subcategories.joinToString(" ")}"

        if (leaderboard.runs.isEmpty()) {
            listItem.numOfRunsText.text = context?.getString(R.string.empty_leaderboard)
            context?.let {
                listItem.numOfRunsText.setTextColor(ContextCompat.getColor(it,
                        android.R.color.secondary_text_light_nodisable))
            }
            listItem.numOfRunsText.setTypeface(listItem.numOfRunsText.typeface, Typeface.ITALIC)
            listItem.wrText.visibility = View.GONE
            listItem.placeText.visibility = View.GONE
            listItem.buttonWRLink.visibility = View.GONE
        } else {
            context?.let {
                listItem.numOfRunsText.setTextColor(ContextCompat.getColor(it,
                        android.R.color.primary_text_light))
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
            val categories = if(leaderboard.subcategories.isEmpty()) {
                game.categories
                        .where().equalTo("name", leaderboard.categoryName, Case.INSENSITIVE).findFirst()
            } else {
                val extendedCategoryName = "${leaderboard.categoryName} - ${leaderboard.subcategories.joinToString(" ")}"
                game.categories
                        .where().equalTo("name", extendedCategoryName, Case.INSENSITIVE).findFirst()
            }
            categories?.let {
                val pb = it.bestTime
                if (pb > 0) {
                    val bopped = leaderboard.runs.find { it.time >= pb }
                    val place = bopped?.place ?:
                    leaderboard.runs[leaderboard.runs.size - 1].place + 1

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

        val toggleExpandInfo = {
            listItem.infoLayout.visibility = if (listItem.infoLayout.visibility == View.GONE)
                View.VISIBLE else View.GONE
            listItem.showMoreButton.scaleY *= -1
        }

        listItem.titleLayout.setOnClickListener { toggleExpandInfo() }
        listItem.showMoreButton.setOnClickListener { toggleExpandInfo() }

        return listItem
    }

    override fun getItem(position: Int) = data[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = data.size

    internal fun clear() {
        data = listOf()
    }
}