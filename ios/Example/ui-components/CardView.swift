//
//  CardView.swift
//  ui-components
//
//  Created by Long Pham on 18/6/25.
//

import SwiftUI

public struct CardView: View {
    let icon: String
    let iconColor: Color
    let borderColor: Color
    let title: String
    let description: String
    
    public init(icon: String, iconColor: Color = .accentColor, borderColor: Color = .accentColor, title: String, description: String) {
        self.icon = icon
        self.iconColor = iconColor
        self.borderColor = borderColor
        self.title = title
        self.description = description
    }
    
    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .renderingMode(.template)
                    .foregroundStyle(iconColor)
                    .accentColor(iconColor)
                    .font(.system(size: 24))
                    .foregroundColor(.blue)
                
                Text(title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.black)
                
                Spacer()
            }
            
            Text(description)
                .font(.system(size: 14))
                .foregroundColor(.gray)
                .lineLimit(nil)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .stroke(borderColor.opacity(0.3), lineWidth: 1)
                .background(Color.blue.opacity(0.05))
        )
        .padding(.horizontal, 20)
    }
}

#Preview {
    VStack {
        CardView(icon: "faceid", iconColor: .red, borderColor: .red, title: "Biometric Verification Required", description: "You will be asked to take a selfie to verify your liveness and identity. This process is secure and your biometric data stays on your device.")
        CardView(icon: "faceid", title: "Biometric Verification Required", description: "You will be asked to take a selfie to verify your liveness and identity. This process is secure and your biometric data stays on your device.")
        CardView(icon: "", title: "Biometric Verification Required", description: "You will be asked to take a selfie to verify your liveness and identity. This process is secure and your biometric data stays on your device.")
    }
}
