package com.mostc.pftt.model.smoke;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** tests that a build has all required features
 * 
 * @author Matt Ficken
 *
 */

public class RequiredFeaturesSmokeTest extends SmokeTest {
	protected static String[] ts_parts, nts_parts;
	static {
		// from email conversation Feb 29-Mar 31, 2012 
		// (assumes default ini used, @see PhpIni)
		String required_nts_features_str = 
"phpinfo()%s" +
"PHP Version => %s"+
"%s"+
"System => %s" +
"Build Date => %s" +
"Compiler => %s" +
"Architecture => %s" +
"Configure Command => %s" +
"Server API => %s" +
"Virtual Directory Support => disabled%s" +
"Configuration File (php.ini) Path => %s" +
"Loaded Configuration File => (none)%s" +
"Scan this dir for additional .ini files => (none)%s" +
"Additional .ini files parsed => (none)%s" +
"PHP API => %s" +
"PHP Extension => %s" +
"Zend Extension => %s" +
"Zend Extension Build => %s" +
"PHP Extension Build => %s" +
"Debug Build => no%s" +
"Thread Safety => disabled%s" +
"Zend Signal Handling => disabled%s" +
"Zend Memory Manager => enabled%s" +
"Zend Multibyte Support => disabled%s" +
"IPv6 Support => enabled%s" +
"DTrace Support => disabled%s" +
"%s" +
"Registered PHP Streams => php, file, glob, data, http, ftp, zip, compress.zlib, phar%s" +
"Registered Stream Socket Transports => tcp, udp%s" +
"Registered Stream Filters => convert.iconv.*, mcrypt.*, mdecrypt.*, string.rot13, string.toupper, string.tolower, string.strip_tags, convert.*, consumed, dechunk, zlib.*%s" +
"%s" +
"This program makes use of the Zend Scripting Language Engine:%s" +
"Zend Engine v%s, Copyright (c) 1998-%s Zend Technologies%s" +
"%s"+
"Configuration%s" +
"%s" +
"bcmath%s" +
"%s" +
"BCMath support => enabled%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"bcmath.scale => 0 => 0%s" +
"%s" +
"calendar%s" +
"%s" +
"Calendar support => enabled%s" +
"%s" +
"Core%s" +
"%s" +
"PHP Version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"allow_url_fopen => On => On%s" +
"allow_url_include => Off => Off%s" +
"always_populate_raw_post_data => Off => Off%s" +
"arg_separator.input => & => &%s" +
"arg_separator.output => & => &%s" +
"asp_tags => Off => Off%s" +
"auto_append_file => no value => no value%s" +
"auto_globals_jit => On => On%s" +
"auto_prepend_file => no value => no value%s" +
"browscap => no value => no value%s" +
"default_charset => no value => no value%s" +
"default_mimetype => text/html => text/html%s" +
"disable_classes => no value => no value%s" +
"disable_functions => no value => no value%s" +
"display_errors => STDOUT => STDOUT%s" +
"display_startup_errors => Off => Off%s" +
"doc_root => no value => no value%s" +
"docref_ext => no value => no value%s" +
"docref_root => no value => no value%s" +
"enable_dl => On => On%s" +
"enable_post_data_reading => On => On%s" +
"error_append_string => no value => no value%s" +
"error_log => no value => no value%s" +
"error_prepend_string => no value => no value%s" +
"error_reporting => no value => no value%s" +
"exit_on_timeout => Off => Off%s" +
"expose_php => On => On%s" +
"extension_dir => %s" +
"file_uploads => On => On%s"+
"highlight.comment => <font style=\"color: #FF8000\">#FF8000</font> => <font style=\"color: #FF8000\">#FF8000</font>%s" +
"highlight.default => <font style=\"color: #0000BB\">#0000BB</font> => <font style=\"color: #0000BB\">#0000BB</font>%s" +
"highlight.html => <font style=\"color: #000000\">#000000</font> => <font style=\"color: #000000\">#000000</font>%s" +
"highlight.keyword => <font style=\"color: #007700\">#007700</font> => <font style=\"color: #007700\">#007700</font>%s" +
"highlight.string => <font style=\"color: #DD0000\">#DD0000</font> => <font style=\"color: #DD0000\">#DD0000</font>%s" +
"html_errors => Off => Off%s" +
"ignore_repeated_errors => Off => Off%s" +
"ignore_repeated_source => Off => Off%s" +
"ignore_user_abort => Off => Off%s" +
"implicit_flush => On => On%s" +
"include_path => %s" +
"log_errors => Off => Off%s" +
"log_errors_max_len => 1024 => 1024%s" +
"mail.add_x_header => Off => Off%s" +
"mail.force_extra_parameters => no value => no value%s" +
"mail.log => no value => no value%s" +
"max_execution_time => 0 => 0%s" +
"max_file_uploads => 20 => 20%s" +
"max_input_nesting_level => 64 => 64%s" +
"max_input_time => -1 => -1%s" +
"max_input_vars => 1000 => 1000%s" +
"memory_limit => 128M => 128M%s" +
"open_basedir => no value => no value%s" +
"output_buffering => 0 => 0%s" +
"output_handler => no value => no value%s" +
"post_max_size => 8M => 8M%s" +
"precision => 14 => 14%s" +
"realpath_cache_size => 16K => 16K%s" +
"realpath_cache_ttl => 120 => 120%s" +
"register_argc_argv => On => On%s" +
"report_memleaks => On => On%s" +
"report_zend_debug => Off => Off%s" +
"request_order => no value => no value%s" +
"sendmail_from => no value => no value%s" +
"sendmail_path => no value => no value%s" +
"serialize_precision => 17 => 17%s" +
"short_open_tag => On => On%s" +
"SMTP => %s" +
"smtp_port => 25 => 25%s" +
"sql.safe_mode => Off => Off%s" +
"track_errors => Off => Off%s" +
"unserialize_callback_func => no value => no value%s" +
"upload_max_filesize => 2M => 2M%s" +
"upload_tmp_dir => no value => no value%s" +
"user_dir => no value => no value%s" +
"user_ini.cache_ttl => 300 => 300%s" +
"user_ini.filename => .user.ini => .user.ini%s" +
"variables_order => EGPCS => EGPCS%s" +
"windows.show_crt_warning => Off => Off%s" +
"xmlrpc_error_number => 0 => 0%s" +
"xmlrpc_errors => Off => Off%s" +
"zend.detect_unicode => On => On%s" +
"zend.enable_gc => On => On%s" +
"zend.multibyte => Off => Off%s" +
"zend.script_encoding => no value => no value%s" +
"%s" +
"ctype%s" +
"%s" +
"ctype functions => enabled%s" +
"%s" +
"date%s" +
"%s" +
"date/time support => enabled%s" +
"\"Olson\" Timezone Database Version => %s" +
"Timezone Database => internal%s" +
"Default timezone => UTC%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"date.default_latitude => 31.7667 => 31.7667%s" +
"date.default_longitude => 35.2333 => 35.2333%s" +
"date.sunrise_zenith => 90.583333 => 90.583333%s" +
"date.sunset_zenith => 90.583333 => 90.583333%s" +
"date.timezone => no value => no value%s" +
"%s" +
"dom" +
"%s" +
"DOM/XML => enabled%s" +
"DOM/XML API Version => %s" +
"libxml Version => %s" +
"HTML Support => enabled%s" +
"XPath Support => enabled%s" +
"XPointer Support => enabled%s" +
"Schema Support => enabled%s" +
"RelaxNG Support => enabled%s" +
"%s" +
"ereg%s" +
"%s" +
"Regex Library => Bundled library enabled%s" +
"%s" +
"filter%s" +
"%s" +
"Input Validation and Filtering => enabled%s" +
"Revision => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"filter.default => unsafe_raw => unsafe_raw%s" +
"filter.default_flags => no value => no value%s" +
"%s" +
"ftp%s" +
"%s" +
"FTP support => enabled%s" +
"%s" +
"hash%s" +
"%s" +
"hash support => enabled%s" +
"Hashing Engines => md2 md4 md5 sha1 sha224 sha256 sha384 sha512 ripemd128 ripemd160 ripemd256 ripemd320 whirlpool tiger128,3 tiger160,3 tiger192,3 tiger128,4 tiger160,4 tiger192,4 snefru snefru256 gost adler32 crc32 crc32b fnv132 fnv164 joaat haval128,3 haval160,3 haval192,3 haval224,3 haval256,3 haval128,4 haval160,4 haval192,4 haval224,4 haval256,4 haval128,5 haval160,5 haval192,5 haval224,5 haval256,5%s" + 
"%s" +
"iconv%s" +
"%s" +
"iconv support => enabled%s" +
"iconv implementation => \"libiconv\"%s" +
"iconv library version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"iconv.input_encoding => ISO-8859-1 => ISO-8859-1%s" +
"iconv.internal_encoding => ISO-8859-1 => ISO-8859-1%s" +
"iconv.output_encoding => ISO-8859-1 => ISO-8859-1%s" +
"%s" +
"json%s" +
"%s" +
"json support => enabled%s" +
"json version => %s" +
"%s" +
"libxml%s" +
"%s" +
"libXML support => active%s" +
"libXML Compiled Version => %s" +
"libXML Loaded Version => %s" +
"libXML streams => enabled%s" +
"%s" +
"mcrypt%s" +
"%s" +
"mcrypt support => enabled%s" +
"mcrypt_filter support => enabled%s" +
"Version => %s" +
"Api No => %s" +
"Supported ciphers => cast-128 gost rijndael-128 twofish cast-256 loki97 rijndael-192 saferplus wake blowfish-compat des rijndael-256 serpent xtea blowfish enigma rc2 tripledes arcfour%s" + 
"Supported modes => cbc cfb ctr ecb ncfb nofb ofb stream%s" + 
"%s" +
"Directive => Local Value => Master Value%s" +
"mcrypt.algorithms_dir => no value => no value%s" +
"mcrypt.modes_dir => no value => no value%s" +
"%s" +
"mhash%s" +
"%s" +
"MHASH support => Enabled%s" +
"MHASH API Version => Emulated Support%s" +
"%s" +
"mysqlnd%s" +
"%s" +
"mysqlnd => enabled%s" +
"Version => mysqlnd %s" +
"Compression => supported%s" +
"SSL => supported%s" +
"Command buffer size => 4096%s" +
"Read buffer size => 32768%s" +
"Read timeout => 31536000%s" +
"Collecting statistics => Yes%s" +
"Collecting memory statistics => No%s" +
"Tracing => n/a%s" +
"Loaded plugins => mysqlnd,example,debug_trace,auth_plugin_mysql_native_password,auth_plugin_mysql_clear_password%s" +
"API Extensions => %s" +
"%s" +
"mysqlnd statistics =>%s" +  
"bytes_sent => 0%s" +
"bytes_received => 0%s" +
"packets_sent => 0%s" +
"packets_received => 0%s" +
"protocol_overhead_in => 0%s" +
"protocol_overhead_out => 0%s" +
"bytes_received_ok_packet => 0%s" +
"bytes_received_eof_packet => 0%s" +
"bytes_received_rset_header_packet => 0%s" +
"bytes_received_rset_field_meta_packet => 0%s" +
"bytes_received_rset_row_packet => 0%s" +
"bytes_received_prepare_response_packet => 0%s" +
"bytes_received_change_user_packet => 0%s" +
"packets_sent_command => 0%s" +
"packets_received_ok => 0%s" +
"packets_received_eof => 0%s" +
"packets_received_rset_header => 0%s" +
"packets_received_rset_field_meta => 0%s" +
"packets_received_rset_row => 0%s" +
"packets_received_prepare_response => 0%s" +
"packets_received_change_user => 0%s" +
"result_set_queries => 0%s" +
"non_result_set_queries => 0%s" +
"no_index_used => 0%s" +
"bad_index_used => 0%s" +
"slow_queries => 0%s" +
"buffered_sets => 0%s" +
"unbuffered_sets => 0%s" +
"ps_buffered_sets => 0%s" +
"ps_unbuffered_sets => 0%s" +
"flushed_normal_sets => 0%s" +
"flushed_ps_sets => 0%s" +
"ps_prepared_never_executed => 0%s" +
"ps_prepared_once_executed => 0%s" +
"rows_fetched_from_server_normal => 0%s" +
"rows_fetched_from_server_ps => 0%s" +
"rows_buffered_from_client_normal => 0%s" +
"rows_buffered_from_client_ps => 0%s" +
"rows_fetched_from_client_normal_buffered => 0%s" +
"rows_fetched_from_client_normal_unbuffered => 0%s" +
"rows_fetched_from_client_ps_buffered => 0%s" +
"rows_fetched_from_client_ps_unbuffered => 0%s" +
"rows_fetched_from_client_ps_cursor => 0%s" +
"rows_affected_normal => 0%s" +
"rows_affected_ps => 0%s" +
"rows_skipped_normal => 0%s" +
"rows_skipped_ps => 0%s" +
"copy_on_write_saved => 0%s" +
"copy_on_write_performed => 0%s" +
"command_buffer_too_small => 0%s" +
"connect_success => 0%s" +
"connect_failure => 0%s" +
"connection_reused => 0%s" +
"reconnect => 0%s" +
"pconnect_success => 0%s" +
"active_connections => 0%s" +
"active_persistent_connections => 0%s" +
"explicit_close => 0%s" +
"implicit_close => 0%s" +
"disconnect_close => 0%s" +
"in_middle_of_command_close => 0%s" +
"explicit_free_result => 0%s" +
"implicit_free_result => 0%s" +
"explicit_stmt_close => 0%s" +
"implicit_stmt_close => 0%s" +
"mem_emalloc_count => 0%s" +
"mem_emalloc_amount => 0%s" +
"mem_ecalloc_count => 0%s" +
"mem_ecalloc_amount => 0%s" +
"mem_erealloc_count => 0%s" +
"mem_erealloc_amount => 0%s" +
"mem_efree_count => 0%s" +
"mem_efree_amount => 0%s" +
"mem_malloc_count => 0%s" +
"mem_malloc_amount => 0%s" +
"mem_calloc_count => 0%s" +
"mem_calloc_amount => 0%s" +
"mem_realloc_count => 0%s" +
"mem_realloc_amount => 0%s" +
"mem_free_count => 0%s" +
"mem_free_amount => 0%s" +
"mem_estrndup_count => 0%s" +
"mem_strndup_count => 0%s" +
"mem_estndup_count => 0%s" +
"mem_strdup_count => 0%s" +
"proto_text_fetched_null => 0%s" +
"proto_text_fetched_bit => 0%s" +
"proto_text_fetched_tinyint => 0%s" +
"proto_text_fetched_short => 0%s" +
"proto_text_fetched_int24 => 0%s" +
"proto_text_fetched_int => 0%s" +
"proto_text_fetched_bigint => 0%s" +
"proto_text_fetched_decimal => 0%s" +
"proto_text_fetched_float => 0%s" +
"proto_text_fetched_double => 0%s" +
"proto_text_fetched_date => 0%s" +
"proto_text_fetched_year => 0%s" +
"proto_text_fetched_time => 0%s" +
"proto_text_fetched_datetime => 0%s" +
"proto_text_fetched_timestamp => 0%s" +
"proto_text_fetched_string => 0%s" +
"proto_text_fetched_blob => 0%s" +
"proto_text_fetched_enum => 0%s" +
"proto_text_fetched_set => 0%s" +
"proto_text_fetched_geometry => 0%s" +
"proto_text_fetched_other => 0%s" +
"proto_binary_fetched_null => 0%s" +
"proto_binary_fetched_bit => 0%s" +
"proto_binary_fetched_tinyint => 0%s" +
"proto_binary_fetched_short => 0%s" +
"proto_binary_fetched_int24 => 0%s" +
"proto_binary_fetched_int => 0%s" +
"proto_binary_fetched_bigint => 0%s" +
"proto_binary_fetched_decimal => 0%s" +
"proto_binary_fetched_float => 0%s" +
"proto_binary_fetched_double => 0%s" +
"proto_binary_fetched_date => 0%s" +
"proto_binary_fetched_year => 0%s" +
"proto_binary_fetched_time => 0%s" +
"proto_binary_fetched_datetime => 0%s" +
"proto_binary_fetched_timestamp => 0%s" +
"proto_binary_fetched_string => 0%s" +
"proto_binary_fetched_blob => 0%s" +
"proto_binary_fetched_enum => 0%s" +
"proto_binary_fetched_set => 0%s" +
"proto_binary_fetched_geometry => 0%s" +
"proto_binary_fetched_other => 0%s" +
"init_command_executed_count => 0%s" +
"init_command_failed_count => 0%s" +
"com_quit => 0%s" +
"com_init_db => 0%s" +
"com_query => 0%s" +
"com_field_list => 0%s" +
"com_create_db => 0%s" +
"com_drop_db => 0%s" +
"com_refresh => 0%s" +
"com_shutdown => 0%s" +
"com_statistics => 0%s" +
"com_process_info => 0%s" +
"com_connect => 0%s" +
"com_process_kill => 0%s" +
"com_debug => 0%s" +
"com_ping => 0%s" +
"com_time => 0%s" +
"com_delayed_insert => 0%s" +
"com_change_user => 0%s" +
"com_binlog_dump => 0%s" +
"com_table_dump => 0%s" +
"com_connect_out => 0%s" +
"com_register_slave => 0%s" +
"com_stmt_prepare => 0%s" +
"com_stmt_execute => 0%s" +
"com_stmt_send_long_data => 0%s" +
"com_stmt_close => 0%s" +
"com_stmt_reset => 0%s" +
"com_stmt_set_option => 0%s" +
"com_stmt_fetch => 0%s" +
"com_deamon => 0%s" +
"bytes_received_real_data_normal => 0%s" +
"bytes_received_real_data_ps => 0%s" +
"%s" +
"example statistics =>  %s" +
"stat1 => 0%s" +
"stat2 => 0%s" +
"%s" +
"odbc%s" +
"%s" +
"ODBC Support => enabled%s" +
"Active Persistent Links => 0%s" +
"Active Links => 0%s" +
"ODBC library => Win32%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"odbc.allow_persistent => On => On%s" +
"odbc.check_persistent => On => On%s" +
"odbc.default_cursortype => Static cursor => Static cursor%s" +
"odbc.default_db => no value => no value%s" +
"odbc.default_pw => no value => no value%s" +
"odbc.default_user => no value => no value%s" +
"odbc.defaultbinmode => return as is => return as is%s" +
"odbc.defaultlrl => return up to 4096 bytes => return up to 4096 bytes%s" +
"odbc.max_links => Unlimited => Unlimited%s" +
"odbc.max_persistent => Unlimited => Unlimited%s" +
"%s" +
"pcre%s" +
"%s" +
"PCRE (Perl Compatible Regular Expressions) Support => enabled%s" +
"PCRE Library Version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"pcre.backtrack_limit => 1000000 => 1000000%s" +
"pcre.recursion_limit => 100000 => 100000%s" +
"%s" +
"PDO%s" +
"%s" +
"PDO support => enabled%s" +
"PDO drivers => %s" +
"%s" +
"Phar%s" +
"%s" +
"Phar: PHP Archive support => enabled%s" +
"Phar EXT version => %s" +
"Phar API version => %s" +
"SVN revision => %s" +
"Phar-based phar archives => enabled%s" +
"Tar-based phar archives => enabled%s" +
"ZIP-based phar archives => enabled%s" +
"gzip compression => enabled%s" +
"bzip2 compression => disabled (install pecl/bz2)%s" +
"OpenSSL support => disabled (install ext/openssl)%s" +
"%s" +
"Phar based on pear/PHP_Archive, original concept by Davey Shafik.%s" +
"Phar fully realized by Gregory Beaver and Marcus Boerger.%s" +
"Portions of tar implementation Copyright (c) 2003-%s Tim Kientzle.%s" +
"Directive => Local Value => Master Value%s" +
"phar.cache_list => no value => no value%s" +
"phar.readonly => On => On%s" +
"phar.require_hash => On => On%s" +
"%s" +
"Reflection%s" +
"%s" +
"Reflection => enabled%s" +
"Version => %s" +
"%s" +
"session%s" +
"%s" +
"Session Support => enabled%s" +
"Registered save handlers => files user %s" +
"Registered serializer handlers => php php_binary wddx%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"session.auto_start => Off => Off%s" +
"session.cache_expire => 180 => 180%s" +
"session.cache_limiter => nocache => nocache%s" +
"session.cookie_domain => no value => no value%s" +
"session.cookie_httponly => Off => Off%s" +
"session.cookie_lifetime => 0 => 0%s" +
"session.cookie_path => / => /%s" +
"session.cookie_secure => Off => Off%s" +
"session.entropy_file => no value => no value%s" +
"session.entropy_length => 0 => 0%s" +
"session.gc_divisor => 100 => 100%s" +
"session.gc_maxlifetime => 1440 => 1440%s" +
"session.gc_probability => 1 => 1%s" +
"session.hash_bits_per_character => 4 => 4%s" +
"session.hash_function => 0 => 0%s" +
"session.name => PHPSESSID => PHPSESSID%s" +
"session.referer_check => no value => no value%s" +
"session.save_handler => files => files%s" +
"session.save_path => no value => no value%s" +
"session.serialize_handler => php => php%s" +
"session.upload_progress.cleanup => On => On%s" +
"session.upload_progress.enabled => On => On%s" +
"session.upload_progress.freq => 1% => 1%%s" +
"session.upload_progress.min_freq => 1 => 1%s" +
"session.upload_progress.name => PHP_SESSION_UPLOAD_PROGRESS => PHP_SESSION_UPLOAD_PROGRESS%s" +
"session.upload_progress.prefix => upload_progress_ => upload_progress_%s" +
"session.use_cookies => On => On%s" +
"session.use_only_cookies => On => On%s" +
"session.use_trans_sid => 0 => 0%s" +
"%s" +
"SimpleXML%s" +
"%s" +
"Simplexml support => enabled%s" +
"Revision => %s" +
"Schema support => enabled%s" +
"%s" +
"SPL%s" +
"%s" +
"SPL support => enabled%s" +
"Interfaces => Countable, OuterIterator, RecursiveIterator, SeekableIterator, SplObserver, SplSubject%s" +
"Classes => AppendIterator, ArrayIterator, ArrayObject, BadFunctionCallException, BadMethodCallException, CachingIterator, CallbackFilterIterator, DirectoryIterator, DomainException, EmptyIterator, FilesystemIterator, FilterIterator, GlobIterator, InfiniteIterator, InvalidArgumentException, IteratorIterator, LengthException, LimitIterator, LogicException, MultipleIterator, NoRewindIterator, OutOfBoundsException, OutOfRangeException, OverflowException, ParentIterator, RangeException, RecursiveArrayIterator, RecursiveCachingIterator, RecursiveCallbackFilterIterator, RecursiveDirectoryIterator, RecursiveFilterIterator, RecursiveIteratorIterator, RecursiveRegexIterator, RecursiveTreeIterator, RegexIterator, RuntimeException, SplDoublyLinkedList, SplFileInfo, SplFileObject, SplFixedArray, SplHeap, SplMinHeap, SplMaxHeap, SplObjectStorage, SplPriorityQueue, SplQueue, SplStack, SplTempFileObject, UnderflowException, UnexpectedValueException%s" +
"%s" +
"standard%s" +
"%s" +
"Dynamic Library Support => enabled%s"+
"Internal Sendmail Support for Windows => enabled%s"+
"%s"+
"Directive => Local Value => Master Value%s"+
"assert.active => 1 => 1%s"+
"assert.bail => 0 => 0%s"+
"assert.callback => no value => no value%s" +
"assert.quiet_eval => 0 => 0%s" +
"assert.warning => 1 => 1%s" +
"auto_detect_line_endings => 0 => 0%s" +
"default_socket_timeout => 60 => 60%s" +
"from => no value => no value%s" +
"url_rewriter.tags => a=href,area=href,frame=src,form=,fieldset= => a=href,area=href,frame=src,form=,fieldset=%s"+
"user_agent => no value => no value%s"+
"%s"+
"tokenizer%s"+
"%s"+
"Tokenizer Support => enabled%s"+
"%s"+
"wddx%s"+
"%s"+
"WDDX Support => enabled%s"+
"WDDX Session Serializer => enabled%s" +
"%s"+
"xml%s" +
"%s" +
"XML Support => active%s" +
"XML Namespace Support => active%s" +
"libxml2 Version => %s" +
"%s" +
"xmlreader%s" +
"%s" +
"XMLReader => enabled%s" +
"%s" +
"xmlwriter%s" +
"%s" +
"XMLWriter => enabled%s" +
"%s" +
"zip%s" +
"%s" +
"Zip => enabled%s" +
"Extension Version => %s" +
"Zip version => %s" +
"Libzip version => %s" +
"%s" +
"zlib%s" +
"%s" +
"ZLib Support => enabled%s" +
"Stream Wrapper => compress.zlib://%s" +
"Stream Filter => zlib.inflate, zlib.deflate%s" +
"Compiled Version => %s" +
"Linked Version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"zlib.output_compression => Off => Off%s" +
"zlib.output_compression_level => -1 => -1%s" +
"zlib.output_handler => no value => no value%s" +
"%s" +
"Additional Modules" +
"%s" +
"Module Name" +
"%s" +
"Environment" +
"%s" +
"PHP Variables" +
"%s" +
"Variable => Value" +
"%s" +
"PHP License" +
"%s";
		// currently, NTS and TS differ only in `Thread Safety` and `Virtual Directory Support`
		String required_ts_features_str = 
"phpinfo()%s" +
"PHP Version => %s"+
"%s"+
"System => %s" +
"Build Date => %s" +
"Compiler => %s" +
"Architecture => %s" +
"Configure Command => %s" +
"Server API => %s" +
"Virtual Directory Support => enabled%s" +
"Configuration File (php.ini) Path => %s" +
"Loaded Configuration File => (none)%s" +
"Scan this dir for additional .ini files => (none)%s" +
"Additional .ini files parsed => (none)%s" +
"PHP API => %s" +
"PHP Extension => %s" +
"Zend Extension => %s" +
"Zend Extension Build => %s" +
"PHP Extension Build => %s" +
"Debug Build => no%s" +
"Thread Safety => enabled%s" +
"Zend Signal Handling => disabled%s" +
"Zend Memory Manager => enabled%s" +
"Zend Multibyte Support => disabled%s" +
"IPv6 Support => enabled%s" +
"DTrace Support => disabled%s" +
"%s" +
"Registered PHP Streams => php, file, glob, data, http, ftp, zip, compress.zlib, phar%s" +
"Registered Stream Socket Transports => tcp, udp%s" +
"Registered Stream Filters => convert.iconv.*, mcrypt.*, mdecrypt.*, string.rot13, string.toupper, string.tolower, string.strip_tags, convert.*, consumed, dechunk, zlib.*%s" +
"%s" +
"This program makes use of the Zend Scripting Language Engine:%s" +
"Zend Engine v%s, Copyright (c) 1998-%s Zend Technologies%s" +
"%s"+
"Configuration%s" +
"%s" +
"bcmath%s" +
"%s" +
"BCMath support => enabled%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"bcmath.scale => 0 => 0%s" +
"%s" +
"calendar%s" +
"%s" +
"Calendar support => enabled%s" +
"%s" +
"Core%s" +
"%s" +
"PHP Version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"allow_url_fopen => On => On%s" +
"allow_url_include => Off => Off%s" +
"always_populate_raw_post_data => Off => Off%s" +
"arg_separator.input => & => &%s" +
"arg_separator.output => & => &%s" +
"asp_tags => Off => Off%s" +
"auto_append_file => no value => no value%s" +
"auto_globals_jit => On => On%s" +
"auto_prepend_file => no value => no value%s" +
"browscap => no value => no value%s" +
"default_charset => no value => no value%s" +
"default_mimetype => text/html => text/html%s" +
"disable_classes => no value => no value%s" +
"disable_functions => no value => no value%s" +
"display_errors => STDOUT => STDOUT%s" +
"display_startup_errors => Off => Off%s" +
"doc_root => no value => no value%s" +
"docref_ext => no value => no value%s" +
"docref_root => no value => no value%s" +
"enable_dl => On => On%s" +
"enable_post_data_reading => On => On%s" +
"error_append_string => no value => no value%s" +
"error_log => no value => no value%s" +
"error_prepend_string => no value => no value%s" +
"error_reporting => no value => no value%s" +
"exit_on_timeout => Off => Off%s" +
"expose_php => On => On%s" +
"extension_dir => %s" +
"file_uploads => On => On%s"+
"highlight.comment => <font style=\"color: #FF8000\">#FF8000</font> => <font style=\"color: #FF8000\">#FF8000</font>%s" +
"highlight.default => <font style=\"color: #0000BB\">#0000BB</font> => <font style=\"color: #0000BB\">#0000BB</font>%s" +
"highlight.html => <font style=\"color: #000000\">#000000</font> => <font style=\"color: #000000\">#000000</font>%s" +
"highlight.keyword => <font style=\"color: #007700\">#007700</font> => <font style=\"color: #007700\">#007700</font>%s" +
"highlight.string => <font style=\"color: #DD0000\">#DD0000</font> => <font style=\"color: #DD0000\">#DD0000</font>%s" +
"html_errors => Off => Off%s" +
"ignore_repeated_errors => Off => Off%s" +
"ignore_repeated_source => Off => Off%s" +
"ignore_user_abort => Off => Off%s" +
"implicit_flush => On => On%s" +
"include_path => %s" +
"log_errors => Off => Off%s" +
"log_errors_max_len => 1024 => 1024%s" +
"mail.add_x_header => Off => Off%s" +
"mail.force_extra_parameters => no value => no value%s" +
"mail.log => no value => no value%s" +
"max_execution_time => 0 => 0%s" +
"max_file_uploads => 20 => 20%s" +
"max_input_nesting_level => 64 => 64%s" +
"max_input_time => -1 => -1%s" +
"max_input_vars => 1000 => 1000%s" +
"memory_limit => 128M => 128M%s" +
"open_basedir => no value => no value%s" +
"output_buffering => 0 => 0%s" +
"output_handler => no value => no value%s" +
"post_max_size => 8M => 8M%s" +
"precision => 14 => 14%s" +
"realpath_cache_size => 16K => 16K%s" +
"realpath_cache_ttl => 120 => 120%s" +
"register_argc_argv => On => On%s" +
"report_memleaks => On => On%s" +
"report_zend_debug => Off => Off%s" +
"request_order => no value => no value%s" +
"sendmail_from => no value => no value%s" +
"sendmail_path => no value => no value%s" +
"serialize_precision => 17 => 17%s" +
"short_open_tag => On => On%s" +
"SMTP => %s" +
"smtp_port => 25 => 25%s" +
"sql.safe_mode => Off => Off%s" +
"track_errors => Off => Off%s" +
"unserialize_callback_func => no value => no value%s" +
"upload_max_filesize => 2M => 2M%s" +
"upload_tmp_dir => no value => no value%s" +
"user_dir => no value => no value%s" +
"user_ini.cache_ttl => 300 => 300%s" +
"user_ini.filename => .user.ini => .user.ini%s" +
"variables_order => EGPCS => EGPCS%s" +
"windows.show_crt_warning => Off => Off%s" +
"xmlrpc_error_number => 0 => 0%s" +
"xmlrpc_errors => Off => Off%s" +
"zend.detect_unicode => On => On%s" +
"zend.enable_gc => On => On%s" +
"zend.multibyte => Off => Off%s" +
"zend.script_encoding => no value => no value%s" +
"%s" +
"ctype%s" +
"%s" +
"ctype functions => enabled%s" +
"%s" +
"date%s" +
"%s" +
"date/time support => enabled%s" +
"\"Olson\" Timezone Database Version => %s" +
"Timezone Database => internal%s" +
"Default timezone => UTC%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"date.default_latitude => 31.7667 => 31.7667%s" +
"date.default_longitude => 35.2333 => 35.2333%s" +
"date.sunrise_zenith => 90.583333 => 90.583333%s" +
"date.sunset_zenith => 90.583333 => 90.583333%s" +
"date.timezone => no value => no value%s" +
"%s" +
"dom" +
"%s" +
"DOM/XML => enabled%s" +
"DOM/XML API Version => %s" +
"libxml Version => %s" +
"HTML Support => enabled%s" +
"XPath Support => enabled%s" +
"XPointer Support => enabled%s" +
"Schema Support => enabled%s" +
"RelaxNG Support => enabled%s" +
"%s" +
"ereg%s" +
"%s" +
"Regex Library => Bundled library enabled%s" +
"%s" +
"filter%s" +
"%s" +
"Input Validation and Filtering => enabled%s" +
"Revision => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"filter.default => unsafe_raw => unsafe_raw%s" +
"filter.default_flags => no value => no value%s" +
"%s" +
"ftp%s" +
"%s" +
"FTP support => enabled%s" +
"%s" +
"hash%s" +
"%s" +
"hash support => enabled%s" +
"Hashing Engines => md2 md4 md5 sha1 sha224 sha256 sha384 sha512 ripemd128 ripemd160 ripemd256 ripemd320 whirlpool tiger128,3 tiger160,3 tiger192,3 tiger128,4 tiger160,4 tiger192,4 snefru snefru256 gost adler32 crc32 crc32b fnv132 fnv164 joaat haval128,3 haval160,3 haval192,3 haval224,3 haval256,3 haval128,4 haval160,4 haval192,4 haval224,4 haval256,4 haval128,5 haval160,5 haval192,5 haval224,5 haval256,5%s" + 
"%s" +
"iconv%s" +
"%s" +
"iconv support => enabled%s" +
"iconv implementation => \"libiconv\"%s" +
"iconv library version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"iconv.input_encoding => ISO-8859-1 => ISO-8859-1%s" +
"iconv.internal_encoding => ISO-8859-1 => ISO-8859-1%s" +
"iconv.output_encoding => ISO-8859-1 => ISO-8859-1%s" +
"%s" +
"json%s" +
"%s" +
"json support => enabled%s" +
"json version => %s" +
"%s" +
"libxml%s" +
"%s" +
"libXML support => active%s" +
"libXML Compiled Version => %s" +
"libXML Loaded Version => %s" +
"libXML streams => enabled%s" +
"%s" +
"mcrypt%s" +
"%s" +
"mcrypt support => enabled%s" +
"mcrypt_filter support => enabled%s" +
"Version => %s" +
"Api No => %s" +
"Supported ciphers => cast-128 gost rijndael-128 twofish cast-256 loki97 rijndael-192 saferplus wake blowfish-compat des rijndael-256 serpent xtea blowfish enigma rc2 tripledes arcfour%s" + 
"Supported modes => cbc cfb ctr ecb ncfb nofb ofb stream%s" + 
"%s" +
"Directive => Local Value => Master Value%s" +
"mcrypt.algorithms_dir => no value => no value%s" +
"mcrypt.modes_dir => no value => no value%s" +
"%s" +
"mhash%s" +
"%s" +
"MHASH support => Enabled%s" +
"MHASH API Version => Emulated Support%s" +
"%s" +
"mysqlnd%s" +
"%s" +
"mysqlnd => enabled%s" +
"Version => mysqlnd %s" +
"Compression => supported%s" +
"SSL => supported%s" +
"Command buffer size => 4096%s" +
"Read buffer size => 32768%s" +
"Read timeout => 31536000%s" +
"Collecting statistics => Yes%s" +
"Collecting memory statistics => No%s" +
"Tracing => n/a%s" +
"Loaded plugins => mysqlnd,example,debug_trace,auth_plugin_mysql_native_password,auth_plugin_mysql_clear_password%s" +
"API Extensions => %s" +
"%s" +
"mysqlnd statistics =>%s" +  
"bytes_sent => 0%s" +
"bytes_received => 0%s" +
"packets_sent => 0%s" +
"packets_received => 0%s" +
"protocol_overhead_in => 0%s" +
"protocol_overhead_out => 0%s" +
"bytes_received_ok_packet => 0%s" +
"bytes_received_eof_packet => 0%s" +
"bytes_received_rset_header_packet => 0%s" +
"bytes_received_rset_field_meta_packet => 0%s" +
"bytes_received_rset_row_packet => 0%s" +
"bytes_received_prepare_response_packet => 0%s" +
"bytes_received_change_user_packet => 0%s" +
"packets_sent_command => 0%s" +
"packets_received_ok => 0%s" +
"packets_received_eof => 0%s" +
"packets_received_rset_header => 0%s" +
"packets_received_rset_field_meta => 0%s" +
"packets_received_rset_row => 0%s" +
"packets_received_prepare_response => 0%s" +
"packets_received_change_user => 0%s" +
"result_set_queries => 0%s" +
"non_result_set_queries => 0%s" +
"no_index_used => 0%s" +
"bad_index_used => 0%s" +
"slow_queries => 0%s" +
"buffered_sets => 0%s" +
"unbuffered_sets => 0%s" +
"ps_buffered_sets => 0%s" +
"ps_unbuffered_sets => 0%s" +
"flushed_normal_sets => 0%s" +
"flushed_ps_sets => 0%s" +
"ps_prepared_never_executed => 0%s" +
"ps_prepared_once_executed => 0%s" +
"rows_fetched_from_server_normal => 0%s" +
"rows_fetched_from_server_ps => 0%s" +
"rows_buffered_from_client_normal => 0%s" +
"rows_buffered_from_client_ps => 0%s" +
"rows_fetched_from_client_normal_buffered => 0%s" +
"rows_fetched_from_client_normal_unbuffered => 0%s" +
"rows_fetched_from_client_ps_buffered => 0%s" +
"rows_fetched_from_client_ps_unbuffered => 0%s" +
"rows_fetched_from_client_ps_cursor => 0%s" +
"rows_affected_normal => 0%s" +
"rows_affected_ps => 0%s" +
"rows_skipped_normal => 0%s" +
"rows_skipped_ps => 0%s" +
"copy_on_write_saved => 0%s" +
"copy_on_write_performed => 0%s" +
"command_buffer_too_small => 0%s" +
"connect_success => 0%s" +
"connect_failure => 0%s" +
"connection_reused => 0%s" +
"reconnect => 0%s" +
"pconnect_success => 0%s" +
"active_connections => 0%s" +
"active_persistent_connections => 0%s" +
"explicit_close => 0%s" +
"implicit_close => 0%s" +
"disconnect_close => 0%s" +
"in_middle_of_command_close => 0%s" +
"explicit_free_result => 0%s" +
"implicit_free_result => 0%s" +
"explicit_stmt_close => 0%s" +
"implicit_stmt_close => 0%s" +
"mem_emalloc_count => 0%s" +
"mem_emalloc_amount => 0%s" +
"mem_ecalloc_count => 0%s" +
"mem_ecalloc_amount => 0%s" +
"mem_erealloc_count => 0%s" +
"mem_erealloc_amount => 0%s" +
"mem_efree_count => 0%s" +
"mem_efree_amount => 0%s" +
"mem_malloc_count => 0%s" +
"mem_malloc_amount => 0%s" +
"mem_calloc_count => 0%s" +
"mem_calloc_amount => 0%s" +
"mem_realloc_count => 0%s" +
"mem_realloc_amount => 0%s" +
"mem_free_count => 0%s" +
"mem_free_amount => 0%s" +
"mem_estrndup_count => 0%s" +
"mem_strndup_count => 0%s" +
"mem_estndup_count => 0%s" +
"mem_strdup_count => 0%s" +
"proto_text_fetched_null => 0%s" +
"proto_text_fetched_bit => 0%s" +
"proto_text_fetched_tinyint => 0%s" +
"proto_text_fetched_short => 0%s" +
"proto_text_fetched_int24 => 0%s" +
"proto_text_fetched_int => 0%s" +
"proto_text_fetched_bigint => 0%s" +
"proto_text_fetched_decimal => 0%s" +
"proto_text_fetched_float => 0%s" +
"proto_text_fetched_double => 0%s" +
"proto_text_fetched_date => 0%s" +
"proto_text_fetched_year => 0%s" +
"proto_text_fetched_time => 0%s" +
"proto_text_fetched_datetime => 0%s" +
"proto_text_fetched_timestamp => 0%s" +
"proto_text_fetched_string => 0%s" +
"proto_text_fetched_blob => 0%s" +
"proto_text_fetched_enum => 0%s" +
"proto_text_fetched_set => 0%s" +
"proto_text_fetched_geometry => 0%s" +
"proto_text_fetched_other => 0%s" +
"proto_binary_fetched_null => 0%s" +
"proto_binary_fetched_bit => 0%s" +
"proto_binary_fetched_tinyint => 0%s" +
"proto_binary_fetched_short => 0%s" +
"proto_binary_fetched_int24 => 0%s" +
"proto_binary_fetched_int => 0%s" +
"proto_binary_fetched_bigint => 0%s" +
"proto_binary_fetched_decimal => 0%s" +
"proto_binary_fetched_float => 0%s" +
"proto_binary_fetched_double => 0%s" +
"proto_binary_fetched_date => 0%s" +
"proto_binary_fetched_year => 0%s" +
"proto_binary_fetched_time => 0%s" +
"proto_binary_fetched_datetime => 0%s" +
"proto_binary_fetched_timestamp => 0%s" +
"proto_binary_fetched_string => 0%s" +
"proto_binary_fetched_blob => 0%s" +
"proto_binary_fetched_enum => 0%s" +
"proto_binary_fetched_set => 0%s" +
"proto_binary_fetched_geometry => 0%s" +
"proto_binary_fetched_other => 0%s" +
"init_command_executed_count => 0%s" +
"init_command_failed_count => 0%s" +
"com_quit => 0%s" +
"com_init_db => 0%s" +
"com_query => 0%s" +
"com_field_list => 0%s" +
"com_create_db => 0%s" +
"com_drop_db => 0%s" +
"com_refresh => 0%s" +
"com_shutdown => 0%s" +
"com_statistics => 0%s" +
"com_process_info => 0%s" +
"com_connect => 0%s" +
"com_process_kill => 0%s" +
"com_debug => 0%s" +
"com_ping => 0%s" +
"com_time => 0%s" +
"com_delayed_insert => 0%s" +
"com_change_user => 0%s" +
"com_binlog_dump => 0%s" +
"com_table_dump => 0%s" +
"com_connect_out => 0%s" +
"com_register_slave => 0%s" +
"com_stmt_prepare => 0%s" +
"com_stmt_execute => 0%s" +
"com_stmt_send_long_data => 0%s" +
"com_stmt_close => 0%s" +
"com_stmt_reset => 0%s" +
"com_stmt_set_option => 0%s" +
"com_stmt_fetch => 0%s" +
"com_deamon => 0%s" +
"bytes_received_real_data_normal => 0%s" +
"bytes_received_real_data_ps => 0%s" +
"%s" +
"example statistics =>  %s" +
"stat1 => 0%s" +
"stat2 => 0%s" +
"%s" +
"odbc%s" +
"%s" +
"ODBC Support => enabled%s" +
"Active Persistent Links => 0%s" +
"Active Links => 0%s" +
"ODBC library => Win32%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"odbc.allow_persistent => On => On%s" +
"odbc.check_persistent => On => On%s" +
"odbc.default_cursortype => Static cursor => Static cursor%s" +
"odbc.default_db => no value => no value%s" +
"odbc.default_pw => no value => no value%s" +
"odbc.default_user => no value => no value%s" +
"odbc.defaultbinmode => return as is => return as is%s" +
"odbc.defaultlrl => return up to 4096 bytes => return up to 4096 bytes%s" +
"odbc.max_links => Unlimited => Unlimited%s" +
"odbc.max_persistent => Unlimited => Unlimited%s" +
"%s" +
"pcre%s" +
"%s" +
"PCRE (Perl Compatible Regular Expressions) Support => enabled%s" +
"PCRE Library Version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"pcre.backtrack_limit => 1000000 => 1000000%s" +
"pcre.recursion_limit => 100000 => 100000%s" +
"%s" +
"PDO%s" +
"%s" +
"PDO support => enabled%s" +
"PDO drivers => %s" +
"%s" +
"Phar%s" +
"%s" +
"Phar: PHP Archive support => enabled%s" +
"Phar EXT version => %s" +
"Phar API version => %s" +
"SVN revision => %s" +
"Phar-based phar archives => enabled%s" +
"Tar-based phar archives => enabled%s" +
"ZIP-based phar archives => enabled%s" +
"gzip compression => enabled%s" +
"bzip2 compression => disabled (install pecl/bz2)%s" +
"OpenSSL support => disabled (install ext/openssl)%s" +
"%s" +
"Phar based on pear/PHP_Archive, original concept by Davey Shafik.%s" +
"Phar fully realized by Gregory Beaver and Marcus Boerger.%s" +
"Portions of tar implementation Copyright (c) 2003-%s Tim Kientzle.%s" +
"Directive => Local Value => Master Value%s" +
"phar.cache_list => no value => no value%s" +
"phar.readonly => On => On%s" +
"phar.require_hash => On => On%s" +
"%s" +
"Reflection%s" +
"%s" +
"Reflection => enabled%s" +
"Version => %s" +
"%s" +
"session%s" +
"%s" +
"Session Support => enabled%s" +
"Registered save handlers => files user %s" +
"Registered serializer handlers => php php_binary wddx%s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"session.auto_start => Off => Off%s" +
"session.cache_expire => 180 => 180%s" +
"session.cache_limiter => nocache => nocache%s" +
"session.cookie_domain => no value => no value%s" +
"session.cookie_httponly => Off => Off%s" +
"session.cookie_lifetime => 0 => 0%s" +
"session.cookie_path => / => /%s" +
"session.cookie_secure => Off => Off%s" +
"session.entropy_file => no value => no value%s" +
"session.entropy_length => 0 => 0%s" +
"session.gc_divisor => 100 => 100%s" +
"session.gc_maxlifetime => 1440 => 1440%s" +
"session.gc_probability => 1 => 1%s" +
"session.hash_bits_per_character => 4 => 4%s" +
"session.hash_function => 0 => 0%s" +
"session.name => PHPSESSID => PHPSESSID%s" +
"session.referer_check => no value => no value%s" +
"session.save_handler => files => files%s" +
"session.save_path => no value => no value%s" +
"session.serialize_handler => php => php%s" +
"session.upload_progress.cleanup => On => On%s" +
"session.upload_progress.enabled => On => On%s" +
"session.upload_progress.freq => 1% => 1%%s" +
"session.upload_progress.min_freq => 1 => 1%s" +
"session.upload_progress.name => PHP_SESSION_UPLOAD_PROGRESS => PHP_SESSION_UPLOAD_PROGRESS%s" +
"session.upload_progress.prefix => upload_progress_ => upload_progress_%s" +
"session.use_cookies => On => On%s" +
"session.use_only_cookies => On => On%s" +
"session.use_trans_sid => 0 => 0%s" +
"%s" +
"SimpleXML%s" +
"%s" +
"Simplexml support => enabled%s" +
"Revision => %s" +
"Schema support => enabled%s" +
"%s" +
"SPL%s" +
"%s" +
"SPL support => enabled%s" +
"Interfaces => Countable, OuterIterator, RecursiveIterator, SeekableIterator, SplObserver, SplSubject%s" +
"Classes => AppendIterator, ArrayIterator, ArrayObject, BadFunctionCallException, BadMethodCallException, CachingIterator, CallbackFilterIterator, DirectoryIterator, DomainException, EmptyIterator, FilesystemIterator, FilterIterator, GlobIterator, InfiniteIterator, InvalidArgumentException, IteratorIterator, LengthException, LimitIterator, LogicException, MultipleIterator, NoRewindIterator, OutOfBoundsException, OutOfRangeException, OverflowException, ParentIterator, RangeException, RecursiveArrayIterator, RecursiveCachingIterator, RecursiveCallbackFilterIterator, RecursiveDirectoryIterator, RecursiveFilterIterator, RecursiveIteratorIterator, RecursiveRegexIterator, RecursiveTreeIterator, RegexIterator, RuntimeException, SplDoublyLinkedList, SplFileInfo, SplFileObject, SplFixedArray, SplHeap, SplMinHeap, SplMaxHeap, SplObjectStorage, SplPriorityQueue, SplQueue, SplStack, SplTempFileObject, UnderflowException, UnexpectedValueException%s" +
"%s" +
"standard%s" +
"%s" +
"Dynamic Library Support => enabled%s"+
"Internal Sendmail Support for Windows => enabled%s"+
"%s"+
"Directive => Local Value => Master Value%s"+
"assert.active => 1 => 1%s"+
"assert.bail => 0 => 0%s"+
"assert.callback => no value => no value%s" +
"assert.quiet_eval => 0 => 0%s" +
"assert.warning => 1 => 1%s" +
"auto_detect_line_endings => 0 => 0%s" +
"default_socket_timeout => 60 => 60%s" +
"from => no value => no value%s" +
"url_rewriter.tags => a=href,area=href,frame=src,form=,fieldset= => a=href,area=href,frame=src,form=,fieldset=%s"+
"user_agent => no value => no value%s"+
"%s"+
"tokenizer%s"+
"%s"+
"Tokenizer Support => enabled%s"+
"%s"+
"wddx%s"+
"%s"+
"WDDX Support => enabled%s"+
"WDDX Session Serializer => enabled%s" +
"%s"+
"xml%s" +
"%s" +
"XML Support => active%s" +
"XML Namespace Support => active%s" +
"libxml2 Version => %s" +
"%s" +
"xmlreader%s" +
"%s" +
"XMLReader => enabled%s" +
"%s" +
"xmlwriter%s" +
"%s" +
"XMLWriter => enabled%s" +
"%s" +
"zip%s" +
"%s" +
"Zip => enabled%s" +
"Extension Version => %s" +
"Zip version => %s" +
"Libzip version => %s" +
"%s" +
"zlib%s" +
"%s" +
"ZLib Support => enabled%s" +
"Stream Wrapper => compress.zlib://%s" +
"Stream Filter => zlib.inflate, zlib.deflate%s" +
"Compiled Version => %s" +
"Linked Version => %s" +
"%s" +
"Directive => Local Value => Master Value%s" +
"zlib.output_compression => Off => Off%s" +
"zlib.output_compression_level => -1 => -1%s" +
"zlib.output_handler => no value => no value%s" +
"%s" +
"Additional Modules" +
"%s" +
"Module Name" +
"%s" +
"Environment" +
"%s" +
"PHP Variables" +
"%s" +
"Variable => Value" +
"%s" +
"PHP License" +
"%s";

		ts_parts = required_ts_features_str.split("%s");
		nts_parts = required_nts_features_str.split("%s");
	} // end static
	
	public ESmokeTestStatus test(PhpBuild build, ConsoleManager cm, Host host) {
		if (!host.isWindows())
			return ESmokeTestStatus.XSKIP;
		
		try {
			String[] parts;
			if (build.isTS(host))
				parts = ts_parts;
			else
				parts = nts_parts;
			
			String info = build.getPhpInfo(cm, host);
			int i = 0, j;
			ESmokeTestStatus status = ESmokeTestStatus.PASS;
			for ( String part : parts ) {
				j = info.indexOf(part, i);
				if (j==-1) {
					cm.println(getName(), "Missing required info: `"+part+"`");
					status = ESmokeTestStatus.FAIL;
				} else {
					i = j+1;
				}
			}
			return status;
		} catch ( Exception ex ) {
			cm.addGlobalException(getClass(), "test", ex, "");
			return ESmokeTestStatus.INTERNAL_EXCEPTION;
		}
	} // end public ESmokeTestStatus test

	@Override
	public String getName() {
		return "Required-Features";
	}

} // end public class RequiredFeaturesSmokeTest
