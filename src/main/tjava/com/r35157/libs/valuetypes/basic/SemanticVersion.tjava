package com.r35157.libs.valuetypes.basic;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic Versioning (SemVer):
 * A version number has the form MAJOR.MINOR.PATCH.
 * - Increment MAJOR for incompatible API changes,
 * - Increment MINOR for added functionality in a backward-compatible way,
 * - Increment PATCH for backward-compatible bug fixes or improvements.
 * TODO: Not the whole specification is implemented yet!
 */
public record SemanticVersion(int major, int minor, int patch) {
    /**
     * Creates a SemanticVersion and validates that all components are valid.
     *
     * @throws IllegalArgumentException if any of {@code major}, {@code minor}, or {@code patch} is negative
     */
    public SemanticVersion {
        initializationGuardClause(major, minor, patch);
    }

    public static SemanticVersion of(int major, int minor, int patch) {
        return new SemanticVersion(major, minor, patch);
    }

    public static SemanticVersion of(int major, int minor) {
        return of(major, minor, 0);
    }

    public static SemanticVersion of(int major) {
        return of(major, 0);
    }

    public static SemanticVersion of(@NotNull String versionStr) {
        final String s = versionStr.trim();

        final Matcher m = SEMVER_REGEX.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid semantic version: '" + versionStr + "'!");
        }

        try {
            final int major = Integer.parseInt(m.group(1));
            final int minor = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            final int patch = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
            return of(major, minor, patch);
        } catch (NumberFormatException e) {
            // Happens only with overruns
            throw new IllegalArgumentException("Invalid semantic version: '" + versionStr + "'!", e);
        }
    }

    @Override
    public @NotNull String toString() {
        return "%d.%d.%d".formatted(major, minor, patch);
    }

    private void initializationGuardClause(int major, int minor, int patch) throws IllegalArgumentException {
        if (major < 0) throw new IllegalArgumentException("Version element 'major' cannot be negative - was '" + major + "'!");
        if (minor < 0) throw new IllegalArgumentException("Version element 'minor' cannot be negative - was '" + minor + "'!");
        if (patch < 0) throw new IllegalArgumentException("Version element 'patch' cannot be negative - was '" + patch + "'!");
    }

    private static final Pattern SEMVER_REGEX =
            Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?$");
}
