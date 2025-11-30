# GymSmartcardApp (Compose for Desktop)

This is a minimal Compose for Desktop prototype for managing gym smartcards.

Quick start (IntelliJ recommended):
1. Open the folder in IntelliJ: `File → Open...` → select `C:\Code\GymSmartcardApp`.
2. Set Project SDK to JDK 17 (File → Project Structure).
3. Create Gradle wrapper (if not present): open the Terminal in IntelliJ and run `gradle wrapper` (requires Gradle installed) or let IntelliJ offer to download Gradle.
4. Run the app with Gradle task `run` or create an Application run configuration with main class `app.MainKt`.

The project contains a fake smartcard service so the UI can be used before integrating a real reader.
