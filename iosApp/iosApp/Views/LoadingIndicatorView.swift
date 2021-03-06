import SwiftUI

struct LoadingIndicatorView : View {
    
    @EnvironmentObject var theme: ThemeNeumorphism
    
    @State private var dots: String = ""
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        Text("Loading \(dots)")
            .font(.title2).bold().foregroundColor(theme.colorPalette.textColor)
            .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: .infinity)
            .onReceive(timer, perform: { _ in
                if self.dots.count == 3 {
                    self.dots = ""
                } else {
                    self.dots += "."
                }
            })
    }
}

#if DEBUG
struct LoadingIndicatorView_Previews: PreviewProvider {
    static var previews: some View {
        LoadingIndicatorView()
    }
}
#endif
