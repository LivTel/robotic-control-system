<?xml version="1.0"?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">

<xsl:template match="/">

  <html><head><title>Project Information - <xsl:value-of select = "project/@id"/></title></head>
     <body bgcolor="white">
     
     <h1><u><xsl:value-of select = "project/@title"/></u></h1>

     <xsl:apply-templates select = "description"/>

     <xsl:apply-templates select="project/subproject"/>
 
     </body>
  </html>

</xsl:template>


<xsl:template match="subproject">

    <p/>
    <hr/>
    <span style = "display:block;font-size:16pt;">
       Subproject ( <xsl:value-of select = "@id"/> ) - <xsl:value-of select = "@title"/> 
    </span>

    <xsl:apply-templates select="workflow"/>

</xsl:template>

<xsl:template match="description">
    <xsl:copy-of select="."/>
</xsl:template>  


<xsl:template match = "workflow">

    <p/>   	
    <hr/>
    <br/>
    <span style = "display:block;font-size:12pt;">
    <u><xsl:value-of select = "@id"/></u> -    <xsl:value-of select = "../@id"/>/<xsl:value-of select = "@title"/>
    </span>
    
    <p/>
    <xsl:apply-templates select = "description"/>

</xsl:template>

</xsl:stylesheet>
