@ECHO OFF
@ECHO.
@ECHO.
@ECHO. PHP Core
@ECHO.
@ECHO Run PHP Core PHPT tests against PHP CLI
@ECHO core_all php-5.5.0beta1-Win32-VC11-x86 php-test-pack-5.5.0beta1
@ECHO.
@ECHO Run PHP Core PHPT tests against PHP's builtin web server
@ECHO core_all -c builtin_web php-5.5.0beta1-Win32-VC11-x86 php-test-pack-5.5.0beta1
@ECHO.
@ECHO Run PHP Core PHPT tests against PHP's builtin web server, both TS and NTS builds
@ECHO core_all -c builtin_web php-5.5.0beta1-Win32-VC11-x86;php-5.5.0beta1-NTS-Win32-VC11-x86 php-test-pack-5.5.0beta1
@ECHO.
@ECHO Run PHP Core PHPT tests against Apache ModPHP
@ECHO core_all -c apache php-5.5.0beta1-Win32-VC11-x86 php-test-pack-5.5.0beta1
@ECHO.
@ECHO Again, with Opcache and disabling error popup messages
@ECHO core_all -c opcache,apache -disable_debug_prompt php-5.5.0beta1-Win32-VC11-x86 php-test-pack-5.5.0beta1
@ECHO.
@ECHO Test twice, once with Opcache and once without any code cache
@ECHO core_all -c opcache,no_code_cache,apache -disable_debug_prompt php-5.5.0beta1-Win32-VC11-x86 php-test-pack-5.5.0beta1
@ECHO.
@ECHO.
@ECHO  PHP Applications
@ECHO.
@ECHO Run Symfony's PhpUnit tests against PHP CLI
@ECHO app_all -c symfony php-5.5.0beta1-Win32-VC11-x86
@ECHO.
@ECHO Run Symfony's PhpUnit tests against Apache ModPHP, both with OpCache and without OpCache
@ECHO app_all -c symfony,apache,opcache,no_code_cache php-5.5.0beta1-Win32-VC11-x86
@ECHO.
@ECHO.
@ECHO  User Interface testing of Applications
@ECHO.
@ECHO Test Wordpress against Apache ModPHP, both with OpCache and without OpCache
@ECHO ui_all -c wordpress,apache,opcache,no_code_cache php-5.5.0beta1-Win32-VC11-x86
@ECHO.
@ECHO Get task and other configuration files for -c
@ECHO list_config
@ECHO.

