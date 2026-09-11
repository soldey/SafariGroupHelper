package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.config.OtherBiomesMode
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.data.SafariStats
import me.kmsold.safarigrouphelper.util.TimeFormat
import me.kmsold.safarigrouphelper.util.text
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

const val SWITCH_BIOME_LABEL = "[Switch biome]"

/** Text of one block plus the index of its clickable "switch biome" line, if it has one. */
data class BlockContent(
    val lines: List<Component>,
    val buttonLineIndex: Int = -1,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

/**
 * Builds the text of each HUD block. Separated from rendering so the editor screen and the
 * click handling can reuse the exact same layout.
 */
object HudContent {

    private const val BAR_WIDTH = 20

    fun build(block: HudBlock, inInventory: Boolean, editorPreview: Boolean): BlockContent {
        if (!block.pos.enabled) return BlockContent(emptyList())
        val config = ConfigManager.config
        val showProgress = editorPreview || LocationTracker.inSafari
        val selectAllowed = editorPreview || LocationTracker.biomeSelectAllowed(config.biomeSelectVisibility)
        val showButton = selectAllowed && (inInventory || !config.switchButtonOnlyInInventory)

        return when (block) {
            HudBlock.MY_BIOME -> myBiome(showProgress, selectAllowed, showButton)
            HudBlock.OTHER_BIOMES -> if (showProgress) otherBiomes() else BlockContent(emptyList())
            HudBlock.TOTAL_PROGRESS -> if (showProgress) totalProgress() else BlockContent(emptyList())
            HudBlock.RUN_INFO -> if (showProgress) runInfo() else BlockContent(emptyList())
        }
    }

    private fun myBiome(showProgress: Boolean, selectAllowed: Boolean, showButton: Boolean): BlockContent {
        if (!showProgress && !selectAllowed) return BlockContent(emptyList())
        val biome = ConfigManager.config.selectedBiome
        val lines = ArrayList<Component>()
        lines += header(biome, showProgress)
        if (showProgress) {
            for (critter in biome.critters) lines += critterLine(critter)
        }
        var buttonIndex = -1
        if (showButton) {
            buttonIndex = lines.size
            lines += text(SWITCH_BIOME_LABEL, ChatFormatting.YELLOW)
        }
        return BlockContent(lines, buttonIndex)
    }

    /** Outside the safari only the biome name is shown - progress is a safari-only thing. */
    private fun header(biome: CritterBiome, showProgress: Boolean): Component {
        val line = text("").append(text(biome.displayName, biome.formatting, ChatFormatting.BOLD))
        if (showProgress) {
            line.append(text(" "))
                .append(text("${SafariSession.uniques(biome)}/${biome.total}", ChatFormatting.AQUA))
        }
        return line
    }

    private fun critterLine(critter: String): Component {
        val count = SafariSession.countOf(critter)
        val line = text("")
        return if (count > 0) {
            line.append(text(" ✔ ", ChatFormatting.GREEN)).append(text(critter, ChatFormatting.WHITE))
            if (count > 1) line.append(text(" x$count", ChatFormatting.DARK_GRAY))
            line
        } else {
            line.append(text(" ✖ ", ChatFormatting.DARK_RED)).append(text(critter, ChatFormatting.GRAY))
        }
    }

    private fun otherBiomes(): BlockContent {
        val config = ConfigManager.config
        if (config.otherBiomesMode == OtherBiomesMode.OFF) return BlockContent(emptyList())
        val others = CritterBiome.entries.filter { it != config.selectedBiome }
        val lines = ArrayList<Component>()
        lines += text("Other biomes", ChatFormatting.GRAY, ChatFormatting.BOLD)
        for (biome in others) {
            lines += text("")
                .append(text(biome.displayName, biome.formatting))
                .append(text(" ${SafariSession.uniques(biome)}/${biome.total}", ChatFormatting.AQUA))
            if (config.otherBiomesMode == OtherBiomesMode.FULL) {
                for (critter in biome.critters) lines += critterLine(critter)
            }
        }
        return BlockContent(lines)
    }

    private fun totalProgress(): BlockContent {
        val done = SafariSession.uniqueTotal
        val total = CritterBiome.totalCritterCount
        val percent = if (total == 0) 0 else done * 100 / total
        val filled = if (total == 0) 0 else done * BAR_WIDTH / total
        val color = when {
            percent >= 100 -> ChatFormatting.GOLD
            percent >= 50 -> ChatFormatting.GREEN
            else -> ChatFormatting.YELLOW
        }
        return BlockContent(
            listOf(
                text("")
                    .append(text("Total ", ChatFormatting.GRAY, ChatFormatting.BOLD))
                    .append(text("$done/$total", ChatFormatting.AQUA))
                    .append(text(" ($percent%)", color)),
                text("")
                    .append(text("█".repeat(filled), color))
                    .append(text("█".repeat(BAR_WIDTH - filled), ChatFormatting.DARK_GRAY)),
            ),
        )
    }

    private fun runInfo(): BlockContent {
        val lines = ArrayList<Component>()
        lines += text("")
            .append(text("Time ", ChatFormatting.GRAY))
            .append(text(TimeFormat.clock(SafariSession.elapsedMs), ChatFormatting.YELLOW))
        lines += text("")
            .append(text("Catches ", ChatFormatting.GRAY))
            .append(text("${SafariSession.state.totalCatches}", ChatFormatting.WHITE))
            .append(text(" (${SafariSession.duplicates} repeats)", ChatFormatting.DARK_GRAY))
        SafariStats.personalBestMs?.let {
            lines += text("")
                .append(text("PB ", ChatFormatting.GRAY))
                .append(text(TimeFormat.clock(it), ChatFormatting.LIGHT_PURPLE))
        }
        return BlockContent(lines)
    }
}
