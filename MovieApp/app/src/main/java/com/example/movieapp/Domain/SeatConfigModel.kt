package com.example.movieapp.Domain

// Khớp với "SeatConfig" trong db.json:
// { "normal": {"rows": [...], "seatsPerRow": 8, "price": 75000}, "vip": {...} }

data class SeatRowConfig(
    val rows: List<String> = emptyList(),
    val seatsPerRow: Int = 0,
    val price: Double = 0.0
)

data class SeatConfigModel(
    val normal: SeatRowConfig = SeatRowConfig(),
    val vip: SeatRowConfig = SeatRowConfig()
)