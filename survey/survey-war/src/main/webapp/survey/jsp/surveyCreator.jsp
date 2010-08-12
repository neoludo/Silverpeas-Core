<%--

    Copyright (C) 2000 - 2009 Silverpeas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    As a special exception to the terms and conditions of version 3.0 of
    the GPL, you may redistribute this Program in connection with Free/Libre
    Open Source Software ("FLOSS") applications as described in Silverpeas's
    FLOSS exception.  You should have recieved a copy of the text describing
    the FLOSS exception, and it is also available here:
    "http://repository.silverpeas.com/legal/licensing"

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="java.io.PrintWriter"%>
<%@ page import="java.io.IOException"%>
<%@ page import="java.io.File"%>
<%@ page import="java.io.FileInputStream"%>
<%@ page import="java.io.ObjectInputStream"%>
<%@ page import="java.util.Vector"%>
<%@ page import="java.beans.*"%>



<%@ include file="checkSurvey.jsp" %>

<%
//R�cup�ration des param�tres
String creationDate = "";
String nextAction = "";

String m_context = GeneralPropertiesManager.getGeneralResourceLocator().getString("ApplicationURL");

//Icons
String mandatoryField = m_context + "/util/icons/mandatoryField.gif";

Button validateButton = null;
Button cancelButton = null;
QuestionContainerDetail survey = null;

String action = request.getParameter("Action");
String surveyId = request.getParameter("SurveyId");
String title = request.getParameter("title");
String description = request.getParameter("description");
String beginDate = request.getParameter("beginDate");
String endDate = request.getParameter("endDate");
String nbQuestions = request.getParameter("nbQuestions");
String anonymousString = request.getParameter("anonymous");
boolean anonymous = StringUtil.isDefined(anonymousString) && "true".equalsIgnoreCase(anonymousString);

//Mise a jour de l'espace
if (action == null) {
    action = "CreateSurvey";
}

%>
<HTML>
<HEAD>
<TITLE></TITLE>
<% out.println(gef.getLookStyleSheet()); %>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/dateUtils.js"></script>
<script type="text/javascript" src="<%=m_context%>/util/javaScript/checkForm.js"></script>
<script type="text/javascript" language="JavaScript1.2">
function sendData() {
    if (isCorrectForm()) {
        document.surveyForm.submit();
    }
}

