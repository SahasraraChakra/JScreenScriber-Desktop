# JScreenScriber

Screenplay writing app: Java Swing + FlatLaf, Fountain format, A4/Letter page view and print.


## Screens

### Light Mode
![Light Mode](screens/v6-lightMode.png)

### Dark Mode
![Dark Mode](screens/v6-darkMode.png)


## Build & run
Double-click `JScreenScriber.vbs` to launch without a console (uses the `javaw` on your PATH; `.jar` double-click depends on Windows' file association, which may point to an older Java).
If the jar does nothing when double-clicked, run `run.bat` (shows the console). Startup errors are also shown in a dialog and saved to `%USERPROFILE%\.jscreenscriber\error.log`.

Requires JDK 17+. Either run `build.bat` (Windows) / `./build.sh` (needs internet once, downloads FlatLaf) which writes `dist/jscreenscriber.jar`,
or open the folder as a Maven project in NetBeans and press Run.

All styling lives in FlatLaf theme files: `src/main/resources/com/jscreenscriber/themes/FlatLightLaf.properties` and `FlatDarkLaf.properties`.

    mvn package
    java -jar target/jscreenscriber-1.0.0.jar [file.fountain]

## Optional fonts (pixel-exact look)
Drop TTFs into `src/main/resources/fonts/`: `Inter-Regular/Medium/SemiBold/Bold.ttf`,
`CourierPrime-Regular/Bold/Italic/BoldItalic.ttf`. Without them it falls back to Segoe UI/Helvetica and Courier New.

## Layout
- `model/`  Paper (A4/Letter geometry), Block, ElementType, TitlePage, ScriptDocument (undo/redo), FountainIO (parser/writer), Layout (wrap + pagination)
- `ui/`     MainFrame, TitleBar, WindowChrome (drag/resize), PageView (paged editor), RulerPanel, SidePanel, StatusBar, CardsView,
            SettingsDialog, TitlePageDesigner, FindDialog, Printing, TitlePageRenderer
- `Theme`   light + dark palettes pushed into FlatLaf defaults; `Settings` persists to ~/.jscreenscriber/settings.properties

## Keys
Ctrl+1..7 element type, Enter smart next, Tab cycle on empty, Shift+Enter line break, Ctrl+B/I/U, Ctrl+F, Ctrl+P, F11 focus, Ctrl+K cards, Ctrl+Shift+D dark.
