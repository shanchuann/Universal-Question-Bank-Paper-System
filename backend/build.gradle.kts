import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
	id("java")
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.4"
	id("org.openapi.generator") version "7.5.0"
	id("com.diffplug.spotless") version "6.25.0"
	id("jacoco")
}

group = "com.universal.qbank"
version = "0.0.1-SNAPSHOT"
description = "Generalized Exam Item Bank backend"

// NOTE: Temporarily removed explicit Java toolchain enforcement so local checks
// can run on the available JDK. Restore a strict Java 17 toolchain in CI.
// java {
//     toolchain {
//         languageVersion = JavaLanguageVersion.of(17)
//     }
// }

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
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("net.logstash.logback:logstash-logback-encoder:7.4")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	testRuntimeOnly("com.h2database:h2")

	implementation("org.openapitools:jackson-databind-nullable:0.2.6")
	implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
	
	// Apache POI for Word/Excel processing
	implementation("org.apache.poi:poi-ooxml:5.2.5")
}

spotless {
	java {
		googleJavaFormat()
		target("src/**/*.java")
	}
}

val openApiSpec = rootProject.layout.projectDirectory.dir("../specs/001-specify-exam-platform/contracts").file("openapi.yaml")
val generatedApiDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
	generatorName.set("spring")
	inputSpec.set(openApiSpec.asFile.canonicalPath)
	outputDir.set(generatedApiDir.get().asFile.canonicalPath)
	apiPackage.set("com.universal.qbank.api.generated")
	modelPackage.set("com.universal.qbank.api.generated.model")
	configOptions.putAll(
		mapOf(
			"interfaceOnly" to "true",
			"dateLibrary" to "java8",
			"useTags" to "true",
			"useSpringBoot3" to "true"
		)
	)
}

val syncedGeneratedSourcesDir = layout.buildDirectory.dir("generated/sources/openapi")

tasks.register("syncGeneratedApi") {
	group = "code generation"
	description = "Synchronize generated OpenAPI interfaces into the main source set"
	dependsOn(tasks.named("openApiGenerate"))
	doLast {
		val targetDir = syncedGeneratedSourcesDir.get().asFile
		targetDir.deleteRecursively()
		generatedApiDir.get().dir("src/main/java").asFile.copyRecursively(targetDir, overwrite = true)
	}
}

sourceSets["main"].java.srcDir(syncedGeneratedSourcesDir)

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}

tasks.named("compileJava") {
	dependsOn("syncGeneratedApi")
}

jacoco {
	toolVersion = "0.8.11"
}

tasks.withType<JacocoReport> {
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

val jacocoTestReport by tasks.existing(JacocoReport::class) {
	dependsOn(tasks.named<Test>("test"))
	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) {
					exclude("**/api/generated/**")
				}
			}
		)
	)
	sourceDirectories.setFrom(files("src/main/java"))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
	dependsOn(jacocoTestReport)
	executionData(jacocoTestReport.get().executionData)
	classDirectories.setFrom(jacocoTestReport.get().classDirectories)
	sourceDirectories.setFrom(jacocoTestReport.get().sourceDirectories)
	violationRules {
		rule {
			element = "BUNDLE"
			limit {
				counter = "LINE"
				minimum = "0.10".toBigDecimal()
			}
		}
	}
}

tasks.named("check") {
	dependsOn("jacocoTestCoverageVerification")
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
	val envFile = file("../.env")
	if (envFile.exists()) {
		envFile.readLines().forEach { line ->
			if (line.isNotBlank() && !line.startsWith("#")) {
				val parts = line.split("=", limit = 2)
				if (parts.size == 2) {
					environment(parts[0].trim(), parts[1].trim())
				}
			}
		}
	}
}
