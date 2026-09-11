package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.l10n.Localization
import me.kmsold.safarigrouphelper.util.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One finished run, kept for the history list in the settings screen. */
data class RunRecord(
    var startedAt: Long = 0L,
    var durationMs: Long = 0L,
    var uniques: Int = 0,
    var total: Int = 0,
    var completed: Boolean = false,
    /** False when the run was reset halfway, which also keeps it out of the personal best. */
    var valid: Boolean = true,
)

object RunHistory {

    const val MAX_ENTRIES = 10

    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM HH:mm")

    /** The history block shown under Critter Safari -> Run history. */
    fun summary(): String {
        val runs = SafariStats.state.runs
        if (runs.isEmpty()) return Localization.tr("runHistory.empty")
        return runs.mapIndexed { index, run ->
            val flag = when {
                !run.valid -> Localization.tr("runHistory.invalid")
                run.completed -> Localization.tr("runHistory.complete")
                else -> ""
            }
            Localization.tr(
                "runHistory.line",
                index + 1,
                formatStart(run.startedAt),
                run.uniques,
                run.total,
                TimeFormat.clock(run.durationMs),
                flag,
            )
        }.joinToString("\n")
    }

    private fun formatStart(epochMillis: Long): String = runCatching {
        dateFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }.getOrDefault("?")
}
