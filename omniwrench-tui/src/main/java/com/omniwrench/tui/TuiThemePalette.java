package com.omniwrench.tui;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Semantic XTerm-256 and ANSI color palette engine for minimal and fullscreen TUI modes.
 * Inspired by OpenCode ThemeProvider, Crush, and Cyberpunk HUD design systems.
 *
 * <p>Traceability:
 * <ul>
 *   <li>Requirement: REQ-00001 (Dual Headless CLI &amp; Interactive TUI Presentation Engine)</li>
 *   <li>Requirement: REQ-00072 (Adaptive Multi-Theme Engine)</li>
 *   <li>Requirement: REQ-00099 (XTerm-256 Color Palette TUI Visual Theming Engine)</li>
 *   <li>Requirement: REQ-00100 (Dual-Mode Minimal Inline Stream &amp; Full-Screen Resizable TUI Workbench)</li>
 *   <li>Feature: FR-00028 (Adaptive Multi-Theme Engine), FR-00047 (XTerm-256 TUI Theming Engine)</li>
 *   <li>Use Case: UC-00001 (Interactive TUI Pair Programming)</li>
 *   <li>Task: TSK-20261101-016 (TUI Revamp: Minimal Skeleton &amp; Fullscreen Resizable Workbench)</li>
 *   <li>Rule: CS-0030.1.1 (Defensive Preconditions), CS-0030.11 (Value Object Equality)</li>
 *   <li>ADR: ADR-0034 (Adaptive Multi-Theme Engine), ADR-0057 (XTerm-256 Color Palette), ADR-0063 (Dual-Mode TUI)</li>
 * </ul>
 */
public final class TuiThemePalette {

    /** ANSI reset sequence (REQ-00099, FR-00047). */
    public static final String RESET = "\u001B[0m";
    /** ANSI bold sequence (REQ-00099, FR-00047). */
    public static final String BOLD = "\u001B[1m";
    /** ANSI dim sequence (REQ-00099, FR-00047). */
    public static final String DIM = "\u001B[2m";

    /** Built-in Cyberpunk Neon XTerm-256 palette (REQ-00099, FR-00047). */
    public static final TuiThemePalette CYBERPUNK = new TuiThemePalette(
            "cyberpunk",
            new BrandColors("\u001B[38;5;51m", "\u001B[38;5;201m", "\u001B[38;5;226m", "\u001B[97m", "\u001B[38;5;245m"),
            new StatusColors("\u001B[38;5;48m", "\u001B[38;5;214m", "\u001B[38;5;196m"),
            new SurfaceColors("\u001B[48;5;234m", "\u001B[48;5;235m", "\u001B[48;5;233m")
    );

    /** Built-in OpenCode Dark Neutral + Warm Amber/Cyan XTerm-256 palette (REQ-00099, FR-00047). */
    public static final TuiThemePalette OPENCODE = new TuiThemePalette(
            "opencode",
            new BrandColors("\u001B[38;5;215m", "\u001B[38;5;117m", "\u001B[38;5;180m", "\u001B[38;5;254m", "\u001B[38;5;243m"),
            new StatusColors("\u001B[38;5;114m", "\u001B[38;5;215m", "\u001B[38;5;203m"),
            new SurfaceColors("\u001B[48;5;233m", "\u001B[48;5;234m", "\u001B[48;5;232m")
    );

    /** Built-in Dracula Purple/Pink/Cyan XTerm-256 palette (REQ-00099, FR-00047). */
    public static final TuiThemePalette DRACULA = new TuiThemePalette(
            "dracula",
            new BrandColors("\u001B[38;5;141m", "\u001B[38;5;212m", "\u001B[38;5;117m", "\u001B[38;5;255m", "\u001B[38;5;103m"),
            new StatusColors("\u001B[38;5;84m", "\u001B[38;5;228m", "\u001B[38;5;203m"),
            new SurfaceColors("\u001B[48;5;235m", "\u001B[48;5;236m", "\u001B[48;5;234m")
    );

    /** Built-in Nord Arctic Blue XTerm-256 palette (REQ-00099, FR-00047). */
    public static final TuiThemePalette NORD = new TuiThemePalette(
            "nord",
            new BrandColors("\u001B[38;5;110m", "\u001B[38;5;109m", "\u001B[38;5;180m", "\u001B[38;5;254m", "\u001B[38;5;244m"),
            new StatusColors("\u001B[38;5;150m", "\u001B[38;5;222m", "\u001B[38;5;167m"),
            new SurfaceColors("\u001B[48;5;236m", "\u001B[48;5;237m", "\u001B[48;5;235m")
    );

    /** Built-in Matrix Phosphor Green XTerm-256 palette (REQ-00099, FR-00047). */
    public static final TuiThemePalette MATRIX = new TuiThemePalette(
            "matrix",
            new BrandColors("\u001B[38;5;46m", "\u001B[38;5;40m", "\u001B[38;5;118m", "\u001B[38;5;120m", "\u001B[38;5;28m"),
            new StatusColors("\u001B[38;5;46m", "\u001B[38;5;154m", "\u001B[38;5;196m"),
            new SurfaceColors("\u001B[48;5;232m", "\u001B[48;5;233m", "\u001B[48;5;232m")
    );