function isCorrectForm() {
     var errorMsg = "";
     var errorNb = 0;
     var title = stripInitialWhitespace(document.surveyForm.title.value);
     var description = document.surveyForm.description.value;
     var nbQuestions = document.surveyForm.nbQuestions.value;
     var re = /(\d\d\/\d\d\/\d\d\d\d)/i;
     var beginDate = document.surveyForm.beginDate.value;
     var endDate = document.surveyForm.endDate.value;
     
     var yearBegin = extractYear(beginDate, '<%=surveyScc.getLanguage()%>'); 
     var monthBegin = extractMonth(beginDate, '<%=surveyScc.getLanguage()%>');
     var dayBegin = extractDay(beginDate, '<%=surveyScc.getLanguage()%>');
     
     var yearEnd = extractYear(endDate, '<%=surveyScc.getLanguage()%>'); 
     var monthEnd = extractMonth(endDate, '<%=surveyScc.getLanguage()%>');
     var dayEnd = extractDay(endDate, '<%=surveyScc.getLanguage()%>');

     var beginDateOK = true;

     if (isWhitespace(title)) {
           errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("GML.name")%>' <%=resources.getString("GML.MustBeFilled")%>\n";
           errorNb++; 
     }
     if (!isValidTextArea(document.surveyForm.description)) {
          errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationDescription")%>' <%=resources.getString("ContainsTooLargeText")%> <%=DBUtil.TextAreaLength%> <%=resources.getString("Characters")%>\n";
          errorNb++; 
     }
     if (isWhitespace(beginDate)) {
     } else {
           if (beginDate.replace(re, "OK") != "OK") {
                   errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationBeginDate")%>' <%=resources.getString("GML.MustContainsCorrectDate")%>\n";
                   errorNb++;
                   beginDateOK = false;
           } else {
                 if (isCorrectDate(yearBegin, monthBegin, dayBegin)==false) {
                   errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationBeginDate")%>' <%=resources.getString("GML.MustContainsCorrectDate")%>\n";
                   errorNb++;
                   beginDateOK = false;
                 }
           }
     }
     if (isWhitespace(endDate)) {
     } else {
           if (endDate.replace(re, "OK") != "OK") {
                 errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationEndDate")%>' <%=resources.getString("GML.MustContainsCorrectDate")%>\n";
                 errorNb++;
           } else {
                 if (isCorrectDate(yearEnd, monthEnd, dayEnd)==false) {
                     errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationEndDate")%>' <%=resources.getString("GML.MustContainsCorrectDate")%>\n";
                     errorNb++;
                 } else {
                     if ((isWhitespace(beginDate) == false) && (isWhitespace(endDate) == false)) {
                           if (beginDateOK && isD1AfterD2(yearEnd, monthEnd, dayEnd, yearBegin, monthBegin, dayBegin) == false) {
                                  errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationEndDate")%>' <%=resources.getString("GML.MustContainsCorrectDate")%>\n";
                                  errorNb++;
                           }
                     } else {
                           if ((isWhitespace(beginDate) == true) && (isWhitespace(endDate) == false)) {
                               if (isFutureDate(yearEnd, monthEnd, dayEnd) == false) {
                                      errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationEndDate")%>' <%=resources.getString("GML.MustContainsCorrectDate")%>\n";
                                      errorNb++;
                               }
                           }
                     }
                 }
           }
     }
     if (isWhitespace(nbQuestions)) {
           errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationNbQuestionPerPage")%>' <%=resources.getString("GML.MustBeFilled")%>\n";
           errorNb++; 
     } else {
           if (isInteger(nbQuestions) == false) {
               errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationNbQuestionPerPage")%>' <%=resources.getString("GML.MustContainsFloat")%>\n";
               errorNb++; 
           } else {
                if (nbQuestions <= 0) {
                   errorMsg+="  - <%=resources.getString("GML.theField")%> '<%=resources.getString("SurveyCreationNbQuestionPerPage")%>' <%=resources.getString("MustContainsPositiveNumber")%>\n";
                   errorNb++;
                }
           }
     }
     switch(errorNb) {
        case 0 :
            result = true;
            break;
        case 1 :
            errorMsg = "<%=resources.getString("GML.ThisFormContains")%> 1 <%=resources.getString("GML.error")%> : \n" + errorMsg;
            window.alert(errorMsg);
            result = false;
            break;
        default :
            errorMsg = "<%=resources.getString("GML.ThisFormContains")%> " + errorNb + " <%=resources.getString("GML.errors")%> :\n" + errorMsg;
            window.alert(errorMsg);
            result = false;
            break;
     }
     return result;
}

</script>
</HEAD>
<BODY>
<%
if (action.equals("SendNewSurvey")) {
      if (beginDate != null) {
          if (beginDate.length()>0)
            beginDate = resources.getDBDate(beginDate);
          else
            beginDate = null;
      }
      if (endDate != null) {
          if (endDate.length()>0)
            endDate = resources.getDBDate(endDate);
          else
            endDate = null;
      }
      QuestionContainerHeader surveyHeader = new QuestionContainerHeader(null, title, description, null, null, beginDate, endDate, false, 0, new Integer(nbQuestions).intValue(), anonymous);
      QuestionContainerDetail surveyDetail = new QuestionContainerDetail();
      surveyDetail.setHeader(surveyHeader);
      surveyScc.setSessionSurveyUnderConstruction(surveyDetail);
} //End if action = SendNewSurvey

