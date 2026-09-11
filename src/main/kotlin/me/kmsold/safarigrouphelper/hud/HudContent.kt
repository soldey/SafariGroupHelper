package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.config.OtherBiomesMode
import me.kmsold.safarigrouphelper.data.CritterBiome
import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.data.SafariStats
import me.kmsold.safarigrouphelper.util.TimeFormat
import me.kmsold.safarigrouphelper.util.text
import me.kmsold.safarigrouphelper.util.tr
import net.minecraft.network.chat.Component

/** Things a HUD line can do when clicked. */
enum class HudAction {
    SWITCH_BIOME,
    RESET_RUN,
}

/** A clickable line inside a block. */
data class HudButton(val lineIndex: Int, val action: HudAction)

data class BlockContent(
    val lines: List<Component>,
    val buttons: List<HudButton> = emptyList(),
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
        if (!block.visible) return BlockContent(emptyList())
        val config = ConfigManager.config
        val showProgress = editorPreview || SafariSession.progressVisible
        val selectAllowed = editorPreview || LocationTracker.biomeSelectAllowed(config.critterSafari.hud.biomeSelectVisibility)
        // Clickable lines only exist while a container screen is open, so they cannot be hit by
        // accident while hunting.
        val buttonsVisible = inInventory

        return when (block) {
            HudBlock.MY_BIOME -> myBiome(showProgress, selectAllowed, selectAllowed && buttonsVisible)
            HudBlock.OTHER_BIOMES -> if (showProgress) otherBiomes() else BlockContent(emptyList())
            HudBlock.TOTAL_PROGRESS -> if (showProgress) totalProgress() else BlockContent(emptyList())
            HudBlock.RUN_INFO -> if (showProgress) runInfo(buttonsVisible) else BlockContent(emptyList())
        }
    }

    private fun myBiome(showProgress: Boolean, selectAllowed: Boolean, showButton: Boolean): BlockContent {
        if (!showProgress && !selectAllowed) return BlockContent(emptyList())
        val biome = ConfigManager.config.critterSafari.selectedBiome
        val lines = ArrayList<Component>()
        lines += if (showProgress) {
            tr("hud.biomeHeader", biome.colorCode, biome.translatedName, SafariSession.uniques(biome), biome.total)
        } else {
            tr("hud.biomeHeaderPlain", biome.colorCode, biome.translatedName)
        }
        if (showProgress) {
            for (critter in biome.critters) lines += critterLine(critter)
        }
        val buttons = ArrayList<HudButton>()
        if (showButton) {
            buttons += HudButton(lines.size, HudAction.SWITCH_BIOME)
            lines += tr("hud.switchBiome")
        }
        return BlockContent(lines, buttons)
    }

    private fun critterLine(critter: String): Component {
        val count = SafariSession.countOf(critter)
        return when {
            count > 1 -> tr("hud.critterCaughtRepeat", critter, count)
            count == 1 -> tr("hud.critterCaught", critter)
            else -> tr("hud.critterMissing", critter)
        }
    }

    private fun otherBiomes(): BlockContent {
        val config = ConfigManager.config
        if (config.critterSafari.hud.otherBiomesMode == OtherBiomesMode.OFF) return BlockContent(emptyList())
        val others = CritterBiome.entries.filter { it != config.critterSafari.selectedBiome }
        val lines = ArrayList<Component>()
        lines += tr("hud.otherBiomes")
        for (biome in others) {
            lines += tr(
                "hud.biomeProgress",
                biome.colorCode,
                biome.translatedName,
                SafariSession.uniques(biome),
                biome.total,
            )
            if (config.critterSafari.hud.otherBiomesMode == OtherBiomesMode.FULL) {
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
            percent >= 100 -> "§6"
            percent >= 50 -> "§a"
            else -> "§e"
        }
        return BlockContent(
            listOf(
                tr("hud.total", done, total, color, percent),
                text("$color${"█".repeat(filled)}§8${"█".repeat(BAR_WIDTH - filled)}"),
            ),
        )
    }

    private fun runInfo(showButton: Boolean): BlockContent {
        val lines = ArrayList<Component>()
        lines += tr("hud.time", TimeFormat.clock(SafariSession.elapsedMs))
        lines += tr("hud.catches", SafariSession.state.totalCatches, SafariSession.duplicates)
        SafariStats.personalBestMs?.let { lines += tr("hud.pb", TimeFormat.clock(it)) }

        val buttons = ArrayList<HudButton>()
        if (showButton) {
            buttons += HudButton(lines.size, HudAction.RESET_RUN)
            lines += if (HudInteractions.resetPending) tr("hud.resetConfirm") else tr("hud.resetRun")
        }
        return BlockContent(lines, buttons)
    }
}
