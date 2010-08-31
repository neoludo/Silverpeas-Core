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

<%@ include file="check.jsp" %>

<%!

final String PARAM_TYPE_CHECKBOX		= "checkbox";
final String PARAM_UPDATE_NEVER			= "never";
final String PARAM_UPDATE_ALWAYS		= "always";
final String PARAM_UPDATE_ONCREATION	= "oncreationonly";
final String PARAM_UPDATE_HIDDEN		= "hidden";
final String PARAM_TYPE_SELECT			= "select";
final String PARAM_TYPE_RADIO			= "radio";

void displayParameter(SPParameter parameter, ResourcesWrapper resource, JspWriter out) throws java.io.IOException
{
	// Value
	boolean isCheckbox = PARAM_TYPE_CHECKBOX.equals(parameter.getType());
	boolean isSelect = PARAM_TYPE_SELECT.equals(parameter.getType());
	boolean isRadio = PARAM_TYPE_RADIO.equals(parameter.getType());

	String help = parameter.getHelp(resource.getLanguage());
	if (help != null) {
		help = Encode.javaStringToJsString(help);
		out.println("<td valign=top align=left>");
		out.print("<img src=\""+resource.getIcon("JSPP.instanceHelpInfo")+"\" onmouseout=\"return nd();\" onmouseover=\"return overlib('"+help+"', CAPTION, '"+Encode.javaStringToJsString(parameter.getLabel())+"')\" border=0>");
		out.println("</td>");
	} else {
		out.println("<td align=left width=15>&nbsp;</td>");
	}

	//New line for the select items
	if (isSelect)
	{
		out.println("<td class=\"intfdcolor4\" nowrap valign=\"top\" align=left colspan=2>");
		out.println("<span class=\"txtlibform\">"+parameter.getLabel()+" : </span><br>");
	}
	else
	{
		out.println("<td class=\"intfdcolor4\" nowrap valign=\"top\" align=left>");
		out.println("<span class=\"txtlibform\">"+parameter.getLabel()+" : </span>");
		out.println("</td>");
		out.println("<td class=\"intfdcolor4\" align=left valign=\"top\">");
	}
		
	
	String disabled = "disabled";
	String sTemp = parameter.getUpdatable().toLowerCase();
	if ((PARAM_UPDATE_ALWAYS.equals(sTemp)) || (PARAM_UPDATE_ONCREATION.equals(sTemp)) || ("".equals(sTemp)))
		disabled = "";
			   
	if (isCheckbox) {
		String checked = "";
		if (parameter.getValue() != null && parameter.getValue().toLowerCase().equals("yes")) {
			checked = "checked";
		}
		out.println("<input type=\"checkbox\" name=\""+parameter.getName()+"\" value=\""+parameter.getValue()+"\" "+checked+" "+disabled+">");
	}
	else if (isSelect)
	{
		ArrayList options = parameter.getOptions();
		if (options != null)
		{
			out.println("<select name=\""+parameter.getName()+"\">");
			for (int i=0; i<options.size(); i++)
			{
				ArrayList option = (ArrayList) options.get(i);
				String name = (String) option.get(0);
				String value = (String) option.get(1);
				out.println("<option value=\""+value+"\">"+name);
			}		
			out.println("</select>");
		}
	}
	else if (isRadio)
	{
		ArrayList radios = parameter.getOptions();
		if (radios != null)
		{
			for (int i=0; i<radios.size(); i++)
			{
				ArrayList radio = (ArrayList) radios.get(i);
				String name = (String) radio.get(0);
				String value = (String) radio.get(1);
				String checked = "";
				if (i==0)
					checked = "checked";
				out.println("<input type=\"radio\" name=\""+parameter.getName()+"\" value=\""+value+"\" "+checked+">");
				out.println(name+"&nbsp;<br>");
			}		
		}
	}
	else {
		// check if parameter is mandatory or not
		boolean mandatory = parameter.isMandatory();

		String sSize = "60";
		if (parameter.getSize() != null && parameter.getSize().length() > 0)
			sSize = parameter.getSize();

		out.println("<input type=\"text\" name=\""+parameter.getName()+"\" size=\""+sSize+"\" maxlength=\"399\" value=\""+Encode.javaStringToHtmlString(parameter.getValue())+"\" "+disabled+">");

		if (mandatory) 
		{ 
			out.println("&nbsp;<img src=\""+resource.getIcon("mandatoryField")+"\" width=\"5\" height=\"5\" border=\"0\">");
		}
	}

	out.println("</td>");
}

