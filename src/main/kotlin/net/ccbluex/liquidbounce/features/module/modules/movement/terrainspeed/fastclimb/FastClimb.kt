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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.fastclimb

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.minecraft.block.LadderBlock
import net.minecraft.block.VineBlock
import net.minecraft.util.math.Direction

/**
 * Fast Climb allows you to climb up ladder-related blocks faster
 */
internal object FastClimb : ToggleableConfigurable(ModuleTerrainSpeed, "FastClimb", true) {

    private val modes = choices(this, "Mode", Motion, arrayOf(Motion, Clip))

    /**
     * Not server or anti-cheat-specific mode.
     * A basic motion fast climb, which should be configurable enough to bypass most anti-cheats.
     */
    private object Motion : Choice("Motion") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val climbMotion by float("Motion", 0.2872F, 0.1f..0.5f)

        val moveHandler = handler<PlayerMoveEvent> {
            if (player.horizontalCollision && player.isClimbing) {
                it.movement.y = climbMotion.toDouble()
            }
        }

    }

    /**
     * A very vanilla-like fast climb. Not working on anti-cheats.
     */
    private object Clip : Choice("Clip") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val moveHandler = handler<PlayerMoveEvent> {

            if (player.isClimbing && mc.options.forwardKey.isPressed) {
                val startPos = player.pos

                val pos = player.blockPos.mutableCopy()
                for (y in 1..8) {
                    pos.y++
                    val block = pos.getBlock()

                    if (block is LadderBlock || block is VineBlock) {
                        player.updatePosition(startPos.x, startPos.y + y, startPos.z)
                    } else {
                        var x = 0.0
                        var z = 0.0
                        when (player.horizontalFacing) {
                            Direction.NORTH -> z = -1.0
                            Direction.SOUTH -> z = 1.0
                            Direction.WEST -> x = -1.0
                            Direction.EAST -> x = 1.0
                            else -> break
                        }

                        player.updatePosition(startPos.x + x, startPos.y + y + 1, startPos.z + z)
                        break
                    }
                }
            }
        }

    }

}
