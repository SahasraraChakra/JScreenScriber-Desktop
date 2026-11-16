' Double-click to start JScreenScriber with the same Java your command prompt uses (no console window).
Set fso = CreateObject("Scripting.FileSystemObject")
Set sh = CreateObject("WScript.Shell")
sh.CurrentDirectory = fso.GetParentFolderName(WScript.ScriptFullName)
If Not fso.FileExists("dist\jscreenscriber.jar") Then
  MsgBox "dist\jscreenscriber.jar not found. Run build.bat first.", 16, "JScreenScriber"
  WScript.Quit 1
End If
sh.Run "javaw --enable-native-access=ALL-UNNAMED -jar ""dist\jscreenscriber.jar""", 0, False
