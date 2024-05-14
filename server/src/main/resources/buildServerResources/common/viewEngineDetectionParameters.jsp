<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="engineComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.EngineComponent"/>

<c:set var="detectionMode" value="${propertiesBean.properties[engineComponent.detectionMode.name]}"/>
<c:choose>
    <c:when test="${detectionMode == engineComponent.detectionMode.automatic.name}">
        <div class="parameter">
            Engine detection mode: <strong>${engineComponent.detectionMode.automatic.displayName}</strong>
        </div>
        <div class="parameter">
            Engine identifier: <props:displayValue name="${engineComponent.engineIdentifier.name}"/>
        </div>
    </c:when>
    <c:when test="${detectionMode == engineComponent.detectionMode.manual.name}">
        <div class="parameter">
            Engine detection mode: <strong>${engineComponent.detectionMode.manual.displayName}</strong>
        </div>
        <div class="parameter">
            Engine root path: <props:displayValue name="${engineComponent.engineRootPath.name}"/>
        </div>
    </c:when>
</c:choose>
