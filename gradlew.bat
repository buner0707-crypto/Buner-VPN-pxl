@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem Gradle startup script for Windows
@rem ##########################################################################
set APP_NAME=BunerVPN
set APP_HOME=%CD%
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_HOME%/bin/java.exe" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
