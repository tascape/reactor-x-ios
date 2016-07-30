#!/bin/bash

mvn clean install

java -cp uia-tool/target/*:uia-tool/target/dependency/* com.tascape.reactor.ios.tools.UiAutomationViewer
