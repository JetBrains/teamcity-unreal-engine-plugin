<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.BuildCookRunComponent"/>
<jsp:useBean id="buildConfigurationComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.BuildConfigurationComponent"/>
<jsp:useBean id="cookComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.CookComponent"/>
<jsp:useBean id="runComponent" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.RunComponent"/>

<c:set var="parameter" value="${component.projectPath}"/>
<%@ include file="../common/textField.jspf"%>

<l:settingsGroup title="Build">
    <c:set var="buildConfigurationComponent" value="${buildConfigurationComponent}"/>
    <%@ include file="editBuildSettings.jspf"%>
</l:settingsGroup>

<l:settingsGroup title="Cook">
    <c:set var="cookComponent" value="${cookComponent}"/>
    <%@ include file="editCookSettings.jspf"%>
</l:settingsGroup>

<l:settingsGroup title="Run">
    <c:set var="runComponent" value="${runComponent}"/>
    <%@ include file="editRunSettings.jspf"%>
</l:settingsGroup>

<c:set var="noBorder" value="${true}"/>
