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
