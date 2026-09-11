package me.kmsold.safarigrouphelper.config

import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorInfoText
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import me.kmsold.safarigrouphelper.data.RunHistory
import io.github.notenoughupdates.moulconfig.processor.ProcessedOptionImpl
import me.kmsold.safarigrouphelper.l10n.Localization
import java.lang.reflect.Field
import java.lang.reflect.Proxy

/**
 * Runs every category and option name through the language files before MoulConfig builds the
 * settings screen. The English text in the annotations is the fallback, so a language only has
 * to translate what it wants to.
 *
 * Keys are derived from where the option lives: `CritterSafariHudConfig.hudBackground` becomes
 * `config.critterSafariHud.hudBackground.name` / `.desc`, and a category field named `dev` becomes
 * `config.category.dev.name` / `.desc`.
 */
class LocalizedConfigProcessor(config: SghConfig) : MoulConfigProcessor<SghConfig>(config) {

    override fun beginCategory(container: Any, field: Field, name: String, desc: String) {
        val key = "config.category.${field.name}"
        super.beginCategory(
            container,
            field,
            Localization.trOr("$key.name", name),
            Localization.trOr("$key.desc", desc),
        )
    }

    /** The run history is generated, not configured, so it gets its text here. */
    override fun createOptionGui(option: ProcessedOption, field: Field, configOption: ConfigOption): GuiOptionEditor {
        if (field.declaringClass == RunHistoryConfig::class.java) {
            return GuiOptionEditorInfoText(option, StructuredText.of(RunHistory.summary()))
        }
        return super.createOptionGui(option, field, configOption)
    }

    override fun createProcessedOption(container: Any, field: Field, option: ConfigOption): ProcessedOptionImpl {
        val key = "config.${categoryKeyOf(field)}.${field.name}"
        val translated = translate(
            option,
            Localization.trOr("$key.name", option.name),
            Localization.trOr("$key.desc", option.desc),
        )
        return super.createProcessedOption(container, field, translated)
    }

    /** `HudConfig` -> `hud`, `CritterSafariConfig` -> `critterSafari`. */
    private fun categoryKeyOf(field: Field): String = field.declaringClass.simpleName
        .removeSuffix("Config")
        .replaceFirstChar { it.lowercase() }

    /**
     * Annotations cannot be instantiated, so the translated one is a proxy that answers
     * `name()` and `desc()` itself and delegates everything else to the original.
     */
    private fun translate(original: ConfigOption, name: String, desc: String): ConfigOption =
        Proxy.newProxyInstance(
            ConfigOption::class.java.classLoader,
            arrayOf(ConfigOption::class.java),
        ) { _, method, args ->
            when (method.name) {
                "name" -> name
                "desc" -> desc
                else -> method.invoke(original, *(args ?: emptyArray()))
            }
        } as ConfigOption
}
