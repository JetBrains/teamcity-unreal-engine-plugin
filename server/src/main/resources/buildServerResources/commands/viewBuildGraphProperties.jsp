<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="buildGraphComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.BuildGraphComponent"/>

<div class="parameter">
    ${buildGraphComponent.script.displayName}: <props:displayValue name="${buildGraphComponent.script.name}"/>
</div>

<div class="parameter">
    ${buildGraphComponent.target.displayName}: <props:displayValue name="${buildGraphComponent.target.name}"/>
</div>

<c:if test="${not empty propertiesBean.properties[buildGraphComponent.options.name]}">
    <div class="parameter">
        ${buildGraphComponent.options.displayName}: <props:displayValue name="${buildGraphComponent.options.name}"/>
    </div>
</c:if>

<div class="parameter">
    ${buildGraphComponent.mode.displayName}: <props:displayValue name="${buildGraphComponent.mode.name}"/>
</div>

<c:set var="buildGraphMode" value="${propertiesBean.properties[buildGraphComponent.mode.name]}"/>
<c:choose>
    <c:when test="${buildGraphMode == buildGraphComponent.mode.distributed.name}">
        <c:set var="postBadges" value="${propertiesBean.properties[buildGraphComponent.postBadges.name]}"/>
        <c:if test="${postBadges}">
            <div class="parameter">
                ${buildGraphComponent.postBadges.displayName}: <strong>Yes</strong>
            </div>

            <div class="parameter">
                ${buildGraphComponent.ugsMetadataServer.displayName}: <props:displayValue name="${buildGraphComponent.ugsMetadataServer.name}"/>
            </div>
        </c:if>
    </c:when>
</c:choose>
