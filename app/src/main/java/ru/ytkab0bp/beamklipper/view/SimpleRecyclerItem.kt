package ru.ytkab0bp.beamklipper.view

import android.content.Context
import android.view.View

abstract class SimpleRecyclerItem<V : View> {
    abstract fun onCreateView(ctx: Context): V
    open fun onBindView(view: V) {}
}
