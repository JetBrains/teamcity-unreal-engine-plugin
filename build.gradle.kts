plugins {
    alias(libs.plugins.teamcity.base)
}

teamcity {
    version = "2023.11"
    validateBeanDefinition = com.github.rodm.teamcity.ValidationMode.FAIL
}
