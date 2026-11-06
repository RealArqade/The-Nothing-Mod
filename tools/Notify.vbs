' Usage: wscript tools\Notify.vbs "message" ["title"]   (shows a pop-up that closes itself after 60 seconds)
Set sh = CreateObject("WScript.Shell")
msg = WScript.Arguments(0)
title = "The Nothing Mod"
If WScript.Arguments.Count > 1 Then title = WScript.Arguments(1)
sh.Popup msg, 60, title, 64