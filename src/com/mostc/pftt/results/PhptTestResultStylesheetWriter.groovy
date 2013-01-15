package com.mostc.pftt.results

final class PhptTestResultStylesheetWriter {
	static def writeStylesheet(String file_path) {
		FileWriter fw = new FileWriter(file_path)
		fw.write("""
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">

<html>
<head>
<title><xsl:value-of select="//phptResult/@testCase" /></title>
</head>
<body>

<p>Test Case: <strong><xsl:value-of select="//phptResult/@testCase" /></strong></p>
<p>Status: <strong><xsl:value-of select="//phptResult/@status" /></strong> Actual Charset: <strong><xsl:value-of select="//phptResult/@actualcharset" /></strong></p>

<h2>Actual</h2>
<pre><xsl:value-of select="//phptResult/actual" /></pre>
<h2>Diff</h2>
<pre><xsl:value-of select="//phptResult/diff" /></pre>
<h2>ENV</h2>
<table border="1">
<xsl:for-each select="//phptResult/env">
	<tr>
		<td><xsl:value-of select="@name"/></td>
		<td><xsl:value-of select="."/></td>
	</tr>
</xsl:for-each>
</table>
<h2>STDIN</h2>
<pre><xsl:value-of select="//phptResult/stdin" /></pre>
<h2>SAPI Output</h2>
<pre><xsl:value-of select="//phptResult/SAPIOutput" /></pre>
<h2>Pre-Override Actual</h2>
<pre><xsl:value-of select="//phptResult/preoverrideActual" /></pre>
<h2>INI</h2>
<pre><xsl:value-of select="//phptResult/ini" /></pre>
<h2>EXPECTF Output</h2>
<pre><xsl:value-of select="//phptResult/expectFOutput" /></pre>
<h2>Shell Script</h2>
<pre><xsl:value-of select="//phptResult/shellScript" /></pre>
<h2>Command</h2>
<table border="1">
<xsl:for-each select="//phptResult/cmdArray/part">
	<tr>
		<td><xsl:value-of select="."/></td>
	</tr>
</xsl:for-each>
</table>
<h2>HTTP Request</h2>
<pre><xsl:value-of select="//phptResult/httpRequest" /></pre>
<h2>HTTP Response</h2>
<pre><xsl:value-of select="//phptResult/httpResponse" /></pre>

<table border="1">
<xsl:for-each select="//phptResult/phptTestCase/*">
	<tr>
		<td><xsl:value-of select="name()"/></td>
		<td><pre><xsl:value-of select="."/></pre></td>
	</tr>
</xsl:for-each>
</table>

<h2>EXPECTF/EXPECTREGEX match debug</h2>
<pre><xsl:value-of select="//phptResult/regexOutput" /></pre>

<h2>EXPECTF/EXPECTREGEX compiler output</h2>
<pre><xsl:value-of select="//phptResult/regexCompilerDump" /></pre>

</body>
</html>

</xsl:template>


</xsl:stylesheet>
""")
		fw.close()
	}
	
}
