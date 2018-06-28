Set oShell = CreateObject("Wscript.Shell")
Dim strArgs
strArgs = "cmd /c UTStarterStopper.bat"
oShell.run strArgs, 0, false