<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" indent="no"/>

<!--
This stylesheet is used to transform the collected list of test results in XML
into a sorted list of name/status pairs in plaintext. This plaintext list is
then suitable for easy regression testing.
-->

<xsl:variable name="newline">
<xsl:text>
</xsl:text>
</xsl:variable>

<xsl:template match="/">
<xsl:for-each select="/testsuites/testsuite/testcase">
	<xsl:sort select="@name"/>
	<xsl:value-of select="@name"/>
	<xsl:text> </xsl:text>
	<xsl:choose>
		<xsl:when test="failure">
			<xsl:text>fail</xsl:text>
		</xsl:when>
		<xsl:when test="error">
			<xsl:text>error</xsl:text>
		</xsl:when>
		<xsl:otherwise>
			<xsl:text>pass</xsl:text>
		</xsl:otherwise>
	</xsl:choose>
	<xsl:value-of select="$newline"/>
</xsl:for-each>
</xsl:template>
</xsl:stylesheet>
