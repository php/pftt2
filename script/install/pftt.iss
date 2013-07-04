;http://www.jrsoftware.org/ishelp/
[Setup]
AppName=PFTT
AppVersion=0.5
DefaultDirName=C:\php-sdk\PFTT\current
; TODO additional metadata
DefaultGroupName=PFTT
;UninstallDisplayIcon={app}\MyProg.exe
Compression=lzma2
SolidCompression=yes
OutputDir=C:\php-sdk\PFTT\current\scripts\install\windows
DirExistsWarning=no
CreateAppDir=yes
; Require at least Windows 6.0 (Vista+)
MinVersion=6.0

[Files]
; http://stackoverflow.com/questions/10645269/inno-setup-exclude-a-directory-and-its-files-also
; TODO excludes doesn't work
; TODO exclude files in root of c:\php-sdk\pftt\current
Source: "C:\php-sdk\PFTT\current\*"; Excludes: ".git"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: "C:\php-sdk\PFTT\current\*"; Excludes: "src"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: "C:\php-sdk\PFTT\current\*"; Excludes: "cache\working"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: "C:\php-sdk\PFTT\current\*"; Excludes: ".settings"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs;
Source: "C:\php-sdk\PFTT\current\*"; Excludes: "scripts\install"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs;

; TODO don't include `jre` folder on non-windows
; TODO start menu entry

;[Icons]
;Name: "{group}\My Program"; Filename: "{app}\MyProg.exe"

;[Run]
;Filename: "{app}\bin\PFTT Shell.LNK"; Parameters: "/nowait"
