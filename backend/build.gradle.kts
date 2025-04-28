plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dk.nimlos"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        // instead of “languageVersion = …”
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.session:spring-session-core")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.liquibase:liquibase-core:4.31.1")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Exec>("buildDatabaseDockerImage") {
    description = "Builds the Docker image for the PostgreSQL database."

    // Define the Docker build command
    commandLine("docker", "build",
        "-t", "db_postgres_aos",
        "-f", "DockerConf/Dockerfiles/Dockerfile.db_postgres",
        "."
    )
}

tasks.register<Exec>("runDatabaseDockerContainer") {
    description = "Runs the Docker container for the PostgreSQL database."
    dependsOn("buildDatabaseDockerImage")

    // Define the Docker run command
    commandLine("docker", "run",
        "-d",
        "--name", "db_postgres_container_aos",
        "-e", "POSTGRES_DB=aos_tracker",
        "-e", "POSTGRES_USER=postgres",
        "-e", "POSTGRES_PASSWORD=AoS2025!",
        "-p", "5435:5432",
        "db_postgres_aos"
    )
}

tasks.bootRun {
    jvmArgs = listOf(
        "-Dspring.profiles.active=${
            if (project.hasProperty("profile"))
                project.property("profile") as String
            else
                "dev"
        }",
        "-Dspring.config.additional-location=file:./config/"
    )
}

tasks.register<Exec>("buildBackendDockerImage") {
    description = "Builds the Docker image for the Backend."

    // Define the Docker build command
    commandLine("docker", "build",
        "-t", "backend-app",
        "-f", "DockerConf/Dockerfiles/Dockerfile.backend",
        "."
    )
}

tasks.register<Exec>("runBackendDockerContainer") {
    description = "Runs the Docker container for the backend."
    dependsOn("buildBackendDockerImage")

    // Define the Docker run command
    commandLine("docker", "run",
        "-d",
        "--name", "backend-container",
        "-p", "1262:1262",
        "-e", "SPRING_PROFILES_ACTIVE=dev",
        "-e", "SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5435/aos_tracker",
        "backend-app"
    )
}