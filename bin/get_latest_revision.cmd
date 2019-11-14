set SNAP_JSON=php-%branch%.json
set SNAP_JSON_URL=https://windows.php.net/downloads/snaps/php-%branch%/%SNAP_JSON%
powershell download_files.ps1 !SNAP_JSON_URL! %PFTT_CACHE% !SNAP_JSON!

set "psCmd="add-type -As System.Web.Extensions;^
$JSON = new-object Web.Script.Serialization.JavaScriptSerializer;^
$JSON.DeserializeObject($input).revision_last_exported""

for /f %%I in ('^<"%PFTT_CACHE%\!SNAP_JSON!" powershell -noprofile %psCmd%') do set "revision=r%%I"
del %PFTT_CACHE%\!SNAP_JSON!