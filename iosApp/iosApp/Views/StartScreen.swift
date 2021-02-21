import SwiftUI

struct StartScreen : View {
    
    @EnvironmentObject var viewModel: CastawayViewModel
    
    var body: some View {
        VStack {
            HStack {
                if let imageUrl = viewModel.feedImage {
                    Image(uiImage: imageUrl)
                        .resizable()
                        .scaledToFill()
                        .frame(width: 48, height: 48)
                        .cornerRadius(10)
                        .padding(8)
                } else {
                    Image(systemName: "mic")
                        .frame(width: 48, height: 48)
                        .background(Rectangle()
                                        .fill(Color.black)
                                        .cornerRadius(10))
                        .foregroundColor(.white)
                        .padding(8)
                        .onAppear {
                            guard let imageUrl = viewModel.currentEpisode?.imageUrl else { return }
                            viewModel.loadImage(imageUrl)
                        }
                }
                
                Text(viewModel.feedTitle)
                    .font(.title)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
                    .padding(.trailing, 8)
                Spacer()
            }
            
            EpisodeListView().environmentObject(viewModel)
        }
    }
}

#if DEBUG
struct StartScreen_Previews: PreviewProvider {
    static var previews: some View {
        StartScreen()
    }
}
#endif
