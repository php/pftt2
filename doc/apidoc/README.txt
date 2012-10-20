
== PHPT Support ==

=== Middlewares ===

 * (1) CLI - runs php.exe|php and php-cgi.exe|php-cgi. used by Cli and Cgi case-runners
 * (2) Builtin Web Server. 2-7 are tested using Http case-runner
 * (3) IIS
 * (4) IIS w/ WinCache
 * (5) Apache
 * (6) Apache w/ APC
 * (7) Apache w/ APC and IG-Binary

=== New Section ===

PFTT supports a new section that can be added to PHPT test files. 

PFTT uses a different CaseRunner to run a test case (PHPT test) on a particular middleware (CLI, IIS, Apache).
This section allows you to provide a custom CaseRunner class used just for that test.

This enables you to have direct control over:
 * skip - deciding if test should be skipped or run
 * clean - cleaning up after the test
 * run - running the test
 * eval - evaluation of the test run to decide PASS, FAIL, SKIP, XSKIP, XFAIL or XFAIL_PASS
 * deploy - how/where the PHPT file and its sections are deployed on the host through the middleware

The section contains executable Ruby code. The last line must return a Hash with keys for :cgi, :cli and :http, with the
values being class names for your custom CaseRunner. Your code MUST return :cgi, :cli and :http because different
middlewares require different case runners.

PFTT will instantiate the CaseRunner classes you provide each time the test case is run.

You probably should extend existing CaseRunner objects. You may use a require statement to load CaseRunner from
a common ruby file, just as you do in PHP in the --FILE-- or --SKIP-- sections.

Generally, your :cgi class should extend Php::CaseRunner::Phpt::CGI, your :cli class should extend Php::CaseRunner::Phpt::CLI
and your :http class should extend Php::CaseRunner::Phpt::Http(which tests PHP running on an actual web server(fastcgi, mod_php)),
:cgi should extend Php::CaseRunner::Phpt::Cgi(which tests php-cgi directly, like run-test.php) and :cli
should extend Php::CaseRunner::Phpt::Cli (which tests php like run-test.php).

===== Example ====

<code>
--PFTT--
# definition/implementation of class_name1
# definition/implementation of class_name2
# definition/implementation of class_name3
#
# other code to produce class_name1, class_name2 and class_name3
#
{:cgi=>class_name1, :http=>class_name2, :cli=>class_name3}
</code>

