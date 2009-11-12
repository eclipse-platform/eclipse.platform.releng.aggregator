<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="yes"/>

<xsl:template match="/">
	<html>
	<head>
		<title>Feature Version Comparison Results</title>
	</head>
	
	  <body>
	  <h1>Feature Version Comparison Results</h1>
			<xsl:for-each select="CompareResult/Category">

		      		<h2>Message type:  <xsl:value-of select="@Name"/></h2>
				
			     <xsl:for-each select="Info">
				<xsl:sort select="@Message"/>

<li>				<xsl:value-of select="@Message"/></li>
<br><br></br></br>
    			       
			</xsl:for-each>
			</xsl:for-each>

	
 	</body>
	</html>
</xsl:template>

</xsl:stylesheet>