    /** Built-in High-Contrast Accessibility ANSI palette (REQ-00099, FR-00047). */
    public static final TuiThemePalette HIGH_CONTRAST = new TuiThemePalette(
            "high-contrast",
            new BrandColors("\u001B[96m", "\u001B[95m", "\u001B[93m", "\u001B[97m", "\u001B[37m"),
            new StatusColors("\u001B[92m", "\u001B[93m", "\u001B[91m"),
            new SurfaceColors("\u001B[40m", "\u001B[40m", "\u001B[40m")
    );

    /** Immutable registry of all bundled themes (REQ-00099, FR-00047). */
    private static final List<TuiThemePalette> ALL_THEMES = List.of(
            CYBERPUNK, OPENCODE, DRACULA, NORD, MATRIX, HIGH_CONTRAST
    );

    /** Theme identifier (REQ-00099). */
    private final String name;
    /** Brand and typography foreground colors (FR-00047). */
    private final BrandColors brand;
    /** Status indicator foreground colors (FR-00047). */
    private final StatusColors status;
    /** Surface background colors (FR-00047). */
    private final SurfaceColors surface;

    /**
     * Constructs a semantic TuiThemePalette from grouped color token sets.
     *
     * <p>Traceability: REQ-00099, FR-00047, CS-0030.1.1.
     *
     * @param nameVal theme name, must not be null
     * @param brandVal brand foreground colors, must not be null
     * @param statusVal status foreground colors, must not be null
     * @param surfaceVal surface background colors, must not be null
     */
    public TuiThemePalette(final String nameVal,
                           final BrandColors brandVal,
                           final StatusColors statusVal,
                           final SurfaceColors surfaceVal) {
        // [REQ-00099, FR-00047, CS-0030.1.1] Validate non-null semantic token groups
        this.name = Objects.requireNonNull(nameVal, "name must not be null");
        this.brand = Objects.requireNonNull(brandVal, "brand must not be null");
        this.status = Objects.requireNonNull(statusVal, "status must not be null");
        this.surface = Objects.requireNonNull(surfaceVal, "surface must not be null");
    }

    /**
     * Resolves a theme palette by name, falling back to CYBERPUNK if unknown.
     *
     * <p>Traceability: REQ-00099, FR-00047, ADR-0057.
     *
     * @param themeName theme identifier, must not be null
     * @return matching TuiThemePalette
     */
    public static TuiThemePalette resolve(final String themeName) {
        // [REQ-00099, CS-0030.1.1] Normalize and validate theme lookup identifier
        final String nonNullName = Objects.requireNonNull(themeName, "themeName must not be null")
                .trim().toLowerCase(Locale.ROOT);
        // [FR-00047] Scan bundled XTerm-256 themes for exact match or fallback to CYBERPUNK
        for (final TuiThemePalette candidate : ALL_THEMES) {
            if (candidate.name.equals(nonNullName)) {
                return candidate;
            }
        }
        return CYBERPUNK;
    }

    /**
     * Cycles to the next theme in the bundled theme list.
     *
     * <p>Traceability: REQ-00099, FR-00047, ADR-0063.
     *
     * @param currentName current theme name, must not be null
     * @return next TuiThemePalette in rotation
     */
    public static TuiThemePalette nextTheme(final String currentName) {
        // [REQ-00099, CS-0030.1.1] Normalize current theme name before rotation
        final String nonNullName = Objects.requireNonNull(currentName, "currentName must not be null")
                .trim().toLowerCase(Locale.ROOT);
        // [FR-00047] Advance circularly to next registered XTerm-256 theme palette
        for (int i = 0; i < ALL_THEMES.size(); i++) {
            if (ALL_THEMES.get(i).name.equals(nonNullName)) {
                final int nextIndex = (i + 1) % ALL_THEMES.size();
                return ALL_THEMES.get(nextIndex);
            }
        }
        return OPENCODE;
    }

    /**
     * Returns an unmodifiable list of all built-in theme palettes.
     *
     * <p>Traceability: REQ-00099, FR-00047.
     *
     * @return list of available palettes
     */
    public static List<TuiThemePalette> availableThemes() {
        // [REQ-00099, FR-00047] Return immutable list of bundled palettes
        return ALL_THEMES;
    }

    /**
     * Returns the theme identifier name.
     *
     * @return theme name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the primary foreground color sequence.
     *
     * @return primary foreground ANSI code
     */
    public String getPrimaryFg() {
        return brand.primaryFg;
    }

    /**
     * Returns the secondary foreground color sequence.
     *
     * @return secondary foreground ANSI code
     */
    public String getSecondaryFg() {
        return brand.secondaryFg;
    }

    /**
     * Returns the accent foreground color sequence.
     *
     * @return accent foreground ANSI code
     */
    public String getAccentFg() {
        return brand.accentFg;
    }

