package io.github.lazyengineer.castaway.shared.database

import io.github.lazyengineer.castaway.shared.common.Result
import io.github.lazyengineer.castaway.shared.entity.Episode
import io.github.lazyengineer.castaway.shared.entity.FeedData
import io.github.lazyengineer.castaway.shared.entity.FeedInfo
import kotlinx.coroutines.flow.Flow

interface LocalFeedDataSource {

  suspend fun loadFeedInfo(feedUrl: String): Result<FeedInfo>
  suspend fun loadFeed(feedUrl: String): Result<FeedData>
  suspend fun loadEpisodes(episodeIds: List<String>): Result<List<Episode>>
  suspend fun saveFeedData(feed: FeedInfo, episodes: List<Episode>): Result<FeedData>
  suspend fun saveEpisode(episode: Episode): Result<Episode>
  fun episodeFlow(episodeIds: List<String>): Flow<List<Episode>>
  fun episodeFlow(podcastUrl: String): Flow<List<Episode>>
}
