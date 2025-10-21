pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        // ⚡️ Kho chứa SDK Gemini AI (Google public)
        maven {
            url = uri("https://maven.pkg.dev/google-gemini-public/java")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // ⚡️ BẮT BUỘC phải thêm dòng này
        maven {
            url = uri("https://maven.pkg.dev/google-gemini-public/java")
        }
    }
}

rootProject.name = "SmartPantryChef"
include(":app")
