package org.rsmod.content.skills.thieving

import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ThievingStunTimerScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerTimer("timer.thieving_stun") { ThievingStun.clearMoveLock(player) }
        onNpcTimer("timer.thieving_npc_face_reset") { npc.resetFaceEntity() }
    }
}
