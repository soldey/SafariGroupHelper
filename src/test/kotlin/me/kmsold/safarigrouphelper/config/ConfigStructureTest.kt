package me.kmsold.safarigrouphelper.config

import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Walks the config exactly like the settings screen does. Without this, a mistyped annotation or
 * a category that silently fails to nest would only be visible by opening the game.
 */
class ConfigStructureTest {

    private fun categories(): Map<String, CategoryInfo> {
        val config = SghConfig()
        val processor = LocalizedConfigProcessor(config)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(config)
        return processor.allCategories.mapValues { (_, category) ->
            CategoryInfo(
                category.displayName.text,
                category.parentCategoryId,
                category.options.map { it.name.text },
            )
        }
    }

    private data class CategoryInfo(val title: String, val parent: String?, val options: List<String>)

    @Test
    fun `every category is discovered`() {
        val titles = categories().values.map { it.title }
        assertTrue("General" in titles, "categories found: $titles")
        assertTrue("Critter Safari" in titles, "categories found: $titles")
        assertTrue("Dev" in titles, "categories found: $titles")
    }

    @Test
    fun `critter safari keeps its main options and gains subcategories`() {
        val categories = categories()
        val safari = categories.values.single { it.title == "Critter Safari" }
        assertEquals(listOf("Enabled", "My biome"), safari.options)

        val safariId = categories.entries.single { it.value.title == "Critter Safari" }.key
        val children = categories.values.filter { it.parent == safariId }.map { it.title }
        assertEquals(listOf("Hud", "Chat", "Run history"), children)
    }

    @Test
    fun `top level categories have no parent`() {
        val topLevel = categories().values.filter { it.title in setOf("General", "Critter Safari", "Dev") }
        assertTrue(topLevel.all { it.parent == null }, "expected top level categories, got $topLevel")
    }

    @Test
    fun `general holds the hud position editor and the language picker`() {
        val general = categories().values.single { it.title == "General" }
        assertEquals(listOf("HUD positions", "Language"), general.options)
    }

    @Test
    fun `the hud subcategory repeats the position editor`() {
        val hud = categories().values.single { it.title == "Hud" }
        assertEquals("HUD positions", hud.options.first())
    }
}
