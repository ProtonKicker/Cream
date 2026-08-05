package ru.ytkab0bp.beamklipper.view

import android.view.ViewGroup
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SimpleRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val viewType = hashMapOf<Class<*>, Int>()
    private val viewCreator = hashMapOf<Int, SimpleRecyclerItem<*>>()
    private var lastType = 0
    var items: List<SimpleRecyclerItem<*>> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val creator = viewCreator[viewType] ?: error("Unknown viewType $viewType")
        return object : RecyclerView.ViewHolder(creator.onCreateView(parent.context)) {}
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (items[position] as SimpleRecyclerItem<View>).onBindView(holder.itemView)
    }

    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

    override fun getItemViewType(position: Int): Int {
        var t = viewType[items[position].javaClass]
        if (t == null) {
            t = lastType++
            viewType[items[position].javaClass] = t
            viewCreator[t] = items[position]
        }
        return t
    }

    override fun getItemCount(): Int = items.size
}
