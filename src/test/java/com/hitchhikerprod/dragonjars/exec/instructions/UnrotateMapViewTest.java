package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Facing;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class UnrotateMapViewTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte)0x70, // UnrotateMapView
            (byte)0x41, //   heap index
            (byte)0x5a  // Exit
    ));

    private static final MapData.Square SQUARE = new MapData.Square(
            0xffaabb,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            0,
            0,
            Optional.empty(),
            false,
            0
    );

    @Test
    public void north() {
        helper(Facing.NORTH, 5, 5, 0x3faabb);
    }

    @Test
    public void south() {
        helper(Facing.SOUTH, 5, 4, 0x3faabb);
    }

    @Test
    public void east() {
        helper(Facing.EAST, 6, 5, 0xf3aabb);
    }

    @Test
    public void west() {
        helper(Facing.WEST, 5, 5, 0xf3aabb);
    }

    private void helper(Facing facing, int x, int y, int result) {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        final Interpreter j = spy(i);
        final MapData mockMapData = mock(MapData.class);
        doReturn(mockMapData).when(j).mapDecoder();
        doReturn(SQUARE).when(mockMapData).getSquare(anyInt(), anyInt());
        Heap.get(Heap.PARTY_X).write(5);
        Heap.get(Heap.PARTY_Y).write(5);
        Heap.get(Heap.PARTY_FACING).write(facing.index());
        Heap.get(0x41).write(0x03); // new wall metadata (which doesn't make any sense)
        j.start(0,0);
        verify(mockMapData).setSquare(x, y, result);
    }

}