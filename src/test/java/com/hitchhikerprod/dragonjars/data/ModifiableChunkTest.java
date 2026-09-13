package com.hitchhikerprod.dragonjars.data;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifiableChunkTest {
    private record Results(int invalidations, int changes) {}

    @Test
    public void writeOne() {
        final ModifiableChunk c = new ModifiableChunk(
                List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04)
        );
        c.write(1, 1, 0xff);
        assertEquals(0xff, c.getUnsignedByte(1));
    }

    @Test
    public void writeFour() {
        final ModifiableChunk c = new ModifiableChunk(
                List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04)
        );
        c.write(1, 4, 0x8090a0b0);
        assertEquals(0xb0, c.getUnsignedByte(1));
        assertEquals(0xa0, c.getUnsignedByte(2));
        assertEquals(0x90, c.getUnsignedByte(3));
        assertEquals(0x80, c.getUnsignedByte(4));
    }

    @Test
    public void setBytes() {
        final ModifiableChunk c = new ModifiableChunk(
                List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04)
        );
        final List<Byte> newBytes = List.of((byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d);
        c.setBytes(1, newBytes);
        assertEquals(0x0a, c.getUnsignedByte(1));
        assertEquals(0x0b, c.getUnsignedByte(2));
        assertEquals(0x0c, c.getUnsignedByte(3));
        assertEquals(0x0d, c.getUnsignedByte(4));
    }

    private Results watchTest(IntegerBinding binding, Runnable testFunction) {
        final IntegerProperty invalidCount = new SimpleIntegerProperty(0);
        final IntegerProperty changeCount = new SimpleIntegerProperty(0);
        final InvalidationListener iListener = (obs) -> invalidCount.set(invalidCount.get() + 1);
        final ChangeListener<? super Number> cListener = (obs, oVal, nVal) -> changeCount.set(changeCount.get() + 1);

        binding.addListener(iListener);
        binding.addListener(cListener);
        testFunction.run();
        binding.removeListener(iListener);
        binding.removeListener(cListener);

        return new Results(invalidCount.get(), changeCount.get());
    }

    @Test
    public void watchTriggersOnWrite() {
        final ModifiableChunk c = new ModifiableChunk(
                List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04)
        );

        final IntegerBinding binding = c.watch(1,1);

        final var results = watchTest(binding, () -> c.write(1, 1, 0xff));

        // One byte written tends to result in one Invalidation and one Change
        assertEquals(1, results.invalidations());
        assertEquals(1, results.changes());
    }

    @Test
    public void watchTriggersOnMultiByteWrite() {
        final ModifiableChunk c = new ModifiableChunk(
                List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04)
        );

        final IntegerBinding binding = c.watch(1,2);

        final var results = watchTest(binding, () -> c.write(1, 2, 0xffff));

        // We don't guarantee the minimum number of changes, just that there's at least one
        assertTrue(results.invalidations() > 0); // likely 4
        assertTrue(results.changes() > 0); // likely 2
    }

    @Test
    public void watchTriggersOnOverlappingWrite() {
        final ModifiableChunk c = new ModifiableChunk(
                List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04)
        );

        final IntegerBinding binding = c.watch(1,1);

        final var results = watchTest(binding, () -> c.write(0, 2, 0xffff));

        assertTrue(results.invalidations() > 0);
        assertTrue(results.changes() > 0);
    }

    @Test
    public void watchWriteToUnwatchedIndex() {
        final ModifiableChunk c = new ModifiableChunk(
                List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04)
        );

        final IntegerBinding binding = c.watch(1,1);

        final var results = watchTest(binding, () -> c.write(3, 1, 0xff));

        // Note that the binding *does* invalidate, because the underlying ObservableList invalidates itself on write.
        // (Even though we bound to a .valueOf binding, which is theoretically a one-value observer.)
        // But the value of the binding *does not* change.
        assertEquals(1, results.invalidations());
        assertEquals(0, results.changes());
    }
}
