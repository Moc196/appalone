package com.example.data

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<Memory>> = memoryDao.getAllMemories()

    fun getMemoriesForDay(startOfDay: Long, endOfDay: Long): Flow<List<Memory>> {
        return memoryDao.getMemoriesForDay(startOfDay, endOfDay)
    }

    suspend fun getMemoryById(id: Int): Memory? {
        return memoryDao.getMemoryById(id)
    }

    suspend fun insert(memory: Memory): Long {
        return memoryDao.insertMemory(memory)
    }

    suspend fun delete(memory: Memory) {
        memoryDao.deleteMemory(memory)
    }
}
