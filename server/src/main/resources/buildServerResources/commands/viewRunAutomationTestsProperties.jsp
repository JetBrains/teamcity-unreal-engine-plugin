<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunAutomationTestsComponent"/>

<div class="parameter">
    ${component.projectPath.displayName}: <props:displayValue name="${component.projectPath.name}"/>
</div>

<c:if test="${not empty propertiesBean.properties[component.tests.name]}">
    <div class="parameter">
        ${component.runTests.displayName}: <props:displayValue name="${component.runTests.name}"/>
    </div>
</c:if>

<c:set var="runAutomationTestsOptionDescription" value="${component.describeFlags(propertiesBean.properties)}"/>
<c:if test="${not empty runAutomationTestsOptionDescription}">
    <div class="parameter">
        Run automation tests flags: <strong>${runAutomationTestsOptionDescription}</strong>
    </div>
</c:if>
