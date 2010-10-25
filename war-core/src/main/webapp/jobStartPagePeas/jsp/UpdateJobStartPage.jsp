<%--

    Copyright (C) 2000 - 2009 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have received a copy of the text describing
    the FLOSS exception, and it is also available here:
    "http://repository.silverpeas.com/legal/licensing"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="com.stratelia.webactiv.beans.admin.SpaceInst"%>
<%@ include file="check.jsp" %>
<%
String spaceId = (String) request.getAttribute("CurrentSpaceId");

browseBar.setSpaceId(spaceId);
browseBar.setClickable(false);
browseBar.setExtraInformation(resource.getString("JSPP.updateHomePage"));

	Integer m_firstPageType = (Integer)request.getAttribute("FirstPageType");
	String m_firstPageParam	= (String) request.getAttribute("FirstPageParam");
	if (m_firstPageParam == null || m_firstPageParam.equals("null"))
		m_firstPageParam = "";
		
	DisplaySorted[] m_Components = (DisplaySorted[])request.getAttribute("Peas");
	
	String 	defaultSP 	= "";
	String	peasSP 		= "";
	String 	portletSP 	= "";
	String	urlSP 		= "";
	switch (m_firstPageType.intValue())
	{
		case SpaceInst.FP_TYPE_STANDARD : 			defaultSP = "checked";
													break;
		case SpaceInst.FP_TYPE_COMPONENT_INST : 	peasSP = "checked";
													break;
		case SpaceInst.FP_TYPE_PORTLET : 			portletSP = "checked";
													break;
		case SpaceInst.FP_TYPE_HTML_PAGE : 			urlSP = "checked";
													break;
		default : defaultSP = "checked";
	}
%>
<HTML>
<HEAD>
<TITLE><%=resource.getString("GML.popupTitle")%></TITLE>
<%
out.println(gef.getLookStyleSheet());
%>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/checkForm.js"></script>
<script language="JavaScript">
function isCorrectForm() {
	var errorMsg = "";
    var errorNb = 0;
    var toCheck = document.multichoice.URL.value;
    if (isWhitespace(toCheck)) {
    	errorMsg+="<%=resource.getString("GML.theField")%> '<%=resource.getString("JSPP.webPage")%>' <%=resource.getString("GML.MustBeFilled")%>\n";
    	errorNb++;
    } else {
    	if (toCheck.substring(0,7) != "http://" && toCheck.substring(0,8) != "https://")
    	{
    		errorMsg+="<%=resource.getString("GML.theField")%> '<%=resource.getString("JSPP.webPage")%>' <%=resource.getString("JSPP.MustBeginsByHTTP")%>\n";
    		errorNb++;
    	}
    }
    
    switch(errorNb)
    {
    	case 0 :
            result = true;
            break;
        default :
            window.alert(errorMsg);
            result = false;
            break;
     }
     return result;	  
}
function sendData()
{
	if (document.multichoice.choix[3].checked)
	{
		if (isCorrectForm())
			document.multichoice.submit();
	}
	else
		document.multichoice.submit();
}
</script>
</HEAD>
<BODY marginheight=5 marginwidth=5 leftmargin=5 topmargin=5>
<% 
out.println(window.printBefore());
out.println(frame.printBefore());
%>
<center>
<%
out.println(board.printBefore());
%>
<FORM NAME="multichoice" method="post" action="Choice">
<table CELLPADDING="2" CELLSPACING="0" BORDER="0" WIDTH="100%">
	<tr>
		<td><INPUT type="radio" name="choix" value="DefaultStartPage" <%=defaultSP%>></td>
		<td class="textePetitBold" nowrap><%=resource.getString("JSPP.main") %></td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td><INPUT type="radio" name="choix" value="SelectPeas" <%=peasSP%>></td>
		<td class="textePetitBold" nowrap><%=resource.getString("JSPP.peas")%> :</td>
		<td>
			<select name="peas" size="1">
			<%				
				if (m_Components != null)
				{
					String checked = "";
					for(int i = 0; i < m_Components.length; i++)
					{
						if (m_firstPageParam.equals(m_Components[i].id))
						{
							checked = "selected";
							m_firstPageParam = "";
						}
							
						out.println("<option value=\""+m_Components[i].id+"\" "+checked+">"+m_Components[i].name+"</option>");
						
						checked = "";
				    }
			    }
			%>
			</select>
		</td>
	</tr>
	<tr>
		<td><INPUT type="radio" name="choix" value="Portlet" <%=portletSP%>></td>
		<td class="textePetitBold" nowrap><%=resource.getString("JSPP.portlet")%>...</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td><INPUT type="radio" name="choix" value="URL" <%=urlSP%>></td>
		<td class="textePetitBold" nowrap><%=resource.getString("JSPP.webPage")%> :</td>
		<td><input type="text" name="URL" size="60" maxlength="255" value="<%=m_firstPageParam%>"></td>
	</tr>
</table>
</form>
<%
out.println(board.printAfter());
%>
<br>
<%
 ButtonPane bouton = gef.getButtonPane();
 bouton.addButton((Button) gef.getFormButton(resource.getString("GML.validate"), "javascript:sendData();", false));
 out.println(bouton.print());
%>
</center>
<% 
out.println(frame.printAfter());
out.println(window.printAfter());
%>
</BODY>
</HTML>