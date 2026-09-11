package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.config.HudPos
import me.kmsold.safarigrouphelper.l10n.Localization

/** The movable HUD blocks. Each one has its own position, scale and on/off switch. */
enum class HudBlock(
    val id: String,
    private val fallbackLabel: String,
    private val defaultX: Int,
    private val defaultY: Int,
) {
    MY_BIOME("my_biome", "My biome", 4, 4),
    OTHER_BIOMES("other_biomes", "Other biomes", 4, 130),
    TOTAL_PROGRESS("total_progress", "Total progress", 4, 205),
    RUN_INFO("run_info", "Run info / timer", 4, 235),
    ;

    /** Name shown above the block in the HUD editor. */
    val label: String get() = Localization.trOr("hud.block.$id", fallbackLabel)

    val pos: HudPos get() = ConfigManager.config.position(id, defaultX, defaultY)

    /** Switched in Critter Safari -> Hud, not in the position editor. */
    val visible: Boolean
        get() = ConfigManager.config.critterSafari.hud.let {
            when (this) {
                MY_BIOME -> it.showMyBiome
                OTHER_BIOMES -> it.showOtherBiomes
                TOTAL_PROGRESS -> it.showTotalProgress
                RUN_INFO -> it.showRunInfo
            }
        }
}

/** A rectangle in scaled GUI coordinates. */
data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
    fun contains(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
}
