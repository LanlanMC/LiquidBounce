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

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.UNFAVORABLE_BLOCKS_TO_PLACE
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Direction
import kotlin.math.absoluteValue


/**
 * SidePlace Module
 *
 * Automatically places blocks if the player is looking at the side of a block.
 * Best with Safe Walk or Eagle for bridging.
 *
 * @author LanlanMC
 */
object ModuleSidePlace: ClientModule("SidePlace", Category.WORLD) {
    val placeDelay by intRange("PlaceDelay", 1..1 , 0..5, "Ticks")

    val holdRight by boolean("HoldRight", false)

    /**
     * Stop placing blocks when the left button is pressed, useful for
     * preventing accidental placements while mining.
     */
    val stopOnLeftClick by boolean("StopOnLeftClick", false)

    private object PitchCheck: ToggleableConfigurable(this, "PitchCheck", false) {
        val pitch by floatRange("Pitch", 0f..45f, 0f..90f)
    }

    init {
        tree(PitchCheck)
    }

    private var placeCooldown = 0

    override fun disable() {
        placeCooldown = 0
    }

    val handler = tickHandler {
        placeCooldown--
        if (placeCooldown > 0) return@tickHandler  // Wait for cooldown

        val heldItem = getHeldItem()
        if (heldItem == null
            || heldItem.isEmpty
            || !isValidBlock(heldItem)
            || heldItem.getBlock() in UNFAVORABLE_BLOCKS_TO_PLACE) {
            return@tickHandler
        }  //  hand item
        if (PitchCheck.enabled && 90-player.pitch.absoluteValue !in PitchCheck.pitch) return@tickHandler  //  pitch
        if (stopOnLeftClick && mc.options.attackKey.isPressed) return@tickHandler  //  left-click
        if (holdRight && !mc.options.useKey.isPressed) return@tickHandler  //  right-click

        val raycastResult = raycast(player.rotation)
        if (raycastResult.type != HitResult.Type.BLOCK) return@tickHandler  // Ensure the player is looking at a block
        if (raycastResult.side in arrayOf(Direction.UP, Direction.DOWN)) return@tickHandler  // Sides only

        val suitableHand = arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND).firstOrNull {
            isValidBlock(player.getStackInHand(it))
        }
        doPlacement(raycastResult, suitableHand!!)
        placeCooldown = placeDelay.random()
    }

    private fun getHeldItem() = if (player.mainHandStack != null && player.mainHandStack.item != ItemStack.EMPTY) {
        player.mainHandStack
    } else {
        player.offHandStack
    }
}
