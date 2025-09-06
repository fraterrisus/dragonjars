package com.hitchhikerprod.dragonjars.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;
import java.util.prefs.Preferences;

public class AppPreferences {
    private static final AppPreferences INSTANCE = new AppPreferences();

    private static final String NODE_NAME = "com.hitchhikerprod.dragonjars";
    private static final String PREF_FILE_EXEC = "file.executable";
    private static final String PREF_FILE_DATA1 = "file.data1";
    private static final String PREF_FILE_DATA2 = "file.data2";
    private static final String PREF_AUDIO_VOLUME = "audio.volume";
    private static final String PREF_VIDEO_SCALE = "video.scale";

    public static AppPreferences getInstance() { return INSTANCE; }

    private final SimpleStringProperty executablePath = new SimpleStringProperty();
    private final SimpleStringProperty data1Path = new SimpleStringProperty();
    private final SimpleStringProperty data2Path = new SimpleStringProperty();
    private final SimpleIntegerProperty volume = new SimpleIntegerProperty();
    private final SimpleIntegerProperty scale = new SimpleIntegerProperty();

    private final Preferences onDiskPrefs = Preferences.userRoot().node(NODE_NAME);

    private AppPreferences() {
        executablePath.set(onDiskPrefs.get(PREF_FILE_EXEC, null));
        data1Path.set(onDiskPrefs.get(PREF_FILE_DATA1, null));
        data2Path.set(onDiskPrefs.get(PREF_FILE_DATA2, null));
        volume.set(onDiskPrefs.getInt(PREF_AUDIO_VOLUME, 50));
        scale.set(onDiskPrefs.getInt(PREF_VIDEO_SCALE, 3));

        executablePath.addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) onDiskPrefs.remove(PREF_FILE_EXEC);
            else onDiskPrefs.put(PREF_FILE_EXEC, nVal);
        });
        data1Path.addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) onDiskPrefs.remove(PREF_FILE_DATA1);
            else onDiskPrefs.put(PREF_FILE_DATA1, nVal);
        });
        data2Path.addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) onDiskPrefs.remove(PREF_FILE_DATA2);
            else onDiskPrefs.put(PREF_FILE_DATA2, nVal);
        });
        volume.addListener((obs, oVal, nVal) -> onDiskPrefs.putInt(PREF_AUDIO_VOLUME, nVal.intValue()));
        scale.addListener((obs, oVal, nVal) -> onDiskPrefs.putInt(PREF_VIDEO_SCALE, nVal.intValue()));
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
}
