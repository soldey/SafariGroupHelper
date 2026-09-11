package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.config.ConfigManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Draws the HUD blocks and remembers where they ended up, so the editor screen can drag them
 * and so clicks on the "switch biome" button can be detected.
 */
object HudRenderer {

    private const val PADDING = 3
    private const val LINE_GAP = 1
    private const val BACKGROUND_COLOR = 0x80000000.toInt()
    private const val TEXT_COLOR = 0xFFFFFFFF.toInt()

    /** Bounds of every block as drawn during the last frame, in scaled GUI coordinates. */
    val lastBounds: MutableMap<HudBlock, Rect> = LinkedHashMap()

    /** Clickable lines drawn during the last frame. */
    val buttonBounds: MutableList<Pair<Rect, HudAction>> = ArrayList()

    fun actionAt(mouseX: Double, mouseY: Double): HudAction? =
        buttonBounds.lastOrNull { it.first.contains(mouseX, mouseY) }?.second

    fun render(graphics: GuiGraphicsExtractor, inInventory: Boolean, editorPreview: Boolean = false) {
        lastBounds.clear()
        buttonBounds.clear()
        if (!ConfigManager.config.critterSafari.enabled) return

        for (block in HudBlock.entries) {
            val content = HudContent.build(block, inInventory, editorPreview)
            if (content.isEmpty) continue
            drawBlock(graphics, block, content, editorPreview)
        }
    }

    private fun drawBlock(
        graphics: GuiGraphicsExtractor,
        block: HudBlock,
        content: BlockContent,
        editorPreview: Boolean,
    ) {
        val minecraft = Minecraft.getInstance()
        val font = minecraft.font
        val pos = block.pos
        val scale = pos.scale.coerceIn(0.5f, 3.0f)
        val lineHeight = font.lineHeight + LINE_GAP

        val innerWidth = content.lines.maxOf { font.width(it) }
        val innerHeight = content.lines.size * lineHeight - LINE_GAP
        val width = (innerWidth + PADDING * 2) * scale
        val height = (innerHeight + PADDING * 2) * scale

        val x = pos.x.toFloat().coerceIn(0f, (graphics.guiWidth() - width).coerceAtLeast(0f))
        val y = pos.y.toFloat().coerceIn(0f, (graphics.guiHeight() - height).coerceAtLeast(0f))
        lastBounds[block] = Rect(x, y, width, height)

        val matrix = graphics.pose()
        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale, scale)

        if (ConfigManager.config.critterSafari.hud.hudBackground || editorPreview) {
            graphics.fill(0, 0, innerWidth + PADDING * 2, innerHeight + PADDING * 2, BACKGROUND_COLOR)
        }
        content.lines.forEachIndexed { index, line ->
            graphics.text(font, line, PADDING, PADDING + index * lineHeight, TEXT_COLOR, true)
        }

        matrix.popMatrix()

        for (button in content.buttons) {
            val line = content.lines.getOrNull(button.lineIndex) ?: continue
            buttonBounds += Rect(
                x + PADDING * scale,
                y + (PADDING + button.lineIndex * lineHeight) * scale,
                font.width(line) * scale,
                font.lineHeight * scale,
            ) to button.action
        }
    }
}
