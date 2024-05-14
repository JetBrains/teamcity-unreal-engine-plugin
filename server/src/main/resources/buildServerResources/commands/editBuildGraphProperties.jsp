<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.BuildGraphComponent"/>

<c:set var="parameter" value="${component.script}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="noBorder" value="${true}"/>

<c:set var="parameter" value="${component.target}"/>
<%@ include file="../common/textField.jspf"%>

<tr id="BuildGraph">
    <th class="noBorder">
        <label for="${component.options.name}">
            ${component.options.displayName}:
        </label>
    </th>
    <td class="noBorder">
        <props:multilineProperty expanded="true" name="${component.options.name}" rows="6" cols="4" className="longField" linkTitle=""/>
        <span class="error" id="error_${component.options.name}"></span>
        <span class="smallNote" id="${component.options.name}-hint">
            ${component.options.description}
        </span>
    </td>
</tr>

<c:set var="parameter" value="${component.mode}"/>
<%@ include file="../common/selectField.jspf"%>
