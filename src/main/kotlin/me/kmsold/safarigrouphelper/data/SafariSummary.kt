package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.l10n.Localization
import me.kmsold.safarigrouphelper.util.TimeFormat

/**
 * The block at the top of the Critter Safari page: your best times and how many runs you have
 * behind you, overall and per biome.
 */
object SafariSummary {

    fun lines(): List<String> {
        val stats = SafariStats.state
        val lines = ArrayList<String>()
        lines += Localization.tr(
            "summary.total",
            SafariStats.personalBestMs?.let { TimeFormat.clock(it) } ?: Localization.tr("summary.never"),
            stats.runsStarted,
            stats.runsCompleted,
        )
        for (biome in CritterBiome.entries) {
            lines += Localization.tr(
                "summary.biome",
                biome.coloredName,
                SafariStats.biomeBestMs(biome)?.let { TimeFormat.clock(it) } ?: Localization.tr("summary.never"),
                stats.runsByBiome[biome.name] ?: 0,
            )
        }
        return lines
    }
}
