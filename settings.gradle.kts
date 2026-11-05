pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HomeworkGate"
include(":app")
// Thu vien noi voi Jokers Permissions (ban build rieng cua Shizuku o ~/Documents/jokers-permissions), de tam dung
// app khac khi tiet kiem pin. Xem jokers-api/NOTICE.
include(":jokers-api")
