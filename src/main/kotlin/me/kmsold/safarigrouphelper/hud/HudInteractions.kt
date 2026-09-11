package me.kmsold.safarigrouphelper.hud

import me.kmsold.safarigrouphelper.data.LocationTracker
import me.kmsold.safarigrouphelper.data.SafariSession
import me.kmsold.safarigrouphelper.util.ChatOut
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/** What happens when a clickable HUD line is pressed. */
object HudInteractions {

    /** Resetting a run throws progress away, so the button asks for a second click. */
    private const val CONFIRM_WINDOW_MS = 5000L

    private var resetConfirmUntil = 0L

    val resetPending: Boolean get() = System.currentTimeMillis() < resetConfirmUntil

    fun click(action: HudAction, parent: Screen?) {
        when (action) {
            HudAction.SWITCH_BIOME -> Minecraft.getInstance().setScreen(BiomeSelectScreen(parent))
            HudAction.RESET_RUN -> resetRun()
        }
    }

    private fun resetRun() {
        if (!resetPending) {
            resetConfirmUntil = System.currentTimeMillis() + CONFIRM_WINDOW_MS
            return
        }
        resetConfirmUntil = 0
        resetRunNow()
    }

    /** Clears the run and restarts the timer right away when we are still inside the safari. */
    fun resetRunNow() {
        SafariSession.reset()
        if (LocationTracker.inSafari) {
            SafariSession.startOrResume()
            // Resetting without leaving the safari means capsules are already gone, so the new
            // run cannot be measured against the others.
            SafariSession.invalidateForPersonalBest()
            ChatOut.send("chat.runResetInSafari")
        } else {
            ChatOut.send("chat.runReset")
        }
        SafariSession.save()
    }
}
