<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="buildCookRunComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.BuildCookRunComponent"/>

<div class="parameter">
    ${buildCookRunComponent.projectPath.displayName}: <props:displayValue name="${buildCookRunComponent.projectPath.name}"/>
</div>

<%@ include file="viewBuildSettings.jspf"%>
<%@ include file="viewCookSettings.jspf"%>
<%@ include file="viewRunSettings.jspf"%>
