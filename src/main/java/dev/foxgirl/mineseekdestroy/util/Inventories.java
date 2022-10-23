package dev.foxgirl.mineseekdestroy.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

public final class Inventories {

    private Inventories() {
    }

    private static ItemStack getStack(Inventory inventory, int slot) {
        ItemStack stack = inventory.getStack(slot);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public static void copy(@NotNull Inventory source, @NotNull Inventory target) {
        copy(source, 0, target, 0, Math.min(source.size(), target.size()));
    }

    public static void copy(@NotNull Inventory source, int sourcePos, @NotNull Inventory target, int targetPos, int length) {
        for (int i = 0; i < length; i++) {
            target.setStack(targetPos + i, getStack(source, sourcePos + i).copy());
        }
    }

    public static void clear(@NotNull Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }
    }

    public static @NotNull List<@NotNull ItemStack> list(@NotNull Inventory inventory) {
        Objects.requireNonNull(inventory, "Argument 'inventory'");
        return new InventoryList(inventory);
    }

    private static final class InventoryList extends AbstractList<ItemStack> implements RandomAccess {
        private final Inventory inventory;

        private InventoryList(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public int size() {
            return inventory.size();
        }

        @Override
        public ItemStack get(int index) {
            int size = inventory.size();
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
            }
            return getStack(inventory, index);
        }

        @Override
        public ItemStack set(int index, ItemStack newStack) {
            ItemStack oldStack = get(index);
            inventory.setStack(index, newStack != null ? newStack : ItemStack.EMPTY);
            return oldStack;
        }
    }

}
