package com.colombiaguard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ColombiaGuard - Professional Dark Palette
 *
 * Security app aesthetic: deep backgrounds, muted accents, high contrast text.
 * Inspired by Linear, Vercel, GitHub Dark.
 *
 * Rules:
 * - NO neon, NO gradients, NO pastel backgrounds
 * - Solid colors only
 * - High contrast for readability
 * - Muted tones for secondary elements
 */

// ── Background ──────────────────────────────────────────
val Background = Color(0xFF0D1117)       // Deep black
val BackgroundAlt = Color(0xFF010409)    // Slightly deeper

// ── Surface (cards, containers) ─────────────────────────
val Surface = Color(0xFF161B22)          // Card background
val SurfaceHover = Color(0xFF1C2128)     // Hover state
val SurfaceVariant = Color(0xFF21262D)   // Secondary containers
val SurfaceBorder = Color(0xFF30363D)    // Borders

// ── Primary (blue - trust, security) ────────────────────
val Primary = Color(0xFF58A6FF)          // Professional blue
val PrimaryDark = Color(0xFF388BFD)      // Pressed state
val PrimaryMuted = Color(0xFF1F6FEB)     // Muted blue
val OnPrimary = Color(0xFFFFFFFF)

// ── Secondary (lighter blue) ────────────────────────────
val Secondary = Color(0xFF79C0FF)
val OnSecondary = Color(0xFFFFFFFF)

// ── Tertiary (purple accent) ────────────────────────────
val Tertiary = Color(0xFFD2A8FF)
val OnTertiary = Color(0xFFFFFFFF)

// ── Semantic Colors ─────────────────────────────────────

// Success / Allowed (muted green)
val Success = Color(0xFF3FB950)
val SuccessMuted = Color(0xFF238636)
val SuccessBackground = Color(0xFF0D1D0D)

// Error / Blocked (muted red)
val Error = Color(0xFFF85149)
val ErrorMuted = Color(0xFFDA3633)
val ErrorBackground = Color(0xFF1D0D0D)

// Warning / Permissive (muted amber)
val Warning = Color(0xFFD29922)
val WarningMuted = Color(0xFF9E6A03)
val WarningBackground = Color(0xFF1D1A0D)

// ── Text ────────────────────────────────────────────────
val OnBackground = Color(0xFFF0F6FC)     // Primary text
val OnSurface = Color(0xFFE6EDF3)        // Card text
val OnSurfaceVariant = Color(0xFF8B949E) // Muted text
val TextTertiary = Color(0xFF6E7681)     // Disabled text

// ── Status Card Backgrounds (dark, not pastel) ──────────
val StatusStrict = Color(0xFF1C0D0D)     // Dark red tint
val StatusNormal = Color(0xFF0D1C0D)     // Dark green tint
val StatusPermissive = Color(0xFF1C1A0D) // Dark amber tint

// ── Status Card Icons ───────────────────────────────────
val IconStrict = Color(0xFFF85149)       // Red
val IconNormal = Color(0xFF3FB950)       // Green
val IconPermissive = Color(0xFFD29922)   // Amber
