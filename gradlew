#!/bin/sh
APP_NAME="BunerVPN"
APP_HOME=$( cd "${APP_HOME:-./}" > /dev/null && pwd -P ) || exit
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
