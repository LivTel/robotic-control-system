<?xml version="1.0"?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">

<xsl:template match="/">

   \document{class="article"}

     <xsl:apply-templates select = "description"/>

     <xsl:apply-templates select="project/subproject"/>

</xsl:template>


<xsl:template match="subproject">

    \section{<xsl:value-of select = "@title"/>}

    <xsl:apply-templates select = "description"/>

    <xsl:apply-templates select="workflow"/>

</xsl:template>

<xsl:template match="description">
    <xsl:copy-of select="."/>
</xsl:template>  


<xsl:template match = "workflow">

    <p/>   	
    <hr color = "blue"/>
    <br/>
    <span style = "display:block;font-size:16pt;font-color:blue;background:#ccff22">
    <u><xsl:value-of select = "@id"/></u> -    <xsl:value-of select = "../@id"/>/<xsl:value-of select = "@title"/>
    </span>
    
    <p/>
    <xsl:apply-templates select = "description"/>

    <p/>
    <div style = "display:block;position:relative;left:20;background:#ff22cc">
      Notes associated with this workflow.
      <ul>
      <font color = "blue" ><xsl:apply-templates select = "note"/> </font>
      </ul>
    </div>

    <p/>
    <div style = "display:block;position:relative;left:20px;width:90%;background:#cc22cc">
      Changelog for this workflow.
      <table border = '1' bgcolor = 'lightgrey'>
      <xsl:apply-templates select="change"/>
      </table>
    </div>

    <p/>
    <div style = "display:block;position:relative;left:20px;width:90%;background:#cc22cc">
     New grouped stuff by class
     <table bgcolor = "#ccddee" frame = "hsides" border = "1">
     <xsl:for-each-group select="change" group-by="classref/@name">  
      <tr><td valign = "top"><b><xsl:value-of select="current-grouping-key()"/></b> <p/><xsl:value-of select="classref/@package"/></td>
      <td><table>    
      <xsl:for-each select="current-group()">
          <tr><td valign = "top"><xsl:value-of select="@date"/></td><td> <xsl:value-of select="detail"/></td></tr>
      </xsl:for-each>
      </table></td></tr>
     </xsl:for-each-group>
     </table>
    </div>
</xsl:template>

<xsl:template match = "note">
  <li><u><xsl:value-of select = "@class"/> </u>, <xsl:value-of select = "@date"/> -  <xsl:copy-of select="."/> </li>
</xsl:template>

<xsl:template match = "change">
 <tr> <td fgcolor = "blue"><xsl:value-of select="classref/@name"/></td>     
      <td fgcolor = "blue"><xsl:value-of select="@date"/> </td> 
      <td fgcolor = "blue"><xsl:value-of select="detail"/> </td> </tr>
</xsl:template>

</xsl:stylesheet>
