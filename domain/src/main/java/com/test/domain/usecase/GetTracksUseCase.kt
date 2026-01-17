package com.test.domain.usecase

import com.test.core.model.Track
import com.test.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTracksUseCase @Inject constructor(private val repository: TrackRepository) {
    operator fun invoke(): Flow<List<Track>> = repository.getTracks()
}
