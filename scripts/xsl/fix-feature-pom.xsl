<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
xmlns:xs="http://www.w3.org/2001/XMLSchema" 
xmlns:my="http://maven.apache.org/POM/4.0.0" 
exclude-result-prefixes="my xs"
version="2.0">
  <!--xsl:output omit-xml-declaration="yes" indent="yes"/>
  <xsl:output indent="yes"/>
  <xsl:strip-space elements="*"/-->
<xsl:output method="xml" 
              encoding="UTF-8"/>

<xsl:template name="group-id">
	<xsl:param name="pText" select="."/>
	<xsl:param name="pCount" select="0"/>
	<xsl:if test="$pCount &lt; 3">
		<xsl:value-of select="substring-before(concat($pText, '.'), '.')"/>
		<xsl:if test="$pCount &lt; 2"><xsl:text>.</xsl:text></xsl:if>
		<xsl:call-template name="group-id">
			<xsl:with-param name="pText" select="substring-after($pText, '.')"/>
			<xsl:with-param name="pCount" select="$pCount + 1"/>
		</xsl:call-template>
	</xsl:if>
	<xsl:if test="$pCount = 3"><xsl:text>.feature</xsl:text></xsl:if>
</xsl:template>

<xsl:template match="/my:project/my:groupId">
	<groupId xmlns="http://maven.apache.org/POM/4.0.0"><xsl:call-template name="group-id">
<xsl:with-param name="pText" select="/my:project/my:artifactId/text()"/>
<xsl:with-param name="pCount" select="0"/>
</xsl:call-template></groupId>
</xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!--xsl:template match="node()|@*">
  <xsl:copy>
    <xsl:apply-templates select="node()|@*">
      <xsl:sort select="name()" />
      <xsl:sort select="@*" />
    </xsl:apply-templates>
  </xsl:copy>
</xsl:template-->
</xsl:stylesheet>
