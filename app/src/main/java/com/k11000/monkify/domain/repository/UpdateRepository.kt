package com.k11000.monkify.domain.repository

import com.k11000.monkify.domain.model.AppUpdate

interface UpdateRepository {
    suspend fun checkForUpdate(): Result<AppUpdate>
}
