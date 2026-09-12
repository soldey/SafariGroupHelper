package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.l10n.Localization
import me.kmsold.safarigrouphelper.util.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** When a biome was finished and who worked it, the biggest contributor first. */
data class BiomeClear(
    var biome: String = "",
    var atMs: Long = 0L,
    var players: MutableList<String> = ArrayList(),
)

/** One finished run, kept for the history list in the settings screen. */
data class RunRecord(
    var startedAt: Long = 0L,
    var durationMs: Long = 0L,
    var uniques: Int = 0,
    var total: Int = 0,
    var completed: Boolean = false,
    /** False when the run was reset halfway, which also keeps it out of the personal best. */
    var valid: Boolean = true,
    /** Everyone who hunted, the biggest contributor first. */
    var players: MutableList<String> = ArrayList(),
    var biomeClears: MutableList<BiomeClear> = ArrayList(),
)

object RunHistory {

    const val MAX_ENTRIES = 10

    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM HH:mm")

    /**
     * The history block shown under Critter Safari -> Run history, newest run first. One run takes
     * a headline plus, when there is something to say, a line of names and a line per biome.
     */
    fun lines(): List<String> {
        val runs = SafariStats.state.runs
        if (runs.isEmpty()) return listOf(Localization.tr("runHistory.empty"))

        val lines = ArrayList<String>()
        runs.forEachIndexed { index, run ->
            val flag = when {
                !run.valid -> Localization.tr("runHistory.invalid")
                run.completed -> Localization.tr("runHistory.complete")
                else -> ""
            }
            lines += Localization.tr(
                "runHistory.line",
                index + 1,
                formatStart(run.startedAt),
                run.uniques,
                run.total,
                TimeFormat.clock(run.durationMs),
                flag,
            )
            if (run.players.isNotEmpty()) {
                lines += Localization.tr("runHistory.players", run.players.joinToString(", "))
            }
            for (clear in run.biomeClears.sortedBy { it.atMs }) {
                val biome = CritterBiome.entries.find { it.name == clear.biome }
                lines += Localization.tr(
                    "runHistory.biomeClear",
                    biome?.coloredName ?: clear.biome,
                    TimeFormat.clock(clear.atMs),
                    clear.players.joinToString(", ").ifEmpty { "-" },
                )
            }
        }
        return lines
    }

    private fun formatStart(epochMillis: Long): String = runCatching {
        dateFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }.getOrDefault("?")
}
