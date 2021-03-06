package io.github.lazyengineer.castaway.androidApp.view.player

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.FastForward
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.PlayPause
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.PlaybackSpeed
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.PrepareData
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.Rewind
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.SeekTo
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.SkipToNext
import io.github.lazyengineer.castaway.androidApp.view.player.PlayerEvent.SkipToPrevious
import io.github.lazyengineer.castawayplayer.MediaServiceClient
import io.github.lazyengineer.castawayplayer.extention.isPlaying
import io.github.lazyengineer.castawayplayer.extention.isPrepared
import io.github.lazyengineer.castawayplayer.service.Constants
import io.github.lazyengineer.castawayplayer.source.MediaData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CastawayPlayer constructor(
  private val mediaServiceClient: MediaServiceClient
) {

  private lateinit var coroutineScope: CoroutineScope
  private val subscriptionCallback = object : SubscriptionCallback() {
	override fun onChildrenLoaded(
	  parentId: String,
	  children: MutableList<MediaItem>
	) {
	  super.onChildrenLoaded(parentId, children)
	}
  }

  private val playerEvents = MutableSharedFlow<PlayerEvent>()
  private val playbackSpeed = MutableStateFlow(1f)

  val playerState: Flow<PlayerState> = combine(
	mediaServiceClient.isConnected,
	mediaServiceClient.playbackPosition,
	mediaServiceClient.nowPlaying,
	mediaServiceClient.playbackState,
	playbackSpeed,
  ) { connected, position, nowPlaying, playbackState, playbackSpeed ->
	PlayerState(
	  connected = connected,
	  prepared = playbackState.isPrepared && (nowPlaying.duration != null && nowPlaying.duration != -1L),
	  mediaData = if (nowPlaying.mediaId.isNotEmpty()) nowPlaying.copy(playbackPosition = position) else null,
	  playing = playbackState.isPlaying,
	  playbackSpeed = playbackSpeed
	)
  }

  fun subscribe(scope: CoroutineScope) {
	coroutineScope = scope

	collectPlayerEvents()
	coroutineScope.launch {
	  mediaServiceClient.subscribe(Constants.MEDIA_ROOT_ID, subscriptionCallback)
	}
  }

  fun unsubscribe() {
	mediaServiceClient.unsubscribe(Constants.MEDIA_ROOT_ID, subscriptionCallback)
  }

  suspend fun dispatchPlayerEvent(playerEvent: PlayerEvent) {
	playerEvents.emit(playerEvent)
  }

  private fun collectPlayerEvents() {
	coroutineScope.launch {
	  playerEvents.collect { playerEvent ->
		when (playerEvent) {
		  is PrepareData -> prepareMediaData(playerEvent.data)
		  SkipToNext -> skipToNext()
		  SkipToPrevious -> skipToPrevious()
		  FastForward -> forwardCurrentItem()
		  Rewind -> replayCurrentItem()
		  is PlayPause -> playPause(playerEvent.itemId)
		  is SeekTo -> seekTo(playerEvent.positionMillis)
		  is PlaybackSpeed -> playbackSpeed(playerEvent.speed)
		}
	  }
	}
  }

  private fun prepareMediaData(data: List<MediaData>) {
	mediaServiceClient.prepare(data)
  }

  private fun playPause(clickedItemId: String) {
	mediaServiceClient.playMediaId(clickedItemId)
  }

  private fun forwardCurrentItem() {
	mediaServiceClient.fastForward()
  }

  private fun replayCurrentItem() {
	mediaServiceClient.rewind()
  }

  private fun skipToPrevious() {
	mediaServiceClient.skipToPrevious()
  }

  private fun skipToNext() {
	mediaServiceClient.skipToNext()
  }

  private fun seekTo(positionMillis: Long) {
	mediaServiceClient.seekTo(positionMillis)
  }

  private fun playbackSpeed(speed: Float) {
	mediaServiceClient.speed(speed)

	coroutineScope.launch {
	  playbackSpeed.emit(speed)
	}
  }
}
