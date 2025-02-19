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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.ModuleAutoQueue.modes
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Hand

/**
 * Can be used for different server that use paper to join a game
 */
object AutoQueuePaper : Choice("Paper") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    val repeatable = tickHandler {
        val paper = Slots.Hotbar.findSlotIndex(Items.PAPER) ?: return@tickHandler

        SilentHotbar.selectSlotSilently(ModuleAutoQueue, paper)
        waitTicks(1)
        interaction.interactItem(player, Hand.MAIN_HAND)

        waitTicks(20)
    }

}
