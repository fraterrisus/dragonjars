package com.hitchhikerprod.dragonjars.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Addresses of resources embedded in DRAGON.COM.
 *
 * Different releases of the DOS executable contain the same resources at
 * different file offsets. Keeping those addresses here avoids scattering
 * executable-specific constants through the renderer, text decoder, and
 * music player.
 */
public record ExecutableLayout(
        int littleManTextureAddress,
        int hudRegionLutAddress,
        int cornerLutAddress,
        int romImageLutAddress,
        int fontAddress,
        int stringDecoderLutAddress,
        int titleMusicAddress,
        int titleDurationLutAddress,
        int titleFrequencyLutAddress
) {
    static final String GOG_SHA256 =
            "d1300a340211d8c92e4b767a1e5e75f177f4146d63ca3312052643cd3db9c678";

    /** Layout used by the executable DragonJars originally supported. */
    public static final ExecutableLayout ORIGINAL = new ExecutableLayout(
            0x6500, 0x2544, 0x6428, 0x67c0, 0xb8a2,
            0x1bca, 0x5edc, 0x575e, 0x58fd
    );

    /** Layout used by the current GOG PC executable. */
    public static final ExecutableLayout GOG = new ExecutableLayout(
            0x6720, 0x2694, 0x6648, 0x69e0, 0xbe52,
            0x1c2a, 0x60fc, 0x597e, 0x5b1d
    );

    public static ExecutableLayout detect(Chunk executable) {
        return forSha256(sha256(executable));
    }

    static ExecutableLayout forSha256(String sha256) {
        return GOG_SHA256.equalsIgnoreCase(sha256) ? GOG : ORIGINAL;
    }

    private static String sha256(Chunk chunk) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < chunk.getSize(); i++) {
                digest.update(chunk.getByte(i));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
