package me.kmsold.safarigrouphelper.data

import me.kmsold.safarigrouphelper.l10n.Language
import me.kmsold.safarigrouphelper.l10n.Localization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The cleared-biome line leaves the client and lands in someone else's chat, so its wording is
 * fixed and must survive any language setting.
 */
class PartyAnnouncementTest {

    @Test
    fun `message wording is exact`() {
        assertEquals("Cavern is cleared", CritterBiome.CAVERN.clearedPartyMessage)
        assertEquals("Forest is cleared", CritterBiome.FOREST.clearedPartyMessage)
        assertEquals("Haunted is cleared", CritterBiome.HAUNTED.clearedPartyMessage)
        assertEquals("Icy is cleared", CritterBiome.ICY.clearedPartyMessage)
    }

    @Test
    fun `message stays english when the mod is running in russian`() {
        Localization.reload(Language.RUSSIAN)
        try {
            assertEquals("Cavern is cleared", CritterBiome.CAVERN.clearedPartyMessage)
        } finally {
            Localization.reload(Language.ENGLISH)
        }
    }
}
