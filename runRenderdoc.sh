sh gradlew processResources neoforge:compileJava neoforge:createLaunchScripts
renderdoccmd capture -w --opt-hook-children sh "neoforge/build/moddev/runClient.sh"