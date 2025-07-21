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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.item.Items


/**
 * HeypixelCheck feature
 *
 * HeypixelCheck feature does some checks to detect Heypixel server's custom items, and
 * allows some extra behaviors.
 */
object FeatureHeypixelCheck: ToggleableConfigurable(ModuleInventoryCleaner, "HeypixelCheck", false) {
    fun isHeypixelUsefulItem(item: ItemStack): Boolean {
        return (item.item == Items.GOLDEN_AXE
            && item.damage >= 31
            && item.getEnchantment(Enchantments.SHARPNESS) > 10)  // check if this golden axe is an instant-kill axe
    }
}
