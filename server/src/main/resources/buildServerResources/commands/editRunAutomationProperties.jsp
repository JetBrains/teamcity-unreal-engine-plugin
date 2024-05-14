<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunAutomationComponent"/>

<c:set var="noBorder" value="${true}"/>
<c:set var="parameter" value="${component.projectPath}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="noBorder" value=""/>
<c:set var="parameter" value="${component.command}"/>
<c:set var="onChange" value="BS.UnrealRunner.updateContentBasedOnSelect('${component.command.name}', ${component.command.optionNamesAsJsArray})"/>
<%@ include file="../common/selectField.jspf"%>
<c:set var="onChange" value=""/>
<c:set var="noBorder" value="${true}"/>

<c:set var="rowId" value="${component.command.filter.name}"/>
<c:set var="parameter" value="${component.runFilter}"/>
<c:set var="onChange" value="BS.UnrealRunner.updateContentBasedOnSelect('${component.runFilter.name}', ${component.runFilter.optionNamesAsJsArray})"/>
<%@ include file="../common/selectField.jspf"%>
<c:set var="onChange" value=""/>
<c:set var="rowId" value=""/>

<tr id="${component.command.list.name}">
    <th class="noBorder">
        <label for="${component.runTests.name}">
            ${component.runTests.displayName}: <l:star/>
        </label>
    </th>
    <td class="noBorder">
        <props:multilineProperty expanded="true" name="${component.runTests.name}" rows="6" cols="4" className="longField" linkTitle=""/>
        <span class="error" id="error_${component.runTests.name}"></span>
        <span class="smallNote" id="${component.runTests.name}-hint">
            ${component.runTests.description}
        </span>
    </td>
</tr>

<c:set var="noBorder" value=""/>
<c:set var="parameter" value="${component.nullRHI}"/>
<%@ include file="../common/checkbox.jspf"%>
<c:set var="noBorder" value="${true}"/>

<script>
    BS.UnrealRunner.updateContentBasedOnSelect('${component.command.name}', ${component.command.optionNamesAsJsArray});
</script>
