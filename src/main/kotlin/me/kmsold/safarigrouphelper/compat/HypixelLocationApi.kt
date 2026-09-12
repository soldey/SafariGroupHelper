package me.kmsold.safarigrouphelper.compat

import me.kmsold.safarigrouphelper.SafariGroupHelper
import me.kmsold.safarigrouphelper.data.LocationTracker
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import kotlin.jvm.optionals.getOrNull

/**
 * Location updates straight from Hypixel. Only touched when the `hypixel-mod-api` mod is
 * installed - see the guard in [SafariGroupHelper.onInitializeClient], which keeps this class
 * from being loaded otherwise.
 */
object HypixelLocationApi {

    fun register() {
        val api = HypixelModAPI.getInstance()
        api.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        api.createHandler(ClientboundLocationPacket::class.java) { packet ->
            val mode = packet.mode.getOrNull()
            val map = packet.map.getOrNull()
            val server = packet.serverName
            SafariGroupHelper.logger.debug("Hypixel location: server={} mode={} map={}", server, mode, map)
            LocationTracker.onLocationPacket(server, mode, map)
        }
        SafariGroupHelper.logger.info("Using hypixel-mod-api for location detection")
    }
}
