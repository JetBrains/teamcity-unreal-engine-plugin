<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsComponent"/>
<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>

<c:set var="noBorder" value="${true}"/>
<c:set var="parameter" value="${component.serverUrl}"/>
<%@ include file="./common/textField.jspf"%>
<c:set var="parameter" value="${component.badge}"/>
<%@ include file="./common/textField.jspf"%>
<c:set var="parameter" value="${component.project}"/>
<%@ include file="./common/textField.jspf"%>

<c:if test="${testConnectionSupported}">
    <script>
        $j(document).ready(function() {
            PublisherFeature.showTestConnection();
        });
    </script>
</c:if>

<script>
    <%@ include file="js/ensure-dependencies.js" %>
</script>
