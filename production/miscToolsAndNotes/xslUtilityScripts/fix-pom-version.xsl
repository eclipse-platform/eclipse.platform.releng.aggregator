<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:my="http://maven.apache.org/POM/4.0.0" exclude-result-prefixes="my xs" version="1.0">
  <!--xsl:output omit-xml-declaration="yes" indent="yes"/>
  <xsl:output indent="yes"/>
  <xsl:strip-space elements="*"/-->
  <xsl:output method="xml" encoding="UTF-8"/>
  <xsl:param name="new-version" select="'0.14.0-SNAPSHOT'"/>
  <xsl:template match="/my:project/my:version">
    <xsl:element name="version" namespace="http://maven.apache.org/POM/4.0.0"><xsl:value-of select="$new-version"/></xsl:element>
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