%>

<%
WAComponent 	component 			= (WAComponent) request.getAttribute("WAComponent");
List 			parameters 			= (List) request.getAttribute("Parameters");
List 			hiddenParameters 	= (List) request.getAttribute("HiddenParameters");
String 			m_ComponentNum 		= (String) request.getAttribute("ComponentNum");
ComponentInst[] brothers 			= (ComponentInst[]) request.getAttribute("brothers");
String 			spaceId				= (String) request.getAttribute("CurrentSpaceId");

String m_JobPeas = component.getLabel();
String m_ComponentType = component.getName();

String m_ComponentIcon = iconsPath+"/util/icons/component/"+m_ComponentType+"Small.gif";

SPParameter parameter = null;

browseBar.setSpaceId(spaceId);
browseBar.setClickable(false);
browseBar.setPath(resource.getString("JSPP.creationInstance"));
%>

<HTML>
<HEAD>
<TITLE><%=resource.getString("GML.popupTitle")%></TITLE>
<%
out.println(gef.getLookStyleSheet());
%>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/animation.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/checkForm.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/overlib.js"></script>

<script language="JavaScript">

function B_ANNULER_ONCLICK() {
	location.href = "ListComponent";
}

/*****************************************************************************/
function B_VALIDER_ONCLICK() {
	if (isCorrectForm()) {
		<%
		for(int nI=0; parameters != null && nI < parameters.size(); nI++)
		{
			parameter = (SPParameter) parameters.get(nI);
			boolean isCheckbox = PARAM_TYPE_CHECKBOX.equals(parameter.getType());
			if (isCheckbox) {
			%>
		    	if (document.infoInstance.<%=parameter.getName()%>.checked)
		        	document.infoInstance.<%=parameter.getName()%>.value = "yes";
		        else
		        	document.infoInstance.<%=parameter.getName()%>.value = "no";
		    <%
			}
			%>
			document.infoInstance.<%=parameter.getName()%>.disabled = false;
		<%
		}
		%>
		document.infoInstance.submit();
	}
}

/*****************************************************************************/

function isCorrectForm() {
	var errorMsg = "";
	var errorNb = 0;

	var name = stripInitialWhitespace(document.infoInstance.NameObject.value);
	var desc = document.infoInstance.Description;

	if (isWhitespace(name)) {
		errorMsg+="  - '<%=resource.getString("GML.name")%>' <%=resource.getString("MustContainsText")%>\n";
		errorNb++;
	}

	var textAreaLength = 400;
	var s = desc.value;
	if (! (s.length <= textAreaLength)) {
		errorMsg+="  - '<%=resource.getString("GML.description")%>' <%=resource.getString("ContainsTooLargeText")+"400 "+resource.getString("Characters")%>\n";
	   	errorNb++;
	}

	<%
	for(int nI=0; parameters != null && nI < parameters.size(); nI++)
	{
		parameter = (SPParameter) parameters.get(nI);
		boolean isRadio = PARAM_TYPE_RADIO.equals(parameter.getType());
		if (parameter.isMandatory() && !isRadio) 
		{
		%>
			var paramValue = stripInitialWhitespace(document.infoInstance.<%=parameter.getName()%>.value);
			if (isWhitespace(paramValue)) {
				errorMsg+="  - '<%=parameter.getLabel()%>' <%=resource.getString("MustContainsText")%>\n";
				errorNb++;
			}
		<%
		}
	}
	%>

	switch(errorNb)
	{
		case 0 :
		    result = true;
		    break;
		case 1 :
		    errorMsg = "<%=resource.getString("ThisFormContains")%> 1 <%=resource.getString("GML.error")%> : \n" + errorMsg;
		    window.alert(errorMsg);
		    result = false;
		    break;
		default :
		    errorMsg = "<%=resource.getString("ThisFormContains")%> " + errorNb + " <%=resource.getString("GML.errors")%> :\n" + errorMsg;
		    window.alert(errorMsg);
		    result = false;
		    break;
	}
	return result;
}

function toDoOnLoad() {
	<% 
		int height = 385;
		if (parameters!=null)
		{
			int nbParameters = parameters.size();
			if (nbParameters >= 5)
				height = height+15*nbParameters;
			else
				height = height+30*nbParameters;
		}
	%>
    window.resizeTo(750,<%=height%>);
    document.infoInstance.NameObject.focus();
}

