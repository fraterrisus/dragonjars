package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.SaveChunkTask;
import com.hitchhikerprod.dragonjars.ui.AppPreferences;
import javafx.scene.control.Alert;

import java.util.Objects;

/* chunk 0x07 (party data) starts at 0x2e19 and has size 0x1600
 * chunk 0x10 (dirty map) starts at 0x6642 and has size 0x2000
 *   heap[98] <- segment for chunk 0x10 (0x04, @4067, == mapDecoder.primary)
 *   heap[56] -> segment for chunk 0x46 (0x03, formerly-clean primary data for map ID 0x00)
 *   copy [56] to [98], then write chunk 0x10
 */

public class PersistChunk implements Instruction {
    // This instruction only ever gets called on chunks 0x07 (party data) and 0x10 (dirty map data).
    // See [0f/0085] for the code that copies party data (and the heap) from segment 1 into chunk 0x07 before persisting
    // See [0f/00c1] for the code that copies dirty map data from the map decoder into chunk 0x10
    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr(OPCODE);
        final int chunkId = i.getAL();
        final Chunk chunkData = getChunkData(i, chunkId);
        if (Objects.isNull(chunkData)) return nextIP;

        final AppPreferences prefs = AppPreferences.getInstance();
        final String data1Path = prefs.data1PathProperty().get();
        final String data2Path = prefs.data2PathProperty().get();
        if (Objects.isNull(data1Path) || Objects.isNull(data2Path)) {
            final Alert alert = new Alert(Alert.AlertType.ERROR, "Can't save game without DATA1 and DATA2");
            alert.showAndWait();
            return nextIP;
        }

        final SaveChunkTask task = new SaveChunkTask(data1Path, data2Path, chunkId, chunkData);
        task.setOnSucceeded(event -> i.start(nextIP));
        task.setOnFailed(event -> {
            final Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save your game.");
            alert.showAndWait();
            i.start(nextIP);
        });

        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return null;
    }

    private Chunk getChunkData(Interpreter i, Integer chunkId) {
        final int segmentId = i.memory().lookupChunkId(chunkId);
        if (segmentId == -1) {
            System.err.println("Chunk ID " + chunkId + " is not loaded, refusing to save it");
            return null;
        }
        if (i.memory().getSegmentFrob(segmentId) == Frob.FROZEN) {
            System.err.println("Chunk ID " + chunkId + " is marked FROZEN, refusing to save it");
            return null;
        }
        return i.memory().getSegment(segmentId);
    }
}
