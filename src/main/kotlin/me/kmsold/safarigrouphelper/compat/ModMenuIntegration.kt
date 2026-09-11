package me.kmsold.safarigrouphelper.compat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.kmsold.safarigrouphelper.hud.SghConfigScreen

/** Only loaded by Fabric when ModMenu is actually installed. */
class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory { parent -> SghConfigScreen(parent) }
}
