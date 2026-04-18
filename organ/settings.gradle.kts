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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ✅ 阿里云镜像（放在最前面）
        maven {
            url = uri("https://maven.aliyun.com/repository/public/")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google/")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin/")
        }

        // 保留原始仓库作为备选
        google()
        mavenCentral()
    }
}

rootProject.name = "organ"
include(":app")
