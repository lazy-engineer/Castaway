package io.github.lazyengineer.castaway.androidApp.viewmodel

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.lazyengineer.castaway.androidApp.usecase.StoreAndGetFeedUseCase
import io.github.lazyengineer.castaway.androidApp.view.EpisodeRowState
import io.github.lazyengineer.castaway.androidApp.view.screen.NowPlayingEpisode
import io.github.lazyengineer.castaway.androidApp.view.screen.NowPlayingState
import io.github.lazyengineer.castaway.androidApp.view.screen.PodcastState
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.EpisodeRowEvent.Click
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.EpisodeRowEvent.Pause
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.EpisodeRowEvent.Play
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.ChangePlaybackSpeed
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.EditingPlayback
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.EditingPlaybackPosition
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.EpisodeClicked
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.FastForward
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.MediaItemClicked
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.Rewind
import io.github.lazyengineer.castaway.androidApp.viewmodel.UiEvent.NowPlayingEvent.SeekTo
import io.github.lazyengineer.castaway.shared.entity.Episode
import io.github.lazyengineer.castaway.shared.entity.FeedData
import io.github.lazyengineer.castaway.shared.entity.PlaybackPosition
import io.github.lazyengineer.castaway.shared.usecase.GetStoredFeedUseCase
import io.github.lazyengineer.castaway.shared.usecase.SaveEpisodeUseCase
import io.github.lazyengineer.castaway.shared.usecase.StoredEpisodeFlowableUseCase
import io.github.lazyengineer.castawayplayer.MediaServiceClient
import io.github.lazyengineer.castawayplayer.extention.isPlaying
import io.github.lazyengineer.castawayplayer.service.Constants.MEDIA_ROOT_ID
import io.github.lazyengineer.castawayplayer.source.MediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CastawayViewModel constructor(
  private val mediaServiceClient: MediaServiceClient,
  private val getStoredFeedUseCase: GetStoredFeedUseCase,
  private val saveEpisodeUseCase: SaveEpisodeUseCase,
  private val storedEpisodeFlowableUseCase: StoredEpisodeFlowableUseCase,
  private val storeAndGetFeedUseCase: StoreAndGetFeedUseCase,
) : ViewModel() {

  private val subscriptionCallback = object : SubscriptionCallback() {
	override fun onChildrenLoaded(
	  parentId: String,
	  children: MutableList<MediaItem>
	) {
	  super.onChildrenLoaded(parentId, children)
	  viewModelScope.launch {
		loadFeed(TEST_URL)
	  }
	}
  }

  private val _episodeRowState = MutableStateFlow<EpisodeRowState>(EpisodeRowState.Unplayed)
  val episodeRowState = _episodeRowState.asStateFlow()

  private val _nowPlayingState = MutableStateFlow<NowPlayingState>(NowPlayingState.Loading)
  val nowPlayingState = _nowPlayingState.asStateFlow()

  private val _podcastState = MutableStateFlow<PodcastState>(PodcastState.Loading)
  val podcastState = _podcastState.asStateFlow()

  private val _playbackEditing = MutableStateFlow(false)

  private val pendingEvents = MutableSharedFlow<UiEvent>()

  init {
	subscribeToMediaService()
	collectUiEvents()
	collectConnectionState()
	collectPlaybackState()
	collectPlaybackPositions()
	collectNowPlaying()
  }

  private fun collectUiEvents() {
	viewModelScope.launch {
	  pendingEvents.collect { uiEvent ->
		when (uiEvent) {
		  Click -> {
		  }
		  Pause -> {
		  }
		  Play -> {
		  }

		  NowPlayingEvent.Pause -> {
		  }
		  NowPlayingEvent.Play -> {
		  }
		  FastForward -> forwardCurrentItem()
		  Rewind -> replayCurrentItem()
		  ChangePlaybackSpeed -> changePlaybackSpeed()
		  is EditingPlayback -> editingPlayback(uiEvent.editing)
		  is EditingPlaybackPosition -> editingPlaybackPosition(uiEvent.position)
		  is SeekTo -> seekTo(uiEvent.positionMillis)
		  is MediaItemClicked -> mediaItemClicked(uiEvent.itemId)
		  is EpisodeClicked -> episodeClicked(uiEvent.item)
		}
	  }
	}
  }

  private fun collectConnectionState() {
	viewModelScope.launch {
	  mediaServiceClient.isConnected.collect { connected ->
		if (connected) {
		  loadFeed(TEST_URL)
		}
	  }
	}
  }

  private fun collectNowPlaying() {
	viewModelScope.launch {
	  mediaServiceClient.nowPlaying.collect { mediaData ->
		val feedEpisode = podcastState.value.feed?.episodes?.firstOrNull { episode ->
		  mediaData.mediaId == episode.id
		}

		feedEpisode?.let {
		  val nowPlayingEpisode = NowPlayingEpisode(
			id = it.id,
			title = it.title,
			subTitle = it.subTitle,
			audioUrl = it.audioUrl,
			imageUrl = it.imageUrl,
			author = it.author,
			playbackPosition = it.playbackPosition.position,
			playbackDuration = mediaData.duration ?: it.playbackPosition.duration,
			playbackSpeed = 1f,
		  )

		  val state = nowPlayingEpisode.playbackPlayingState(playbackPlayingState(nowPlayingEpisode.id))
		  _nowPlayingState.emit(state)
		}
	  }
	}
  }

  private fun collectPlaybackState() {
	viewModelScope.launch {
	  mediaServiceClient.playbackState.collect {
		handlePlaybackState(it)
	  }
	}
  }

  private fun handlePlaybackState(playbackState: PlaybackStateCompat) {
	playingOrPaused { nowPlayingEpisode ->
	  viewModelScope.launch {
		val playingState = nowPlayingEpisode.playbackPlayingState(playbackState.isPlaying)
		_nowPlayingState.emit(playingState)

		podcastState.value.feed?.episodes?.mapPlayingEpisodeToEpisode(nowPlayingEpisode)?.let { episode ->
		  storeEpisodeOnPausedOrStopped(episode, playbackState, episode.playbackPosition.duration)
		}
	  }
	}
  }

  private fun playingOrPaused(block: (NowPlayingEpisode) -> Unit) {
	when (val state = nowPlayingState.value) {
	  is NowPlayingState.Paused -> {
		block(state.episode)
	  }
	  is NowPlayingState.Playing -> {
		block(state.episode)
	  }
	}
  }

  private fun collectPlaybackPositions() {
	viewModelScope.launch {
	  mediaServiceClient.playbackPosition.collect { position ->
		if (_playbackEditing.value.not()) editingPlaybackPosition(position)
		updateCurrentEpisodePlaybackPosition()
	  }
	}
  }

  private fun NowPlayingEpisode.playbackPlayingState(playing: Boolean): NowPlayingState {
	return if (playing) {
	  NowPlayingState.Playing(this)
	} else {
	  NowPlayingState.Paused(this)
	}
  }

  private fun updateCurrentEpisodePlaybackPosition() {
	playingOrPaused { episode ->
	  val updatedEpisodes = podcastState.value.feed?.episodes?.map {
		if (it.id == episode.id) {
		  it.copy(playbackPosition = PlaybackPosition(position = episode.playbackPosition, duration = episode.playbackDuration))
		} else {
		  it
		}
	  } ?: run { emptyList() }

	  _podcastState.value.feed?.let {
		viewModelScope.launch {
		  _podcastState.emit(PodcastState.Loaded(it.copy(episodes = updatedEpisodes)))
		}
	  }
	}
  }

  private fun collect(feedUrl: String) {
	storedEpisodeFlowableUseCase(feedUrl).subscribe(viewModelScope,
	  onEach = {
		Log.d("MainViewModel", "onEach Episode 🍻")
	  },
	  onError = {},
	  onComplete = {}
	)
  }

  private fun fetchFeed() {
	viewModelScope.launch {
	  Log.d("CastawayViewModel", "Fetch: $TEST_URL 🌐")
	  fetchFeedFromUrl(TEST_URL)
	}
  }

  private suspend fun loadFeed(url: String) {
	withContext(Dispatchers.IO) {
	  getStoredFeedUseCase(url).subscribe(
		this,
		onSuccess = {
		  Log.d("CastawayViewModel", "Local ✅")
		  prepareMediaData(it.episodes)
		  viewModelScope.launch {
			_podcastState.emit(PodcastState.Loaded(FeedData(info = it.info, episodes = it.episodes)))
		  }
		},
		onError = {
		  Log.d("CastawayViewModel", "There is no stored Feed: $url ❌ $it 👉 💾 Download...")
		  fetchFeed()
		},
	  )
	}
  }

  private suspend fun fetchFeedFromUrl(url: String) {
	withContext(Dispatchers.IO) {
	  storeAndGetFeedUseCase(url).subscribe(
		this,
		onSuccess = {
		  Log.d("CastawayViewModel", "Fetched 💯")

		  prepareMediaData(it.episodes)
		},
		onError = {
		  Log.d("CastawayViewModel", "Error fetching: ❌ $it")
		},
	  )
	}
  }

  private suspend fun storeEpisodeOnPausedOrStopped(episode: Episode, playbackState: PlaybackStateCompat, duration: Long = 1) {
	if (playbackState.onPausedOrStopped()) {
	  val updatedEpisode = episode.copy(
		playbackPosition = PlaybackPosition(
		  position = playbackState.position,
		  duration = duration,
		)
	  )
	  storeEpisode(updatedEpisode)
	}
  }

  private fun PlaybackStateCompat.onPausedOrStopped() = (state == PlaybackStateCompat.STATE_PAUSED || state == PlaybackStateCompat.STATE_STOPPED)

  private suspend fun storeEpisode(episode: Episode) {
	withContext(Dispatchers.IO) {
	  saveEpisodeUseCase(episode).subscribe(
		this,
		onSuccess = {
		  Log.d("CastawayViewModel", "Stored: 💾 ${it.title}")
		},
		onError = {
		  Log.d("CastawayViewModel", "Error storing: ❌ $it")
		},
	  )
	}
  }

  private fun prepareMediaData(episodes: List<Episode>) {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.prepare {
		episodes.mapToMediaData()
	  }
	}
  }

  private fun List<Episode>.mapToMediaData() = this.map {
	MediaData(
	  mediaId = it.id,
	  mediaUri = it.audioUrl,
	  displayTitle = it.title,
	  displayIconUri = it.imageUrl,
	  displayDescription = it.description,
	  displaySubtitle = it.subTitle ?: "",
	  playbackPosition = it.playbackPosition.position,
	  duration = it.playbackPosition.duration,
	)
  }

  private fun subscribeToMediaService() {
	viewModelScope.launch {
	  mediaServiceClient.subscribe(MEDIA_ROOT_ID, subscriptionCallback)
	}
  }

  private fun mediaItemClicked(clickedItemId: String) {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.playMediaId(clickedItemId)
	}
  }

  private fun episodeClicked(clickedItem: Episode) {
	if (mediaServiceClient.isConnected.value) {
	  if (!playbackPlayingState(clickedItem.id)) {
		mediaServiceClient.playMediaId(clickedItem.id)
	  }
	}
  }

  private fun forwardCurrentItem() {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.fastForward()
	}
  }

  private fun replayCurrentItem() {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.rewind()
	}
  }

  private fun skipToPrevious() {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.skipToPrevious()
	}
  }

  private fun skipToNext() {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.skipToNext()
	}
  }

  private fun seekTo(positionMillis: Long) {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.seekTo(positionMillis)
	}
  }

  private fun playbackSpeed(speed: Float) {
	if (mediaServiceClient.isConnected.value) {
	  mediaServiceClient.speed(speed)
	}
  }

  private fun changePlaybackSpeed() {
	when (val state = nowPlayingState.value) {
	  is NowPlayingState.Playing -> {
		val playbackSpeed = nextSupportedPlaybackSpeed(state.episode.playbackSpeed)
		emitNowPlayingState(NowPlayingState.Playing(state.episode.copy(playbackSpeed = playbackSpeed)))
		playbackSpeed(playbackSpeed)
	  }
	  is NowPlayingState.Paused -> {
		val playbackSpeed = nextSupportedPlaybackSpeed(state.episode.playbackSpeed)
		emitNowPlayingState(NowPlayingState.Paused(state.episode.copy(playbackSpeed = playbackSpeed)))
		playbackSpeed(playbackSpeed)
	  }
	  NowPlayingState.Buffering -> TODO()
	  NowPlayingState.Loading -> TODO()
	  NowPlayingState.Played -> TODO()
	}
  }

  private fun nextSupportedPlaybackSpeed(currentPlaybackSpeed: Float): Float {

	val supportedSpeedRates = listOf(1.0f, 1.5f, 2.0f)
	val currentIndex = supportedSpeedRates.indexOf(currentPlaybackSpeed)

	var newIndex = 0
	if (supportedSpeedRates.size > currentIndex + 1) {
	  newIndex = currentIndex + 1
	}

	return supportedSpeedRates[newIndex]
  }

  private fun playbackPlayingState(mediaId: String): Boolean {
	val isActive = mediaId == mediaServiceClient.nowPlaying.value.mediaId
	val isPlaying = mediaServiceClient.playbackState.value.isPlaying
	return when {
	  !isActive -> false
	  isPlaying -> true
	  else -> false
	}
  }

  private fun editingPlayback(editing: Boolean) {
	_playbackEditing.value = editing
  }

  private fun editingPlaybackPosition(position: Long) {
	when (val state = nowPlayingState.value) {
	  is NowPlayingState.Playing -> {
		emitNowPlayingState(NowPlayingState.Playing(state.episode.copy(playbackPosition = position)))
	  }
	  is NowPlayingState.Paused -> {
		emitNowPlayingState(NowPlayingState.Paused(state.episode.copy(playbackPosition = position)))
	  }
	  NowPlayingState.Buffering -> {
	  } //TODO()
	  NowPlayingState.Loading -> {
	  } //TODO()
	  NowPlayingState.Played -> {
	  } //TODO()
	}
  }

  private fun emitNowPlayingState(state: NowPlayingState) {
	viewModelScope.launch {
	  _nowPlayingState.emit(state)
	}
  }

  fun submitEvent(uiEvent: UiEvent) {
	viewModelScope.launch {
	  pendingEvents.emit(uiEvent)
	}
  }

  override fun onCleared() {
	super.onCleared()
	mediaServiceClient.unsubscribe(MEDIA_ROOT_ID, subscriptionCallback)
  }

  private fun List<Episode>.mapPlayingEpisodeToEpisode(playingEpisode: NowPlayingEpisode) = this.first {
	playingEpisode.id == it.id
  }.joinToEpisode(playingEpisode)

  private fun Episode.joinToEpisode(playingEpisode: NowPlayingEpisode): Episode {
	return Episode(
	  id = id,
	  title = playingEpisode.title,
	  subTitle = playingEpisode.subTitle,
	  description = description,
	  audioUrl = playingEpisode.audioUrl,
	  imageUrl = playingEpisode.imageUrl,
	  author = playingEpisode.author,
	  playbackPosition = PlaybackPosition(playingEpisode.playbackPosition, playingEpisode.playbackDuration),
	  episode = episode,
	  podcastUrl = podcastUrl,
	)
  }

  companion object {

	const val TEST_URL = "https://atp.fm/rss"
  }
}

sealed class UiEvent {

  sealed class EpisodeRowEvent : UiEvent() {
	object Play : EpisodeRowEvent()
	object Pause : EpisodeRowEvent()
	object Click : EpisodeRowEvent()
  }

  sealed class NowPlayingEvent : UiEvent() {
	object Play : NowPlayingEvent()
	object Pause : NowPlayingEvent()
	object Rewind : NowPlayingEvent()
	object FastForward : NowPlayingEvent()
	object ChangePlaybackSpeed : NowPlayingEvent()
	data class EpisodeClicked(val item: Episode) : NowPlayingEvent()
	data class MediaItemClicked(val itemId: String) : NowPlayingEvent()
	data class SeekTo(val positionMillis: Long) : NowPlayingEvent()
	data class EditingPlaybackPosition(val position: Long) : NowPlayingEvent()
	data class EditingPlayback(val editing: Boolean) : NowPlayingEvent()
  }
}