ANDROID_STUDIO_JDK := /Applications/Android Studio.app/Contents/jbr/Contents/Home
ANDROID_SDK := $(HOME)/Library/Android/sdk

.PHONY: verify contracts-test backend-test backend-run backend-container android-test android-connected-test android-build install-fixtures submission-preflight

verify: contracts-test android-test

contracts-test:
	cd backend && UV_CACHE_DIR=/private/tmp/looplock-uv-cache uv run pytest -q

backend-test: contracts-test

backend-run:
	cd backend && UV_CACHE_DIR=/private/tmp/looplock-uv-cache uv run uvicorn looplock_agent.main:app --host 127.0.0.1 --port 8080 --no-access-log

backend-container:
	docker build --tag looplock-agent:local backend

android-test:
	cd android && JAVA_HOME="$(ANDROID_STUDIO_JDK)" ANDROID_HOME="$(ANDROID_SDK)" ./gradlew --no-daemon testDebugUnitTest

android-connected-test:
	cd android && JAVA_HOME="$(ANDROID_STUDIO_JDK)" ANDROID_HOME="$(ANDROID_SDK)" ./gradlew --no-daemon connectedDebugAndroidTest

android-build:
	cd android && JAVA_HOME="$(ANDROID_STUDIO_JDK)" ANDROID_HOME="$(ANDROID_SDK)" ./gradlew --no-daemon assembleDebug

install-fixtures: android-build
	android/scripts/install-fixtures.sh

submission-preflight:
	scripts/submission-preflight.sh
