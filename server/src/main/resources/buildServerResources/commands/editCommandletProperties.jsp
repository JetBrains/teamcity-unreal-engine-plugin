<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunCommandletComponent"/>

<c:set var="noBorder" value="${true}"/>

<c:set var="parameter" value="${component.editorExecutable}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="parameter" value="${component.projectPath}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="parameter" value="${component.commandletName}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="parameter" value="${component.commandletArguments}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="noBorder" value=""/>
