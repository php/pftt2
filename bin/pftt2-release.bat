@ECHO OFF

REM Check if build directory exists
cd ../
if exist build (
	REM Create pftt-release directory in main folder
	mkdir pftt-release

	REM Copy contents of bin, conf and lib to respective folders
	cd pftt-release
	mkdir bin
	mkdir conf
	mkdir lib
	cd ..

	xcopy /s /i "./bin" "./pftt-release/bin"
	xcopy /s /i "./conf" "./pftt-release/conf"
	xcopy /s /i "./lib" "./pftt-release/lib"

	REM Create pftt2.jar and place in lib of the package
	cd build
	jar cf pftt2.jar ./com/ ./org/columba ./org/kxml2 ./org/incava
	MOVE ./pftt2.jar ../pftt-release/lib
	cd ..

	REM Create zip file of pftt-release folder
	"./bin/7za.exe" a -tzip "./pftt-release.zip" "./pftt-release"

	REM Delete temp files/folders
	rd /s /q pftt-release
	cd ./bin
) else (
	ECHO Build folder does not exist
)