    /**
     * Returns the primary body text foreground color sequence.
     *
     * @return text foreground ANSI code
     */
    public String getTextFg() {
        return brand.textFg;
    }

    /**
     * Returns the muted foreground color sequence.
     *
     * @return muted foreground ANSI code
     */
    public String getMutedFg() {
        return brand.mutedFg;
    }

    /**
     * Returns the success foreground color sequence.
     *
     * @return success foreground ANSI code
     */
    public String getSuccessFg() {
        return status.successFg;
    }

    /**
     * Returns the warning foreground color sequence.
     *
     * @return warning foreground ANSI code
     */
    public String getWarningFg() {
        return status.warningFg;
    }

    /**
     * Returns the error foreground color sequence.
     *
     * @return error foreground ANSI code
     */
    public String getErrorFg() {
        return status.errorFg;
    }

    /**
     * Returns the header bar background color sequence.
     *
     * @return header background ANSI code
     */
    public String getHeaderBg() {
        return surface.headerBg;
    }

    /**
     * Returns the panel/sidebar background color sequence.
     *
     * @return panel background ANSI code
     */
    public String getPanelBg() {
        return surface.panelBg;
    }

    /**
     * Returns the footer bar background color sequence.
     *
     * @return footer background ANSI code
     */
    public String getFooterBg() {
        return surface.footerBg;
    }

    @Override
    public boolean equals(final Object o) {
        // [CS-0030.11] Value equality based on canonical theme name
        if (this == o) {
            return true;
        }
        if (!(o instanceof TuiThemePalette)) {
            return false;
        }
        final TuiThemePalette that = (TuiThemePalette) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        // [CS-0030.11] Hash code consistent with equals
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        // [CS-0030.11] Diagnostic string representation
        return "TuiThemePalette{" + "name='" + name + '\'' + '}';
    }

    /**
     * Brand and text foreground ANSI sequences (REQ-00099, FR-00047).
     */
    public static final class BrandColors {
        /** Primary foreground. */
        private final String primaryFg;
        /** Secondary foreground. */
        private final String secondaryFg;
        /** Accent foreground. */
        private final String accentFg;
        /** Body text foreground. */
        private final String textFg;
        /** Muted text foreground. */
        private final String mutedFg;

        /**
         * Constructs BrandColors.
         *
         * @param primaryVal primary color, must not be null
         * @param secondaryVal secondary color, must not be null
         * @param accentVal accent color, must not be null
         * @param textVal text color, must not be null
         * @param mutedVal muted color, must not be null
         */
        public BrandColors(final String primaryVal,
                           final String secondaryVal,
                           final String accentVal,
                           final String textVal,
                           final String mutedVal) {
            // [REQ-00099, CS-0030.1.1] Validate non-null brand token sequences
            this.primaryFg = Objects.requireNonNull(primaryVal, "primaryVal must not be null");
            this.secondaryFg = Objects.requireNonNull(secondaryVal, "secondaryVal must not be null");
            this.accentFg = Objects.requireNonNull(accentVal, "accentVal must not be null");
            this.textFg = Objects.requireNonNull(textVal, "textVal must not be null");
            this.mutedFg = Objects.requireNonNull(mutedVal, "mutedVal must not be null");
        }
    }

    /**
     * Semantic status ANSI sequences (success, warning, error) (REQ-00099, FR-00047).
     */
    public static final class StatusColors {
        /** Success foreground. */
        private final String successFg;
        /** Warning foreground. */
        private final String warningFg;
        /** Error foreground. */
        private final String errorFg;

        /**
         * Constructs StatusColors.
         *
         * @param successVal success color, must not be null
         * @param warningVal warning color, must not be null
         * @param errorVal error color, must not be null
         */
        public StatusColors(final String successVal, final String warningVal, final String errorVal) {
            // [REQ-00099, CS-0030.1.1] Validate non-null status token sequences
            this.successFg = Objects.requireNonNull(successVal, "successVal must not be null");
            this.warningFg = Objects.requireNonNull(warningVal, "warningVal must not be null");
            this.errorFg = Objects.requireNonNull(errorVal, "errorVal must not be null");
        }
    }

    /**
     * Surface background ANSI sequences (REQ-00099, FR-00047).
     */
    public static final class SurfaceColors {
        /** Header background. */
        private final String headerBg;
        /** Panel background. */
        private final String panelBg;
        /** Footer background. */
        private final String footerBg;

        /**
         * Constructs SurfaceColors.
         *
         * @param headerVal header background, must not be null
         * @param panelVal panel background, must not be null
         * @param footerVal footer background, must not be null
         */
        public SurfaceColors(final String headerVal, final String panelVal, final String footerVal) {
            // [REQ-00099, CS-0030.1.1] Validate non-null surface background sequences
            this.headerBg = Objects.requireNonNull(headerVal, "headerVal must not be null");
            this.panelBg = Objects.requireNonNull(panelVal, "panelVal must not be null");
            this.footerBg = Objects.requireNonNull(footerVal, "footerVal must not be null");
        }
    }
}
