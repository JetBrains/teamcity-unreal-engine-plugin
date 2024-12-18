<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunAutomationCommandComponent"/>

<c:set var="noBorder" value="${true}"/>

<c:set var="parameter" value="${component.command}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="parameter" value="${component.arguments}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="noBorder" value=""/>
