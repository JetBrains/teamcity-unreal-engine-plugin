<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.UnrealRunnerComponent"/>
<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>

<jsp:include page="${teamcityPluginResourcesPath}/common/viewEngineDetectionParameters.jsp"/>

<c:forEach items="${component.commandType.commands}" var="command">
    <c:if test="${propertiesBean.properties[component.commandType.name] == command.option.name}">
        <div class="parameter">
            ${component.commandType.displayName}: <strong>${command.option.displayName}</strong>
        </div>
        <jsp:include page="${teamcityPluginResourcesPath}/commands/${command.viewPage}"/>
    </c:if>
</c:forEach>

<c:if test="${not empty propertiesBean.properties[component.additionalArguments.name]}">
    <div class="parameter">
        ${component.additionalArguments.displayName}: <props:displayValue name="${component.additionalArguments.name}"/>
    </div>
</c:if>
