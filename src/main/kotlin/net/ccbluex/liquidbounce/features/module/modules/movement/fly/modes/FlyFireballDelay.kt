/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.Entity
import net.minecraft.item.FireChargeItem
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.lwjgl.glfw.GLFW
import java.util.concurrent.LinkedBlockingQueue
import java.lang.String

internal object FlyFireballDelay : Choice("FireballDelay") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    private val velocityBeforeExplosion by boolean("VelocityBeforeExplosion", false)

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var target: Entity? = null
    private var s12count = 0
//
//    object Jump : ToggleableConfigurable(this, "Jump", true) {
//        val delay by int("Delay", 3, 0..20, "ticks")
//    }
//
//    val sprint by boolean("Sprint", true)
//    // Stop moving when module is active to avoid falling off, for example, a bridge.
//    val stopMove by boolean("StopMove", true)
//    var canMove = true
//
//    object Rotations : RotationsConfigurable(this) {
//        val pitch by float("Pitch", 90f, 0f..90f)
//        val backwards by boolean("Backwards", true)
//    }
//
//    // Silent fireball selection
//    object AutoFireball : ToggleableConfigurable(this, "AutoFireball", true) {
//        val slotResetDelay by int("SlotResetDelay", 5, 0..20, "ticks")
//    }
//
//    init {
//        tree(Jump)
//        tree(Rotations)
//        tree(AutoFireball)
//    }

    @Suppress("unused")
    private     val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        when (packet) {
            is CommonPongC2SPacket, is KeepAliveC2SPacket -> {
                event.cancelEvent()
                packets.add(packet)
            }
            is PlayerInteractEntityC2SPacket -> {
                if (target == null) event.cancelEvent()
            }
            is EntityVelocityUpdateS2CPacket -> {
                if (packet.entityId == player.id) {
                    event.cancelEvent()
                    packets.add(packet)
                    s12count++
                }
            }
            is ExplosionS2CPacket -> {
                event.cancelEvent()
                packets.add(packet)
                s12count++
            }
            is BlockUpdateS2CPacket, is BlockEventS2CPacket, is BlockEntityUpdateS2CPacket -> {
                event.cancelEvent()
                packets.add(packet)
            }
            is EntityS2CPacket -> {
                if (packet.getEntity(world) == player) {
                    event.cancelEvent()
                    packets.add(packet)
                }
            }
            is PlayerPositionLookS2CPacket -> packets.add(packet)
        }
    }

    @Suppress("unused")
    val keyboardKeyEventHandler = handler<KeyboardKeyEvent> { event ->
        if (packets.isEmpty() || s12count <= 0 || event.key.code != GLFW.GLFW_KEY_K) return@handler

        var c0fId = -1
        var lastId = -1
        var packet = packets.take()

        fun isVelocityNonZero(velocity: Vec3d) = velocity.x != 0.0 || velocity.y != 0.0 || velocity.z != 0.0

        while (!(packet is EntityVelocityUpdateS2CPacket
                || (packet is ExplosionS2CPacket
                && (isVelocityNonZero(packet.playerKnockback.get()))))
        ) {
            if (packet is CommonPongC2SPacket) {
                val newId = packet.parameter

                if (c0fId != -1) {
                    if (c0fId - newId == 1) {
                        lastId = c0fId
                        c0fId = newId
                    } else if (lastId != -1 && lastId - newId == 1) {
                        c0fId = lastId
                    }
                } else {
                    c0fId = newId
                    lastId = -1
                }

                if (c0fId != -1 && lastId == -1) {
                    c0fId = newId
                }
            }
            sendPacketSilently(packet)
            packet = packets.take()
        }
        sendPacketSilently(packet)
        s12count--
        chat("lastC0f : $lastId")
        chat("C0f : $c0fId")
        while (!(packet is CommonPongC2SPacket && packet.parameter == c0fId - 1)) {
            if (packet is CommonPongC2SPacket) {
                chat(String.valueOf(packet.parameter))
            }
            packet = packets.take()
            sendPacketSilently(packet)
        }
        sendPacketSilently(packets.take())
    }

    private fun blink() {
        val velocityUpdatePackets = LinkedBlockingQueue<EntityVelocityUpdateS2CPacket?>()
        val explosionPackets = LinkedBlockingQueue<ExplosionS2CPacket?>()
        val currentPackets = LinkedBlockingQueue<Packet<*>?>()
        while (!packets.isEmpty()) {
            val packet = packets.take()
            when (packet) {
                is EntityVelocityUpdateS2CPacket -> velocityUpdatePackets.add(packet)
                is ExplosionS2CPacket ->  explosionPackets.add(packet)
                else -> currentPackets.add(packet)
            }
        }
        if (velocityBeforeExplosion) {
            while (!velocityUpdatePackets.isEmpty()) {
                velocityUpdatePackets.take()?.let { sendPacketSilently(it) }
            }
        }

        while (!explosionPackets.isEmpty()) {
            explosionPackets.take()?.let { sendPacketSilently(it) }
        }
        if (!velocityBeforeExplosion) {
            while (!velocityUpdatePackets.isEmpty()) {
                velocityUpdatePackets.take()?.let { sendPacketSilently(it) }
            }
        }
        while (!currentPackets.isEmpty()) {
            currentPackets.take()?.let { sendPacketSilently(it) }
        }
        target = null
    }

    override fun enable() {
        s12count = 0
        target = ModuleKillAura.targetTracker.target
    }

    override fun disable() {
        blink()
    }
//
//    @Suppress("unused")
//    val rotationUpdateHandler = handler<RotationUpdateEvent> {
//        RotationManager.setRotationTarget(
//            Rotation(if (Rotations.backwards) this.invertYaw(player.yaw) else player.yaw, Rotations.pitch),
//            configurable = Rotations,
//            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
//            provider = ModuleFly
//        )
//    }
//
//    @Suppress("unused")
//    val movementInputHandler = sequenceHandler<MovementInputEvent> { event ->
//        if (stopMove && !canMove) {
//            event.directionalInput = DirectionalInput.Companion.BACKWARDS  // Cancel out movement.
//        }
//    }
//
//    @Suppress("unused")
//    val repeatable = tickHandler {
//        if (AutoFireball.enabled) {
//            val bestMainHandSlot = findFireballSlot()
//            if (bestMainHandSlot != null) {
//                SilentHotbar.selectSlotSilently(this, bestMainHandSlot, AutoFireball.slotResetDelay)
//            } else {
//                SilentHotbar.resetSlot(this)
//            }
//        } else {
//            SilentHotbar.resetSlot(this)
//        }
//
//        canMove = !stopMove
//
//        if (Jump.enabled && player.isOnGround) {
//            player.jump()
//        }
//
//        if (Jump.enabled) {
//            waitTicks(Jump.delay)
//        }
//
//        throwFireball()
//
//        if (sprint) {
//            player.isSprinting = true
//        }
//
//        ModuleFly.enabled = false  // Disable after the fireball was thrown
//        canMove = true
//    }
//
//    /**
//     * Inverts yaw (-180 to 180)
//     */
//    private fun invertYaw(yaw: Float): Float {
//        return MathHelper.wrapDegrees(yaw + 180)
//    }
//
//    private fun findFireballSlot(): Int? {
//        return (0..8).firstOrNull {
//            val stack = player.inventory.getStack(it)
//            stack.item is FireChargeItem
//        }
//    }
//
//    fun throwFireball() {
//        interactItem(Hand.MAIN_HAND)
//    }
}
