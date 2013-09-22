package li.cil.oc.common.container

import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer

/** Utility for inventory containers providing basic re-usable functionality. */
abstract class GenericInventoryContainer(protected val playerInventory: InventoryPlayer, val otherInventory: IInventory) extends Container {
  /** Number of player inventory slots to display horizontally. */
  protected val playerInventorySizeX = InventoryPlayer.getHotbarSize

  /** Subtract four for armor slots. */
  protected val playerInventorySizeY = (playerInventory.getSizeInventory - 4) / playerInventorySizeX

  /** Render size of slots (width and height). */
  protected val slotSize = 18

  def canInteractWith(player: EntityPlayer) = otherInventory.isUseableByPlayer(player)

  override def transferStackInSlot(player: EntityPlayer, index: Int): ItemStack = {
    val slot = inventorySlots.get(index).asInstanceOf[Slot]
    if (slot != null && slot.getHasStack) {
      // Get search range and direction for checking for merge options.
      val playerInventorySize = 4 * 9
      val (begin, length, direction) =
        if (index < otherInventory.getSizeInventory) {
          // Merge the item into the player inventory.
          (otherInventory.getSizeInventory, playerInventorySize, true)
        }
        else {
          // Merge the item into the container inventory.
          (0, otherInventory.getSizeInventory, false)
        }

      val stack = slot.getStack
      val originalStack = stack.copy()
      // TODO this won't check a slot's isItemValidForSlot value...
      if (mergeItemStack(stack, begin, length, direction)) {
        if (stack.stackSize == 0) {
          // We could move everything, clear the slot.
          slot.putStack(null)
        }
        else {
          // Partial move, signal change.
          slot.onSlotChanged()
        }

        if (stack.stackSize != originalStack.stackSize) {
          slot.onPickupFromSlot(player, stack)
          return originalStack
        }
        // else: Nothing changed.
      }
      // else: Merge failed.
    }
    // else: Empty slot.
    null
  }

  /** Render player inventory at the specified coordinates. */
  protected def addPlayerInventorySlots(left: Int, top: Int) = {
    // Show the inventory proper. Start at plus one to skip hot bar.
    for (slotY <- 1 until playerInventorySizeY) {
      for (slotX <- 0 until playerInventorySizeX) {
        val index = slotX + slotY * playerInventorySizeX
        val x = left + slotX * slotSize
        // Compensate for hot bar offset.
        val y = top + (slotY - 1) * slotSize
        addSlotToContainer(new Slot(playerInventory, index, x, y))
      }
    }

    // Show the quick slot bar below the internal inventory.
    val quickBarSpacing = 4
    for (index <- 0 until InventoryPlayer.getHotbarSize) {
      val x = left + index * slotSize
      val y = top + slotSize * (playerInventorySizeY - 1) + quickBarSpacing
      addSlotToContainer(new Slot(playerInventory, index, x, y));
    }
  }
}