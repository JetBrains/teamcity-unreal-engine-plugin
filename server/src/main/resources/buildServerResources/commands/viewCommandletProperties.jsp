<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunCommandletComponent"/>

<div class="parameter">
    ${component.editorExecutable.displayName}: <props:displayValue name="${component.editorExecutable.name}"/>
</div>

<div class="parameter">
    ${component.projectPath.displayName}: <props:displayValue name="${component.projectPath.name}"/>
</div>

<div class="parameter">
    ${component.commandletName}: <props:displayValue name="${component.commandletName.name}"/>
</div>

<div class="parameter">
    ${component.commandletArguments}: <props:displayValue name="${component.commandletArguments.name}"/>
</div>
