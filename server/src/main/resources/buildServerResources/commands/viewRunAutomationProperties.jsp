<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="runAutomationComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunAutomationComponent"/>

<div class="parameter">
    ${runAutomationComponent.projectPath.displayName}: <props:displayValue name="${runAutomationComponent.projectPath.name}"/>
</div>

<c:if test="${not empty propertiesBean.properties[runAutomationComponent.tests.name]}">
    <div class="parameter">
        ${runAutomationComponent.runTests.displayName}: <props:displayValue name="${runAutomationComponent.runTests.name}"/>
    </div>
</c:if>

<c:set var="runAutomationOptionDescription" value="${runAutomationComponent.describeFlags(propertiesBean.properties)}"/>
<c:if test="${not empty runAutomationOptionDescription}">
    <div class="parameter">
        Run automation flags: <strong>${runAutomationOptionDescription}</strong>
    </div>
</c:if>
