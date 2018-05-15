package il.ronmad.speedruntimer

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.split_list_item.view.*

class SplitRecyclerViewAdapter(data: OrderedRealmCollection<Split>)
    : RealmRecyclerViewAdapter<Split, SplitRecyclerViewAdapter.SplitViewHolder>(data,  true) {

    private val segmentSplitTimes = data.fold(longArrayOf()) { acc, split ->
        acc + if (acc.isNotEmpty()) acc.last() + split.pbTime else split.pbTime }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SplitViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.split_list_item, parent, false))

    override fun onBindViewHolder(holder: SplitViewHolder, position: Int) {
        val split = getItem(position) ?: return
        holder.split = split
        holder.splitNameText.text = split.name
        holder.segmentDurationText.text = split.pbTime.getFormattedTime()
        holder.splitTimeText.text = segmentSplitTimes[position].getFormattedTime()
    }

    class SplitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var split: Split
        val splitNameText: TextView = itemView.nameText
        val segmentDurationText: TextView = itemView.segmentDurationText
        val splitTimeText: TextView = itemView.splitTimeText
    }
}