package me.kmsold.safarigrouphelper.l10n

/**
 * The languages the mod ships with.
 *
 * Adding one is two steps and no code changes anywhere else:
 *  1. drop `assets/safarigrouphelper/lang/<code>.json` into the resources, copied from
 *     `en_us.json` - any key you leave out falls back to English;
 *  2. add an entry here with the same code and the language's own name.
 */
enum class Language(val code: String, private val label: String) {
    /** Follows whatever language Minecraft itself is set to, English if we have no file for it. */
    AUTO("", "Auto"),
    ENGLISH("en_us", "English"),
    RUSSIAN("ru_ru", "Русский"),
    ;

    override fun toString(): String = label

    companion object {
        const val FALLBACK_CODE = "en_us"

        fun byCode(code: String): Language? = entries.find { it.code.equals(code, ignoreCase = true) }
    }
}
