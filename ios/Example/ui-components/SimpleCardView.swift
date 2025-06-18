//
//  SimpleCardView.swift
//  ui-components
//
//  Created by Long Pham on 18/6/25.
//

import SwiftUI

public struct SimpleCardView: View {
    
    let icon: String
    let title: String
    let description: String
    let action: () -> Void
    @State private var isPressed = false
    
    init(icon: String, title: String, description: String, action: @escaping () -> Void, isPressed: Bool = false) {
        self.icon = icon
        self.title = title
        self.description = description
        self.action = action
        self.isPressed = isPressed
    }
    
    public var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(.green)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.black)
                
                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
            }
            
            Spacer()
        }
    }
}

#Preview {
    SimpleCardView(icon: "shield.pattern.checkered",
                   title: "Authenticate",
                   description: "Use your biometric credentials to securely log in to services",
                   action: {
                   }
    )
}
