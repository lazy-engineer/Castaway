import Foundation
import shared
import FeedKit
import AVFoundation

extension Episode {
    func copy(playbackPosition: Int64) -> Episode {
        return self.doCopy(
            id: self.id,
            title: self.title,
            subTitle: self.subTitle,
            description: self.description,
            audioUrl: self.audioUrl,
            imageUrl: self.imageUrl,
            author: self.author,
            playbackPosition: PlaybackPosition(
                position: playbackPosition,
                duration: self.playbackPosition.duration,
                percentage: self.playbackPosition.percentage
            ),
            isPlaying: self.isPlaying,
            podcastUrl: self.podcastUrl)
    }
}

extension Episode {
    func copy(duration: KotlinLong) -> Episode {
        return self.doCopy(
            id: self.id,
            title: self.title,
            subTitle: self.subTitle,
            description: self.description,
            audioUrl: self.audioUrl,
            imageUrl: self.imageUrl,
            author: self.author,
            playbackPosition: PlaybackPosition(
                position: self.playbackPosition.position,
                duration: duration,
                percentage: self.playbackPosition.percentage
            ),
            isPlaying: self.isPlaying,
            podcastUrl: self.podcastUrl)
    }
}

extension Episode {
    func toMediaData() -> MediaData {
        return MediaData.init(
            mediaId: self.id,
            mediaUri: self.audioUrl,
            playbackPosition: self.playbackPosition.position,
            duration: playbackPosition.duration ?? 1
        )
    }
}

extension MediaData {
    func toAVPlayerItem() -> AVPlayerItem {
        return AVPlayerItem.init(url: URL.init(string: self.mediaUri)!)
    }
}

extension RSSFeed {
    func toFeedData(url: String) -> FeedData {
        return FeedData(
            url: url,
            title: self.title!,
            episodes: self.items!.map({ $0.toEpisode(url: url) }))
    }
}

extension RSSFeedItem {
    func toEpisode(url: String) -> Episode {
        return Episode(
            id: UUID.init().uuidString,
            title: self.title!,
            subTitle: self.iTunes?.iTunesSubtitle,
            description: self.description,
            audioUrl: (self.media?.mediaContents!.first!.attributes!.url)!,
            imageUrl: nil,
            author: self.author,
            playbackPosition: PlaybackPosition(position: 0, duration: nil, percentage: nil),
            isPlaying: false,
            podcastUrl: url)
    }
}

extension Dictionary where Value: Equatable {
    func allKeys(forValue val: Value) -> [Key] {
        return self.filter { $1 == val }.map { $0.0 }
    }
}
