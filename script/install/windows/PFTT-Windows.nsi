Name "PFTT"

OutFile "\php-sdk\PFTT\PFTT-Windows-x86-VERSION.exe"

RequestExecutionLevel admin
XPStyle on

# meta-data
VIAddVersionKey /LANG=1033-English "ProductName" "PFTT"
VIAddVersionKey /LANG=1033-English "Comments" "Full Coverage test tool for the PHP Ecosystem"
VIAddVersionKey /LANG=1033-English "CompanyName" "Microsoft"
VIAddVersionKey /LANG=1033-English "LegalCopyright" "© 2013 Microsoft Open Source Technology Center"
VIAddVersionKey /LANG=1033-English "FileDescription" "Full Coverage test tool for the PHP Ecosystem"
VIAddVersionKey /LANG=1033-English "FileVersion" "1.2.3.4"
VIProductVersion "1.2.3.4"

# see http://nsis.sourceforge.net/Docs/Chapter4.html
ShowInstDetails show
ShowUnInstDetails show 
SetCompressor /SOLID lzma
SetCompressorDictSize 30
SetDateSave off
SetOverwrite on
AllowSkipFiles off

# ask user to accept the license
Page license
LicenseData \php-sdk\PFTT\current\LICENSE.txt

# value will be replaced in onInit
InstallDir ""

Function .onInit
	# check Windows version
	ReadRegStr $R4 HKLM "SOFTWARE\Microsoft\Windows NT\CurrentVersion" CurrentVersion
	# $R4 will be 6.0 6.1 6.2
	# check that it starts with 6
	StrCpy $R3 $R4 1 0
	StrCmp $R3 '6' windows_ok
	MessageBox MB_OK "Only Windows 6.x is supported (upgrade to Windows Vista+/7/8/2012/2008r2, etc...)"
	Quit
  
	windows_ok:
  

	# get %SYSTEMDRIVE%  (should be an NSIS constant like %WINDIR% but its not)
	ReadEnvStr $R5 SYSTEMDRIVE

	# InstallDir is a compile-time directive, but can replace it at run-time like so
	StrCpy $INSTDIR "$R5\php-sdk\PFTT\current"
FunctionEnd

# comment this line out: do NOT ask user to confirm install directory (go with the $INSTDIR value calculated in .onInit)
# Page directory

# ask user to confirm start menu entries
Page custom StartMenuGroupSelect "" ": Start Menu Folder"
Function StartMenuGroupSelect
	Push $R1

	StartMenu::Select /checknoshortcuts "Don't create a start menu folder" /autoadd /lastused $R0 "PFTT"
	Pop $R1

	StrCmp $R1 "success" success
	StrCmp $R1 "cancel" done
		; error
		MessageBox MB_OK $R1
		StrCpy $R0 "PFTT Install Cancelled" # use default
		Return
	success:
	Pop $R0

	done:
	Pop $R1
FunctionEnd

# install files and set registry entries
Page instfiles
# TODO 
Section
	SetOutPath $INSTDIR
	# TODO must manually delete script\install\Windows\*.exe
	# TODO must manually move conf\internal bin\internal cache\working\*
	# NOTE for some reason .git is skipped here already for some reason
	File /r /x *.git* /x *conf\internal* /x *bin\internal* /x *cache\working* /x *scripts\install* \php-sdk\PFTT\current\*
	
	WriteUninstaller $INSTDIR\uninstaller.exe
	
	# add entry to `Programs & Features` page in Control Panel
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PFTT" "DisplayName" "PFTT"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PFTT" "InstallLocation" "$INSTDIR"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PFTT" "Publisher" "Microsoft Open Source Technology Center"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PFTT" "UninstallString" "$INSTDIR\uninstaller.exe"
SectionEnd
Section
	# this part is only necessary if you used /checknoshortcuts
	StrCpy $R1 $R0 1
	StrCmp $R1 ">" skip
		# run this block if not /checknoshortcuts
		
		SetShellVarContext All
		CreateDirectory "$SMPROGRAMS\$R0"
		CreateShortCut "$SMPROGRAMS\$R0\PFTT Shell.lnk" "$INSTDIR\BIN\PFTT Shell.lnk"		
		CreateShortCut "$SMPROGRAMS\$R0\PFTT Documentation.lnk" "$INSTDIR\DOC"		
		CreateShortCut "$SMPROGRAMS\$R0\PHP on Windows.lnk" "http://windows.php.net/"
		CreateShortCut "$SMPROGRAMS\$R0\PHP Bugs.lnk" "http://bugs.php.net/"
		CreateShortCut "$SMPROGRAMS\$R0\PFTT Homepage.lnk" "http://github.com/OSTC/PFTT2/"
		CreateShortCut "$SMPROGRAMS\$R0\Uninstall PFTT.lnk" "$INSTDIR\uninstaller"
	skip:
SectionEnd

# prompt user to run the PFTT shell when installation is finished (so user gets started/engaged quickly)
Function .onInstSuccess
	MessageBox MB_YESNO "PFTT is installed.$\r$\n$\r$\nRun PFTT Shell?" IDNO NoPFTTShell
		ExecShell "open" "$INSTDIR\BIN\PFTT Shell.lnk" SW_SHOWMAXIMIZED
	NoPFTTShell:
FunctionEnd

# confirm uninstalling PFTT
UninstPage uninstConfirm

# delete files and registry key
UninstPage instfiles
Section "Uninstall"
	# remove from `Programs & Features` page in Control Panel
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PFTT"
	Delete $INSTDIR
SectionEnd
