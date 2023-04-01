package dev.foxgirl.mineseekdestroy.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    public static @NotNull Inventory copyOf(@NotNull Inventory source) {
        return copyOf(source, 0, source.size());
    }

    public static @NotNull Inventory copyOf(@NotNull Inventory source, int sourcePos, int length) {
        var target = create(length);
        for (int i = 0; i < length; i++) {
            target.setStack(i, getStack(source, sourcePos + i).copy());
        }
        return target;
    }

    public static void clear(@NotNull Inventory inventory) {
        Objects.requireNonNull(inventory, "Argument 'inventory'");
        for (int i = 0, length = inventory.size(); i < length; i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }
    }

    public static boolean equals(@NotNull Inventory a, @NotNull Inventory b) {
        Objects.requireNonNull(a, "Argument 'a'");
        Objects.requireNonNull(b, "Argument 'b'");
        int length = a.size();
        if (length != b.size()) return false;
        for (int i = 0; i < length; i++) {
            if (!ItemStack.areEqual(getStack(a, i), getStack(b, i))) return false;
        }
        return true;
    }

    public static @NotNull List<@NotNull ItemStack> list(@NotNull Inventory inventory) {
        Objects.requireNonNull(inventory, "Argument 'inventory'");
        return new InventoryList(inventory);
    }

    public static @NotNull Inventory create(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Argument 'size' is negative");
        }
        return new ArrayInventory(size);
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

    private static final class ArrayInventory implements Inventory {
        private static final ItemStack EMPTY_STACK = ItemStack.EMPTY;
        private static final int MAX_COUNT_PER_STACK = Inventory.MAX_COUNT_PER_STACK;

        private final ItemStack[] stacks;

        private ArrayInventory(int size) {
            stacks = new ItemStack[size];
            clear();
        }

        private boolean checkIndex(int i) {
            return i >= 0 && i < stacks.length;
        }

        @Override
        public String toString() {
            return Arrays.toString(stacks);
        }

        @Override
        public int size() {
            return stacks.length;
        }

        @Override
        public void clear() {
            var stacks = this.stacks;
            for (int i = 0, l = stacks.length; i < l; i++) {
                stacks[i] = EMPTY_STACK;
            }
        }

        @Override
        public boolean isEmpty() {
            for (var stack : stacks) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getStack(int i) {
            return checkIndex(i) ? stacks[i] : EMPTY_STACK;
        }

        @Override
        public void setStack(int i, ItemStack stack) {
            if (checkIndex(i)) {
                if (stack == null) {
                    stack = EMPTY_STACK;
                } else if (stack.getCount() > MAX_COUNT_PER_STACK) {
                    stack.setCount(MAX_COUNT_PER_STACK);
                }
                stacks[i] = stack;
            }
        }

        @Override
        public ItemStack removeStack(int i) {
            if (checkIndex(i)) {
                var stack = stacks[i];
                stacks[i] = EMPTY_STACK;
                return stack;
            }
            return EMPTY_STACK;
        }

        @Override
        public ItemStack removeStack(int i, int amount) {
            if (checkIndex(i) && amount > 0) {
                return stacks[i].split(amount);
            }
            return EMPTY_STACK;
        }

        @Override
        public boolean isValid(int i, ItemStack stack) {
            return checkIndex(i);
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return true;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public int getMaxCountPerStack() {
            return MAX_COUNT_PER_STACK;
        }
    }

}
