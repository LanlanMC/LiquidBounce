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

package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyFireballDelay
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.FlyFireball
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.client.PacketSnapshot
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult


internal object VelocityGrimFull : VelocityMode("GrimFull") {
private val maxStuckTicks by int("MaxStuckTicks", 5, 1..100, "ticks")
    private val onlyOnGround by boolean("OnlyOnGround", true)

    private const val BLOCK_HIT_PITCH = 89.79f

    private var canCancel = false
    private var delay = false
    private var needClick = false
    private var waitForUpdate = false
    private var hitResult: BlockHitResult? = null
    private var shouldSkip = false
    private var stuckTicks = 0
    private val delayedPacketQueue = Queues.newConcurrentLinkedQueue<PacketSnapshot>()

    override fun enable() {
        canCancel = false
        delay = false
        needClick = false
        waitForUpdate = false
        hitResult = null
        shouldSkip = false
        stuckTicks = 0
        delayedPacketQueue.clear()
    }

    override fun disable() {
        delayedPacketQueue.forEach { handlePacket(it.packet) }
        delayedPacketQueue.clear()
    }

    @Suppress("unused")
    private val packetEventHandler = sequenceHandler<PacketEvent> { event ->
        if (ModuleFly.running && ModuleFly.modes.activeChoice in arrayOf(FlyFireball, FlyFireballDelay)) {
            // We need the knockback to perform a fireball jump
            delayedPacketQueue.forEach { handlePacket(it.packet) }
            delayedPacketQueue.clear()
            return@sequenceHandler
        }

        val packet = event.packet

        if (packet is PlayerInteractEntityC2SPacket || packet is PlayerInteractBlockC2SPacket) {
            shouldSkip = true
        }

        if (packet is PlayerMoveC2SPacket && packet.changePosition && waitForUpdate) {
            event.cancelEvent()
        }

        if (event.isCancelled || event.origin == TransferOrigin.OUTGOING) {
            return@sequenceHandler
        }

        if (waitForUpdate && packet is BlockUpdateS2CPacket && packet.pos.equals(player.blockPos)) {
            waitTicks(1)
            waitForUpdate = false
            needClick = false
            return@sequenceHandler
        }

        if (waitForUpdate) {
            return@sequenceHandler
        }

        if (delay) {
            delayedPacketQueue.add(PacketSnapshot(packet, event.origin, System.currentTimeMillis()))
            event.cancelEvent()
            return@sequenceHandler
        }

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            canCancel = true
        }

        when (packet) {
            is EntityVelocityUpdateS2CPacket -> {
                if (packet.entityId != player.id) {
                    return@sequenceHandler
                }
            }
            is ExplosionS2CPacket -> {}
            else -> { return@sequenceHandler }
        }

        if (shouldCancelKnockback()) {
            event.cancelEvent()
            delay = true
            canCancel = false
            needClick = true
        }
    }

    @Suppress("unused")
    private val playerTickEventHandler = handler<PlayerTickEvent> { event ->
        if (needClick) {
            hitResult = raycast(rotation = Rotation(player.yaw, BLOCK_HIT_PITCH))
            val pos = hitResult!!.blockPos.offset(hitResult!!.side)
            if (!pos.equals(player.blockPos) || shouldSkip) {
                hitResult = null
            }
        }

        if (hitResult != null) {
            delay = false
            delayedPacketQueue.forEach { handlePacket(it.packet) }
            delayedPacketQueue.clear()

            if (interaction.interactBlock(player, Hand.MAIN_HAND, hitResult) == ActionResult.SUCCESS) {
                player.swingHand(Hand.MAIN_HAND)
            }

            if (RotationManager.serverRotation.pitch != BLOCK_HIT_PITCH) {
                network.sendPacket(
                    PlayerMoveC2SPacket.LookAndOnGround(
                        player.yaw,
                        BLOCK_HIT_PITCH,
                        player.isOnGround,
                        player.horizontalCollision
                    )
                )
            } else {
                network.sendPacket(
                    PlayerMoveC2SPacket.OnGroundOnly(
                        player.isOnGround,
                        player.horizontalCollision
                    )
                )
            }

            stuckTicks = 0
            waitForUpdate = true
            hitResult = null
            needClick = false
        }

        if (waitForUpdate) {
            event.cancelEvent()
            stuckTicks++
            if (stuckTicks > maxStuckTicks) {
                waitForUpdate = false
                needClick = false
            }
            this.debugParameter("StuckTicks", { stuckTicks })
            this.debugParameter("MaxStuckTicks", { maxStuckTicks })
        }

        shouldSkip = false
    }

    private fun shouldCancelKnockback() = canCancel
                                       && player.activeItem.useAction != UseAction.EAT
                                       && player.activeItem.useAction != UseAction.DRINK
                                       && !InventoryManager.isInventoryOpen
                                       && mc.currentScreen !is GenericContainerScreen
                                       && (!onlyOnGround || player.isOnGround)

}
