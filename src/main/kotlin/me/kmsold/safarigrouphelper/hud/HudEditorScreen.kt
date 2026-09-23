package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.compat.McCompat
import me.kmsold.safarigrouphelper.config.ConfigManager
import me.kmsold.safarigrouphelper.util.text
import me.kmsold.safarigrouphelper.util.tr
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent

/**
 * Drag the HUD blocks around. Scroll over a block to scale it, right click to toggle it off,
 * R resets everything to the defaults.
 */
class HudEditorScreen(private val parent: Screen? = null) : Screen(tr("screen.hudEditor.title")) {

    private var dragging: HudBlock? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    override fun isPauseScreen(): Boolean = false

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        HudRenderer.render(graphics, inInventory = true, editorPreview = true)

        for ((block, rect) in HudRenderer.lastBounds) {
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            val color = if (hovered) 0xFFFFFF55.toInt() else 0xFF55FFFF.toInt()
            drawOutline(graphics, rect, color)
            graphics.text(
                font,
                text(block.label, ChatFormatting.GRAY),
                rect.x.toInt(),
                (rect.y - font.lineHeight - 1).toInt().coerceAtLeast(0),
                0xFFFFFFFF.toInt(),
                true,
            )
        }

        val hints = listOf(
            "screen.hudEditor.hintDrag",
            "screen.hudEditor.hintScroll",
            "screen.hudEditor.hintKeys",
        )
        hints.forEachIndexed { index, hint ->
            graphics.text(
                font,
                tr(hint),
                6,
                height - 6 - (hints.size - index) * (font.lineHeight + 1),
                0xFFFFFFFF.toInt(),
                true,
            )
        }
    }

    private fun drawOutline(graphics: GuiGraphicsExtractor, rect: Rect, color: Int) {
        val x = rect.x.toInt()
        val y = rect.y.toInt()
        val x2 = (rect.x + rect.width).toInt()
        val y2 = (rect.y + rect.height).toInt()
        graphics.fill(x, y, x2, y + 1, color)
        graphics.fill(x, y2 - 1, x2, y2, color)
        graphics.fill(x, y, x + 1, y2, color)
        graphics.fill(x2 - 1, y, x2, y2, color)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val block = blockAt(event.x(), event.y())
        if (block != null) {
            dragging = block
            val rect = HudRenderer.lastBounds[block] ?: return true
            dragOffsetX = (event.x() - rect.x).toFloat()
            dragOffsetY = (event.y() - rect.y).toFloat()
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val block = dragging ?: return super.mouseDragged(event, dragX, dragY)
        block.pos.x = (event.x() - dragOffsetX).toInt().coerceAtLeast(0)
        block.pos.y = (event.y() - dragOffsetY).toInt().coerceAtLeast(0)
        ConfigManager.markDirty()
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        dragging = null
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val block = blockAt(mouseX, mouseY) ?: return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        val step = if (scrollY > 0) 0.1f else -0.1f
        block.pos.scale = (block.pos.scale + step).coerceIn(0.5f, 3.0f)
        ConfigManager.markDirty()
        return true
    }

    override fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            ConfigManager.config.positions.clear()
            ConfigManager.markDirty()
            return true
        }
        return super.keyPressed(event)
    }

    override fun onClose() {
        ConfigManager.save()
        McCompat.setScreen(parent)
    }

    private fun blockAt(mouseX: Double, mouseY: Double): HudBlock? =
        HudRenderer.lastBounds.entries.lastOrNull { it.value.contains(mouseX, mouseY) }?.key
}
