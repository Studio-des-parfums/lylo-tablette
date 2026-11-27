# Raccourcis du projet — l'équivalent des « npm run … »
#   make apk                          → build l'APK (dans dist/)
#   make install ROBOT_IP=192.168.x.x → build + installe sur le robot en Wi-Fi
#   make connect ROBOT_IP=192.168.x.x → teste seulement la connexion au robot

ADB ?= $(shell command -v adb || echo $(HOME)/Library/Android/sdk/platform-tools/adb)

.PHONY: apk install connect

apk:
	./scripts/build-apk.sh

install:
	ROBOT_IP=$(ROBOT_IP) ./scripts/build-apk.sh --install

connect:
	@test -n "$(ROBOT_IP)" || (echo "Usage : make connect ROBOT_IP=192.168.x.x" && exit 1)
	ping -c 3 $(ROBOT_IP)
	$(ADB) connect $(ROBOT_IP):5555
	$(ADB) devices
