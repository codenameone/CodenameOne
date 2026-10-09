pluginManagement {
    repositories {
        mavenLocal()
        maven("https://repo.codenameone.com/maven2")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.codenameone") {
                useModule("com.codenameone:codenameone-gradle-plugin:${requested.version}")
            }
        }
    }
}
plugins { id("com.codenameone") version "8.0-SNAPSHOT" }
rootProject.name = "backend-mvc"
