// The purpose of this script is to ensure that a page has all "shared"
// plugin dependencies (css, js, other files) loaded and ready to use.
// It injects them into the "head" tag if they aren't already there.

// This script requires the JSP variable 'teamcityPluginResourcesPath'
// to be defined in the scope of the file that includes this one.
// As this normally happens within a JSP file, the corresponding variable
// will be substituted.

// Also, note that when referencing a JS variable in string interpolation,
// it should be escaped with '\'

$j(document).ready(() => {
    const styles = `${teamcityPluginResourcesPath}css/common-styles.css`;

    if ($j(`link[href="\${styles}"]`).length === 0) {
        $j("<link>", {
            rel: "stylesheet",
            type: "text/css",
            href: styles,
        }).appendTo("head");
    }
});
