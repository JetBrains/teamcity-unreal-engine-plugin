plugins {
    alias(libs.plugins.teamcity.base)
}

teamcity {
    version = libs.versions.teamcity.get()
    validateBeanDefinition = com.github.rodm.teamcity.ValidationMode.FAIL
}
