<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="engineComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.EngineComponent"/>

<c:set var="parameter" value="${engineComponent.detectionMode}"/>
<c:set var="onChange" value="BS.UnrealRunner.updateContentBasedOnSelect('${engineComponent.detectionMode.name}', ${engineComponent.detectionMode.optionNamesAsJsArray})"/>
<%@ include file="./selectField.jspf"%>
<c:set var="onChange" value=""/>

<tbody id="${engineComponent.detectionMode.automatic.name}">
    <c:set var="parameter" value="${engineComponent.engineIdentifier}"/>
    <%@ include file="textField.jspf"%>
</tbody>

<tbody id="${engineComponent.detectionMode.manual.name}">
    <c:set var="parameter" value="${engineComponent.engineRootPath}"/>
    <%@ include file="textField.jspf"%>
</tbody>

<script type="text/javascript">
    BS.UnrealRunner.updateContentBasedOnSelect('${engineComponent.detectionMode.name}', ${engineComponent.detectionMode.optionNamesAsJsArray});
</script>
