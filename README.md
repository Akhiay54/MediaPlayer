# MediaPlayer

This Android application demonstrates advanced video streaming capabilities and API reverse engineering skills as part of the Teleparty Android Challenge.

### Task 1: DRM Video Player Implementation Process

#### 1. Architecture Setup
- **MVVM Pattern**: Implemented clean architecture with `VideoPlayerViewModel` and `VideoPlayerScreen`
- **State Management**: Used `StateFlow` for reactive UI updates
- **Lifecycle Management**: Added `DefaultLifecycleObserver` to prevent memory leaks during rotation

#### 2. ExoPlayer Integration
- **Version**: ExoPlayer 2.18.7 (resolved compatibility issues with DRM)
- **DRM Configuration**: Widevine DRM using `DefaultDrmSessionManager`
- **Media Source**: DASH streaming with `DashMediaSource.Factory`
- **Track Selection**: Custom `DefaultTrackSelector` for quality management

#### 3. Custom UI Implementation
- **Quality Selection**: Custom dropdown with parsed MPD manifest qualities
- **Fullscreen Mode**: Landscape orientation with hidden navigation tabs
- **Player Controls**: Custom controls integrated with ExoPlayer's `StyledPlayerView`
- **Material Design 3**: Clean, modern UI with proper theming


### Task 2: Instagram API Reverse Engineering Implementation Process

#### 1. API Analysis
- **Target**: Instagram's internal GraphQL API endpoint
- **Method**: Analyzed real network requests from Instagram web app
- **Authentication**: Extracted session cookies, CSRF tokens, and headers

#### 2. Real Implementation
- **Endpoint**: `https://www.instagram.com/graphql/query`
- **Headers**: Real User-Agent, Origin, Referer, and authentication headers
- **Payload**: GraphQL query with `doc_id=8845758582119845` and shortcode variables
- **Response Parsing**: JSON parsing to extract username, caption, likes, comments

#### 3. Technical Details
- **HTTP Method**: POST with `application/x-www-form-urlencoded`
- **Session Management**: Real Instagram session cookies
- **Error Handling**: Proper HTTP response code checking and exception handling
- **UI Integration**: Clean Compose UI with loading states

#### 4. Reverse Engineering Process
- **Network Inspection**: Used browser dev tools to capture real Instagram requests
- **Header Analysis**: Identified required authentication headers
- **Payload Structure**: Reverse-engineered GraphQL query format
- **Response Mapping**: Mapped JSON response to UI display format