</script>
</HEAD>
<BODY marginheight="5" marginwidth="5" leftmargin="5" topmargin="5" onLoad="javascript:toDoOnLoad()">
<FORM NAME="infoInstance" action="EffectiveCreateInstance" METHOD="POST">
	<input type="hidden" name="ComponentNum" value="<%=m_ComponentNum%>">
<%
out.println(window.printBefore());
out.println(frame.printBefore());
out.println(board.printBefore());
%>
<table CELLPADDING="5" CELLSPACING="0" BORDER="0" WIDTH="100%">
	<tr>
		<td class="txtlibform"><%=resource.getString("GML.type")%> :</td>
		<td><IMG SRC="<%=m_ComponentIcon%>" border="0" align="middle">&nbsp;<%=m_JobPeas%></td>
	</tr>
	<%=I18NHelper.getFormLine(resource)%>
	<tr>
		<td class="txtlibform"><%=resource.getString("GML.name")%> :</td>
		<td><input type="text" name="NameObject" size="60" maxlength="60">&nbsp;<img src="<%=resource.getIcon("mandatoryField")%>" width="5" height="5" border="0"></td>
	</tr>
	<tr>
		<td class="txtlibform"><%=resource.getString("GML.description")%> :</td>
		<td valign="top"><textarea name="Description" rows="4" cols="59"></textarea></td>
	</tr>
	<tr>
		<td class="txtlibform"><%=resource.getString("JSPP.ComponentPlace")%> :</td>
		<td>
			<SELECT name="ComponentBefore" id="ComponentBefore">
				<%
					for (int i = 0; i < brothers.length; i++)
					{
						out.println("<OPTION value=\"" + brothers[i].getId() + "\">" + brothers[i].getLabel() + "</OPTION>");
					}
				%>
				<OPTION value="-1" selected><%=resource.getString("JSPP.PlaceLast")%></OPTION>
			</SELECT>
		</td>
	</tr>
</table>
<% 
if (parameters.size() > 0)
{
%>
	<br>
	<table width=100%>
	<tr class="intfdcolor51"><td align="center"><span class="txtlibform"><img src="<%=resource.getIcon("JSPP.px")%>" height="20" width="1" align="middle">Param�tres de l'instance</span></td></tr>
	</table>
<%
}
%>
<table border=0>
<tr>
<%
	boolean on2Columns = false;
	if (parameters.size() >= 5)
		on2Columns = true;
	
	if (on2Columns)
	{
		out.println("<td>");
		out.println("<table border=0 width=\"100%\">");
		for(int nI=0; parameters != null && nI < parameters.size(); nI++)
		{
			parameter = (SPParameter) parameters.get(nI);
			if (nI%2 == 0)
				out.println("<tr>");

			displayParameter(parameter, resource, out);

			if (nI%2 != 0)
				out.println("</tr>");
			else
				out.println("<td width=\"40px\">&nbsp;</td>");
		}
		if (parameters.size()%2 != 0)
		{
			out.println("<td>&nbsp;</td><td>&nbsp;</td>");
			out.println("</tr>");
		}
		out.println("</table>");
		out.println("</td>");
	} else {
		for(int nI=0; parameters != null && nI < parameters.size(); nI++)
		{
			parameter = (SPParameter) parameters.get(nI);
			out.println("<tr>");
			displayParameter(parameter, resource, out);
			out.println("</tr>");
		}
	}
	
	for(int nI=0; hiddenParameters != null && nI < hiddenParameters.size(); nI++)
	{
		parameter = (SPParameter) hiddenParameters.get(nI);
		
		out.println("<input type=\"hidden\" name=\""+parameter.getName()+"\" value=\""+parameter.getValue()+"\"/>\n");
	}
%>
	<tr align="left">
		<td colspan="3">(<img border="0" src="<%=resource.getIcon("mandatoryField")%>" width="5" height="5"> : <%=resource.getString("GML.requiredField")%>)</td>
	</tr>
	</table>
<%
	out.println(board.printAfter());
	out.println("<BR/>");
	ButtonPane buttonPane = gef.getButtonPane();
	buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.validate"), "javascript:onClick=B_VALIDER_ONCLICK();", false));
	buttonPane.addButton((Button) gef.getFormButton(resource.getString("GML.cancel"), "javascript:onClick=B_ANNULER_ONCLICK();", false));
	out.println("<center>"+buttonPane.print()+"</center>");
	
	out.println(frame.printAfter());
	out.println(window.printAfter());
%>
</FORM>

</BODY>
</HTML>