<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">
  <xsl:output
    omit-xml-declaration="yes"
    indent="yes" />
  <xsl:strip-space elements="*" />
  <xsl:template match="/repository/units">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
      <xsl:copy-of select="unit[@id='org.eclipse.equinox.executable.feature.group']" />
    </xsl:copy>
  </xsl:template>
  <xsl:template match="/repository/units/unit[@id='org.eclipse.equinox.executable.feature.group']/@id">
    <xsl:attribute name="id">
      <xsl:value-of select="'org.eclipse.equinox.executable'" />
    </xsl:attribute>
  </xsl:template>
  <xsl:template
    match="/repository/units/unit[@id='org.eclipse.equinox.executable.feature.group']/provides/provided[@name='org.eclipse.equinox.executable.feature.group']/@name">
    <xsl:attribute name="name">
      <xsl:value-of select="'org.eclipse.equinox.executable'" />
    </xsl:attribute>
  </xsl:template>
  <xsl:template match="/repository/units/unit[@id='org.eclipse.equinox.executable.feature.group']/update/@id">
    <xsl:attribute name="id">
      <xsl:value-of select="'org.eclipse.equinox.executable'" />
    </xsl:attribute>
  </xsl:template>
  <xsl:template
    match="/repository/units/unit[@id='org.eclipse.equinox.executable.feature.group']/properties/property[@name='df_LT.featureName']">
    <xsl:copy>
      <xsl:attribute name="name">
          <xsl:value-of select="'df_LT.featureName'" />
    </xsl:attribute>
      <xsl:attribute name="value">
          <xsl:value-of select="'Eclipse Platform Launcher Executables for Multi-Architecture Builds'" />
    </xsl:attribute>
    </xsl:copy>
  </xsl:template>
  <xsl:template
    match="/repository/units/unit[@id='org.eclipse.equinox.executable.feature.group']/requires/required[starts-with(@name, 'org.eclipse.equinox.executable_root')]" />
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()">
        <!--xsl:sort select="@id|@name"/ -->
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
