#!/bin/bash

# Teleparty MediaPlayer Build and Test Script
echo "🎬 Teleparty MediaPlayer - Build and Test Script"
echo "================================================"

# Check if Android SDK is available
if ! command -v adb &> /dev/null; then
    echo "❌ Android SDK not found. Please install Android Studio and add SDK to PATH."
    exit 1
fi

echo "✅ Android SDK found"

# Clean and build the project
echo "🧹 Cleaning project..."
./gradlew clean

echo "🔨 Building project..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    # Check if device is connected
    DEVICE_COUNT=$(adb devices | grep -c "device$")
    if [ $DEVICE_COUNT -gt 0 ]; then
        echo "📱 Android device detected. Installing app..."
        ./gradlew installDebug
        
        if [ $? -eq 0 ]; then
            echo "✅ App installed successfully!"
            echo ""
            echo "🎯 Testing Instructions:"
            echo "1. Open the MediaPlayer app on your device"
            echo "2. Test Task 1 (DRM Video Player):"
            echo "   - Verify video loads and plays"
            echo "   - Test play/pause controls"
            echo "   - Test seek/scrub functionality"
            echo "   - Try different quality selections"
            echo "3. Test Task 2 (Metadata Viewer):"
            echo "   - Enter a YouTube video ID"
            echo "   - Tap 'Fetch Metadata'"
            echo "   - Verify metadata displays correctly"
            echo ""
            echo "📹 Don't forget to record a demo video as required!"
        else
            echo "❌ App installation failed"
        fi
    else
        echo "⚠️  No Android device connected. Please connect a device and try again."
        echo "💡 You can also install the APK manually from: app/build/outputs/apk/debug/"
    fi
else
    echo "❌ Build failed. Please check the error messages above."
fi

echo ""
echo "🏁 Script completed!"
