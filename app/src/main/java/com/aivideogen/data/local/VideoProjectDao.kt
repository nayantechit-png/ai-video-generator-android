package com.aivideogen.data.local

import androidx.room.*
import com.aivideogen.data.model.GenerationStatus
import com.aivideogen.data.model.VideoProject
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {

    @Query("SELECT * FROM video_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE status = :status ORDER BY createdAt DESC")
    fun getProjectsByStatus(status: GenerationStatus): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): VideoProject?

    @Query("SELECT * FROM video_projects WHERE status = 'COMPLETED' ORDER BY updatedAt DESC")
    fun getCompletedProjects(): Flow<List<VideoProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Update
    suspend fun updateProject(project: VideoProject)

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE video_projects SET status = :status, progress = :progress, updatedAt = :time WHERE id = :id")
    suspend fun updateProjectStatus(id: Long, status: GenerationStatus, progress: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE video_projects SET outputVideoPath = :videoPath, thumbnailPath = :thumbPath, status = 'COMPLETED', progress = 100, updatedAt = :time WHERE id = :id")
    suspend fun markCompleted(id: Long, videoPath: String, thumbPath: String?, time: Long = System.currentTimeMillis())

    @Query("UPDATE video_projects SET status = 'FAILED', errorMessage = :error, updatedAt = :time WHERE id = :id")
    suspend fun markFailed(id: Long, error: String, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM video_projects WHERE status = 'COMPLETED'")
    fun getCompletedCount(): Flow<Int>

    @Query("DELETE FROM video_projects WHERE status = 'FAILED'")
    suspend fun deleteAllFailed()
}
