package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caption: String,
    val mood: String, // e.g. "Peaceful", "Nostalgic", "Quiet", "Cozy", "Reflective"
    val timestamp: Long = System.currentTimeMillis(),
    val timeOfDay: String, // "Morning", "Noon", "Evening", "Night"
    val photoPath: String, // File URI path or preset asset name
    val weather: String = "Serene", // e.g. "Amber Sunbeams", "Fresh Air", "Peach Light", "Velvet Night"
    val location: String = "Private Space", // E.g., "Cozy Desk", "Window Seat"
    val filterApplied: String = "Nostalgia", // "Vintage Soft", "Warm Grain", "Lomo Glow", "Noir"
    val notes: String = ""
)