else if (action.equals("CreateSurvey")) {
      cancelButton = (Button) gef.getFormButton(generalMessage.getString("GML.cancel"), "Main.jsp", false);
      validateButton = (Button) gef.getFormButton(generalMessage.getString("GML.validate"), "javascript:onClick=sendData()", false);
      surveyScc.removeSessionSurveyUnderConstruction();
      title = "";
      description = "";
      creationDate = resources.getOutputDate(new Date());
      beginDate = resources.getInputDate(new Date());
      endDate = "";
      nbQuestions = "3";
      nextAction="SendNewSurvey";
      
      Window window = gef.getWindow();
      Frame frame = gef.getFrame();
      Board board = gef.getBoard();

      BrowseBar browseBar = window.getBrowseBar();
      browseBar.setDomainName(surveyScc.getSpaceLabel());
      browseBar.setComponentName(surveyScc.getComponentLabel(),"surveyList.jsp?Action=View");
   	  browseBar.setExtraInformation(resources.getString("SurveyCreation"));

      out.println(window.printBefore());
      out.println(frame.printBefore());
      out.println(board.printBefore());
%>
      <center>
<table CELLPADDING=5 width="100%">
    <form name="surveyForm" Action="surveyCreator.jsp" method="POST">
    <tr><td class="txtlibform"><%=resources.getString("GML.name")%> :</td><td><input type="text" name="title" size="60" value="<%=Encode.javaStringToHtmlString(title)%>" maxlength="100">&nbsp;<img border="0" src="<%=mandatoryField%>" width="5" height="5"></td></tr>
   	<tr><td class="txtlibform" valign="top"><%=resources.getString("SurveyCreationDescription")%> :</td><td><textarea name="description" cols="50" rows="4"><%=description%></textarea></td></tr>
    <tr><td class="txtlibform"><%=resources.getString("SurveyCreationDate")%> :</td><td><%=creationDate%></td></tr>
    <tr><td class="txtlibform"><%=resources.getString("SurveyCreationBeginDate")%> :</td><td><input type="text" name="beginDate" size="12" value="<%=beginDate%>" maxlength="<%=DBUtil.DateFieldLength%>"></td></tr>
    <tr><td class="txtlibform"><%=resources.getString("SurveyCreationEndDate")%> :</td><td><input type="text" name="endDate" size="12" value="<%=endDate%>" maxlength="<%=DBUtil.DateFieldLength%>"></td></tr>
    <tr><td class="txtlibform"><%=resources.getString("SurveyCreationNbQuestionPerPage")%> :</td><td><input type="text" name="nbQuestions" size="5" value="<%=nbQuestions%>" maxLength="2">&nbsp;<img border="0" src="<%=mandatoryField%>" width="5" height="5"></td></tr>
	<tr><td class="txtlibform"><%=resources.getString("survey.surveyAnonymous")%> :</td>
    	<%
			String anonymousCheck = "";
	    	if (anonymous)
	        {
	        	anonymousCheck = "checked";
	        }
		%>
    	<td><input type="checkbox" name="anonymous" value="true" <%=anonymousCheck%>>
    	  <input type="hidden" name="anonymousString" value="<%=anonymousString%>"></td>
    </tr>    
    <tr><td colspan="2">(<img border="0" src="<%=mandatoryField%>" width="5" height="5"> : <%=generalMessage.getString("GML.requiredField")%>)</td></tr>
    <tr><td><input type="hidden" name="Action" value="<%=nextAction%>"></td></tr>
    </form>
</table>
</center>
<%
	  out.println(board.printAfter());
      out.println(frame.printMiddle());
      ButtonPane buttonPane = gef.getButtonPane();
      buttonPane.addButton(validateButton);
      buttonPane.addButton(cancelButton);
      buttonPane.setHorizontalPosition();
      out.println("<BR><center>"+buttonPane.print()+"</center>");
      out.println(frame.printAfter());
      out.println(window.printAfter());
      out.println("</BODY></HTML>");
 } //End if action = ViewQuestion
if (action.equals("SendNewSurvey")) {
%>
<HTML>
<HEAD>
<script language="Javascript">
    function goToQuestionCreator() {
        document.questionForm.submit();
    }
</script>
</HEAD>
<BODY onLoad="goToQuestionCreator()">
<Form name="questionForm" Action="questionCreator.jsp" Method="POST" ENCTYPE="multipart/form-data">
<input type="hidden" name="Action" value="FirstQuestion">
</Form>
</BODY>
</HTML>
<% } %>