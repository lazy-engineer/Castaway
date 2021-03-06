import shared
import FeedKit
import UIKit
import AVFoundation

extension Episode {
    func copy(image: UIImage) -> Episode {
        return doCopy(
            id: id,
            title: title,
            subTitle: subTitle,
            description: description_,
            audioUrl: audioUrl,
            imageUrl: imageUrl,
            author: author,
            playbackPosition: playbackPosition,
            episode: episode,
            podcastUrl: podcastUrl)
    }
}

extension Episode {
    func copy(position: Int64) -> Episode {
        return doCopy(
            id: id,
            title: title,
            subTitle: subTitle,
            description: description_,
            audioUrl: audioUrl,
            imageUrl: imageUrl,
            author: author,
            playbackPosition: PlaybackPosition(
                position: position,
                duration: playbackPosition.duration
            ),
            episode: episode,
            podcastUrl: podcastUrl)
    }
}

extension Episode {
    func copy(duration: Int64) -> Episode {
        return doCopy(
            id: id,
            title: title,
            subTitle: subTitle,
            description: description_,
            audioUrl: audioUrl,
            imageUrl: imageUrl,
            author: author,
            playbackPosition: PlaybackPosition(
                position: playbackPosition.position,
                duration: duration
            ),
            episode: episode,
            podcastUrl: podcastUrl)
    }
}

extension Episode {
    func copy(playbackPosition: PlaybackPosition) -> Episode {
        return doCopy(
            id: id,
            title: title,
            subTitle: subTitle,
            description: description_,
            audioUrl: audioUrl,
            imageUrl: imageUrl,
            author: author,
            playbackPosition: playbackPosition,
            episode: episode,
            podcastUrl: podcastUrl)
    }
}

extension Episode {
    func toMediaData() -> MediaData {
        return MediaData.init(
            mediaId: id,
            mediaUri: audioUrl,
            title: title,
            podcastTitle: "Accidental Tech Podcast",
            playbackPosition: playbackPosition.position,
            duration: playbackPosition.duration
        )
    }
}

extension MediaData {
    func toAVPlayerItem() -> AVPlayerItem {
        return AVPlayerItem.init(url: URL.init(string: mediaUri)!)
    }
}

extension RSSFeed {
    func toFeedData(url: String) -> FeedData {
        let feedImage = self.feedImage()
        
        return FeedData(
            info: FeedInfo(
                url: url,
                title: title!,
                imageUrl: feedImage
            ),
            episodes: items!.enumerated().compactMap({ $0.element.toEpisode(url: url, index: Int32($0.offset)) }))
    }
    
    private func feedImage() -> String? {
        var feedImage: String? = nil
        
        if let image = image?.url {
            feedImage = image
        } else if let iTunesFeedImage = iTunes?.iTunesImage?.attributes?.href {
            feedImage = iTunesFeedImage
        }
        
        return feedImage
    }
}

extension RSSFeedItem {
    func toEpisode(url: String, index: Int32) -> Episode? {
        guard let audioUrl = audioUrl() else { return nil }
        let episodeImage = self.episodeImage()
        
        return Episode(
            id: UUID.init().uuidString,
            title: title!,
            subTitle: iTunes?.iTunesSubtitle,
            description: description,
            audioUrl: audioUrl,
            imageUrl: episodeImage,
            author: author,
            playbackPosition: PlaybackPosition(position: 0, duration: 1),
            episode: index,
            podcastUrl: url)
    }
    
    private func audioUrl() -> String? {
        var audioUrl: String? = nil
        
        if let enclosureUrl = enclosure?.attributes?.url {
            audioUrl = enclosureUrl
        } else if let mediaUrl = media?.mediaContents?.first?.attributes?.url {
            audioUrl = mediaUrl
        }
        
        return audioUrl
    }
    
    private func episodeImage() -> String? {
        guard let iTunesImage = iTunes?.iTunesImage?.attributes?.href else { return nil }
        
        return iTunesImage
    }
}

extension Dictionary where Value: Equatable {
    func allKeys(forValue val: Value) -> [Key] {
        return self.filter { $1 == val }.map { $0.0 }
    }
}
