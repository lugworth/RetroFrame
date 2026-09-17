package com.janoski.retroframe

data class FilmAdjustments(
    val exposure: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val fade: Float,
    val grain: Float,
    val vignette: Float,
    val intensity: Float,
)

enum class FilmPreset(
    val label: String,
    val shortLabel: String,
    val stockDescription: String,
    val base: FilmAdjustments,
    val swatches: List<Long>,
) {
    NATURAL(
        "Neutral", "NTRL", "Clean negative",
        FilmAdjustments(0f, 1f, 1f, 0f, 0f, 0f, 0f, 0f),
        listOf(0xFFE9E4D7, 0xFF8D8A7F, 0xFF4C504D),
    ),
    PORTRA_400(
        "Portra 400", "P400", "Soft skin · warm shadows",
        FilmAdjustments(0.05f, 0.92f, 0.88f, 0.20f, 0.10f, 0.18f, 0.10f, 0.85f),
        listOf(0xFFE5B79B, 0xFFAF907C, 0xFF5D5D56),
    ),
    GOLD_200(
        "Gold 200", "G200", "Golden · nostalgic",
        FilmAdjustments(0f, 1.08f, 1.12f, 0.28f, 0.04f, 0.20f, 0.12f, 0.88f),
        listOf(0xFFF0C87B, 0xFFD49758, 0xFF64705B),
    ),
    EKTAR_100(
        "Ektar 100", "E100", "Crisp · saturated",
        FilmAdjustments(-0.03f, 1.16f, 1.20f, 0.06f, 0f, 0.08f, 0.08f, 0.90f),
        listOf(0xFF80A59C, 0xFFB67147, 0xFFE1C97F),
    ),
    SUPERIA_400(
        "Superia 400", "S400", "Cool greens · flash pop",
        FilmAdjustments(-0.02f, 1.02f, 1.06f, -0.16f, 0.08f, 0.28f, 0.14f, 0.84f),
        listOf(0xFF7F9B7B, 0xFF748A9C, 0xFFD1B66F),
    ),
    HP5(
        "HP5 Plus", "HP5", "Grainy · timeless B&W",
        FilmAdjustments(0.02f, 1.14f, 0f, 0f, 0.02f, 0.34f, 0.16f, 0.96f),
        listOf(0xFFE8E6DF, 0xFF898983, 0xFF30302E),
    ),
    TRI_X(
        "Tri-X 400", "TX400", "Punchy · gritty B&W",
        FilmAdjustments(-0.04f, 1.26f, 0f, 0f, 0f, 0.52f, 0.20f, 0.98f),
        listOf(0xFFF2EEE4, 0xFF7D7B75, 0xFF252422),
    ),
    CINESTILL_800T(
        "800T", "800T", "Tungsten · halation mood",
        FilmAdjustments(0.08f, 0.94f, 1.02f, -0.32f, 0.16f, 0.26f, 0.22f, 0.90f),
        listOf(0xFF8DA7B9, 0xFFD6A36F, 0xFFB75D61),
    ),
}

fun FilmAdjustments.mixedWith(other: FilmAdjustments, t: Float): FilmAdjustments {
    fun mix(a: Float, b: Float) = a + (b - a) * t
    return FilmAdjustments(
        mix(exposure, other.exposure),
        mix(contrast, other.contrast),
        mix(saturation, other.saturation),
        mix(temperature, other.temperature),
        mix(fade, other.fade),
        mix(grain, other.grain),
        mix(vignette, other.vignette),
        mix(intensity, other.intensity),
    )
}
