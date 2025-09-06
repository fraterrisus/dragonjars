package com.hitchhikerprod.dragonjars.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;
import java.util.prefs.Preferences;

public class AppPreferences {
    private static final AppPreferences INSTANCE = new AppPreferences();

    public static AppPreferences getInstance() { return INSTANCE; }

    private static final String NODE_NAME = "com.hitchhikerprod.dragonjars";
    private static final String PREF_FILE_EXEC = "file.executable";
    private static final String PREF_FILE_DATA1 = "file.data1";
    private static final String PREF_FILE_DATA2 = "file.data2";
    private static final String PREF_AUDIO_VOLUME = "audio.volume";
    private static final String PREF_VIDEO_SCALE = "video.scale";
    private static final String PREF_COMBAT_DELAY = "game.delay";

    private final StringProperty executablePath = new SimpleStringProperty();
    private final StringProperty data1Path = new SimpleStringProperty();
    private final StringProperty data2Path = new SimpleStringProperty();
    private final IntegerProperty volume = new SimpleIntegerProperty();
    private final IntegerProperty scale = new SimpleIntegerProperty();
    private final DoubleProperty combatDelay = new SimpleDoubleProperty();

    private final Preferences onDiskPrefs = Preferences.userRoot().node(NODE_NAME);

    private AppPreferences() {
        executablePath.set(onDiskPrefs.get(PREF_FILE_EXEC, null));
        executablePath.addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) onDiskPrefs.remove(PREF_FILE_EXEC);
            else onDiskPrefs.put(PREF_FILE_EXEC, nVal);
        });

        data1Path.set(onDiskPrefs.get(PREF_FILE_DATA1, null));
        data1Path.addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) onDiskPrefs.remove(PREF_FILE_DATA1);
            else onDiskPrefs.put(PREF_FILE_DATA1, nVal);
        });

        data2Path.set(onDiskPrefs.get(PREF_FILE_DATA2, null));
        data2Path.addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) onDiskPrefs.remove(PREF_FILE_DATA2);
            else onDiskPrefs.put(PREF_FILE_DATA2, nVal);
        });

        volume.set(onDiskPrefs.getInt(PREF_AUDIO_VOLUME, 50));
        volume.addListener((obs, oVal, nVal) -> onDiskPrefs.putInt(PREF_AUDIO_VOLUME, nVal.intValue()));

        scale.set(onDiskPrefs.getInt(PREF_VIDEO_SCALE, 3));
        scale.addListener((obs, oVal, nVal) -> onDiskPrefs.putInt(PREF_VIDEO_SCALE, nVal.intValue()));

        combatDelay.set(onDiskPrefs.getDouble(PREF_COMBAT_DELAY, 5.0));
        combatDelay.addListener((obs, oVal, nVal) -> onDiskPrefs.putDouble(PREF_COMBAT_DELAY, nVal.doubleValue()));
    }

    public StringProperty executablePathProperty() {
        return executablePath;
    }

    public StringProperty data1PathProperty() {
        return data1Path;
    }

    public StringProperty data2PathProperty() {
        return data2Path;
    }

    public IntegerProperty volumeProperty() {
        return volume;
    }

    public IntegerProperty scaleProperty() {
        return scale;
    }

    public DoubleProperty combatDelayProperty() {
        return combatDelay;
    }
}
