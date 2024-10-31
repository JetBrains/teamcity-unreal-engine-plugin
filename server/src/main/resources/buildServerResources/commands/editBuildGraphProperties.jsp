<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.BuildGraphComponent"/>

<c:set var="parameter" value="${component.script}"/>
<%@ include file="../common/textField.jspf"%>

<c:set var="noBorder" value="${true}"/>

<c:set var="parameter" value="${component.target}"/>
<%@ include file="../common/textField.jspf"%>

<tr id="BuildGraph">
    <th class="noBorder">
        <label for="${component.options.name}">
            ${component.options.displayName}:
        </label>
    </th>
    <td class="noBorder">
        <props:multilineProperty expanded="true" name="${component.options.name}" rows="6" cols="4" className="longField" linkTitle=""/>
        <span class="error" id="error_${component.options.name}"></span>
        <span class="smallNote" id="${component.options.name}-hint">
            ${component.options.description}
        </span>
    </td>
</tr>

<c:set var="parameter" value="${component.mode}"/>
<c:set var="onChange" value="BS.UnrealRunner.updateContentBasedOnSelect('${component.mode.name}', ${component.mode.optionNamesAsJsArray})"/>
<%@ include file="../common/selectField.jspf"%>

<tbody id="${component.mode.distributed.name}">
    <c:set var="parameter" value="${component.postBadges}"/>
    <c:set var="onclick" value="BS.UnrealRunner.updateContentBasedOnCheckbox('${component.postBadges.name}', '.post-badges-settings')"/>
    <%@ include file="../common/checkbox.jspf"%>
    <c:set var="onclick" value=""/>

    <c:set var="cssClass" value="post-badges-settings"/>
    <c:set var="parameter" value="${component.ugsMetadataServer}"/>
    <%@ include file="../common/textField.jspf"%>
    <c:set var="cssClass" value=""/>
</tbody>

<script type="text/javascript">
    BS.UnrealRunner.updateContentBasedOnSelect('${component.mode.name}', ${component.mode.optionNamesAsJsArray})
    BS.UnrealRunner.updateContentBasedOnCheckbox('${component.postBadges.name}', '.post-badges-settings');
</script>
