package com.healthio.ui.stats

import com.patrykandpatrick.vico.core.entry.ChartEntry

class MacroEntry(
    override val x: Float,
    override val y: Float,
    val grams: Int,
    val type: String // "P", "C", "F"
) : ChartEntry {
    override fun withY(y: Float): ChartEntry = MacroEntry(x, y, grams, type)
}
