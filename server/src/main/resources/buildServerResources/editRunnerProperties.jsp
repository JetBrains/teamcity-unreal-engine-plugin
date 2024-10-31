<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri ="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="component" class="com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui.UnrealRunnerComponent"/>
<jsp:useBean id="teamcityPluginResourcesPath" scope="request" type="java.lang.String"/>

<script type="text/javascript">
    BS.UnrealRunner = BS.UnrealRunner || {
        updateContentBasedOnCheckbox(checkboxId, contentValueClass) {
            const advancedHiddenCoreClass = ".advanced_hidden";
            const isChecked = $j(BS.Util.escapeId(checkboxId)).is(":checked");
            const $content = $j(BS.Util.escape(contentValueClass)).not(advancedHiddenCoreClass);
            if (isChecked) {
                $content.show();
            } else {
                $content.hide();
            }
        },
        updateContentBasedOnSelect(selectId, options) {
            const selectedValue = $j(BS.Util.escapeId(selectId)).val();

            const hideAll = () => {
                for (const option of options) {
                    $j(BS.Util.escapeId(option)).hide();
                }
            }

            const show = (option) => $j(BS.Util.escapeId(option)).show();

            for (const option of options) {
                if (selectedValue === option) {
                    hideAll();
                    show(option)
                    BS.MultilineProperties.updateVisible();
                    return;
                }
            }

            hideAll();
            show(options.first())
            BS.MultilineProperties.updateVisible();
        }
    };

    const reactExtensions = document.createElement('script');
    reactExtensions.id = "unreal-runner-react-extensions";
    reactExtensions.src = "<c:url value='${teamcityPluginResourcesPath}react/bundle.js'/>";
    reactExtensions.onload = () => {
        $j(reactExtensions).trigger("UnrealRunnerReactExtensionsLoaded");
    };
    document.body.appendChild(reactExtensions);
</script>

<l:settingsGroup title="Engine detection">
    <jsp:include page="${teamcityPluginResourcesPath}/common/editEngineDetectionParameters.jsp"/>
</l:settingsGroup>

<l:settingsGroup title="General">
    <c:set var="parameter" value="${component.commandType}"/>
    <c:set var="onChange" value="BS.UnrealRunner.updateContentBasedOnSelect('${component.commandType.name}', ${component.commandType.optionNamesAsJsArray})"/>
    <%@ include file="./common/selectField.jspf"%>
    <c:set var="onChange" value=""/>

    <c:forEach items="${component.commandType.commands}" var="command">
        <tbody id="${command.option.name}">
            <jsp:include page="${teamcityPluginResourcesPath}/commands/${command.editPage}"/>
        </tbody>
    </c:forEach>
</l:settingsGroup>

<c:set var="parameter" value="${component.additionalArguments}"/>
<%@ include file="./common/textField.jspf"%>

<script>
    BS.UnrealRunner.updateContentBasedOnSelect('${component.commandType.name}', ${component.commandType.optionNamesAsJsArray});
    <%@ include file="js/ensure-dependencies.js" %>
</script>
