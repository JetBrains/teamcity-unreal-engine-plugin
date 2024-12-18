<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunAutomationCommandComponent"/>

<div class="parameter">
    ${component.command.displayName}: <props:displayValue name="${component.command.name}"/>
</div>

<div class="parameter">
    ${component.arguments.displayName}: <props:displayValue name="${component.arguments.name}"/>
</div>
