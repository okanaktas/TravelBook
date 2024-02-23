package com.okanaktas.travelbook.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.okanaktas.travelbook.model.Place

@Database(entities = [Place::class], version = 1)
abstract class PlaceDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}