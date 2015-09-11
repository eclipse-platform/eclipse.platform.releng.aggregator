<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--  
    This transform is to work around bug 350088.
    https://bugs.eclipse.org/bugs/show_bug.cgi?id=350088
    
    The original idea of using and XSL transform for this work-around, 
    came from a message list posting in 2009 by Paul Webster, 
    https://www.eclipse.org/forums/index.php?t=msg&th=40931&start=0&
    While was referenced in a 2009 blog post by Andrew Niefer, 
    http://aniefer.blogspot.com/2009/06/patching-features-part-2.html. 
    The details here are heavily modified, just wanted to acknowledge those
    sources of inspiration. Paul also used XSLT to final form of metadata for 
    our executable feature, in current builds. 
 -->

  <xsl:param name="patchFeatureVersionRange">
    $patchFeatureVersionRange
  </xsl:param>
  <xsl:param name="patchFeatureIU">
    $patchFeatureIU
  </xsl:param>
  <!-- 
  <xsl:variable name="patchFeatureVersionRange"><xsl:value-of select="$patchFeatureVersionRange" /></xsl:variable> 
  <xsl:variable name="patchFeatureIU"><xsl:value-of select="$patchFeatureIU" /></xsl:variable>
   -->
  <xsl:variable name="quot">
    "
  </xsl:variable>
  <xsl:variable name="apos">
    '
  </xsl:variable>

  <xsl:template match="processing-instruction('metadataRepository')">
  <xsl:text>&#xa;</xsl:text>
  <xsl:copy />
  <xsl:text>&#xa;</xsl:text>
    <xsl:comment>
      This content.xml file was transformed to include "specific range match" 
      for the feature intended to be patched. 
      Feature (IU) to be patched: 
         <xsl:value-of select="$patchFeatureIU" />
      Version Range:
         <xsl:value-of select="$patchFeatureVersionRange" />   

      XSLT Version = <xsl:copy-of select="system-property('xsl:version')" />
      XSLT Vendor = <xsl:copy-of select="system-property('xsl:vendor')" />
      XSLT Vendor URL = <xsl:copy-of select="system-property('xsl:vendor-url')" />
    <xsl:text>&#xa;</xsl:text>
    </xsl:comment>
    <xsl:text>&#xa;</xsl:text>
    <xsl:apply-templates />
  </xsl:template>


	<!-- standard copy template -->
  <xsl:template match="@*|node()">

    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

<!--  For this awkward use of concat, see http://www.oxygenxml.com/archives/xsl-list/200811/msg00544.html 
  <xsl:template match="concat ($apos, @range [@name=, $apos, $patchFeatureIU, $apos,],$apos )">
  -->
  <xsl:template match="@range[../@name='org.eclipse.jdt.feature.group']">
    <xsl:attribute name="range"><xsl:value-of select="$patchFeatureVersionRange" /></xsl:attribute>
  </xsl:template>
  -->
</xsl:stylesheet>
