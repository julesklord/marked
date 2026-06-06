// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

val keystoreFile = file("debug.keystore")
val base64File = file("debug.keystore.base64")
if (base64File.exists() && !keystoreFile.exists()) {
    try {
        val base64Text = base64File.readText().trim()
        val decodedBytes = java.util.Base64.getDecoder().decode(base64Text)
        keystoreFile.writeBytes(decodedBytes)
        logger.lifecycle("Automatically decoded debug.keystore from base64")
    } catch (e: Exception) {
        logger.error("Failed to automatically decode debug.keystore: ${e.message}")
    }
}

