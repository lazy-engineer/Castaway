package io.github.lazyengineer.castaway.shared.database

import io.github.lazyengineer.castaway.shared.common.Result
import io.github.lazyengineer.castaway.shared.entity.Episode
import io.github.lazyengineer.castaway.shared.entity.FeedData

interface LocalFeedDataSource {

    suspend fun fetchFeed(feedUrl: String): Result<FeedData>
    suspend fun saveFeedData(feed: FeedData): Result<FeedData>
    suspend fun saveEpisode(episode: Episode): Result<Episode>
}