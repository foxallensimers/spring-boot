#!/bin/bash
set -ex

###########################################################
# UTILS
###########################################################

apt-get update
apt-get install --no-install-recommends -y ca-certificates net-tools libxml2-utils git curl libudev1 libxml2-utils iptables iproute2 jq
rm -rf /var/lib/apt/lists/*

curl https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.2/concourse-java.sh > /opt/concourse-java.sh


###########################################################
# JAVA
###########################################################

case "$1" in
	java8)
		 JDK_URL=https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u192-b12/OpenJDK8U-jdk_x64_linux_hotspot_8u192b12.tar.gz
		 COMPONENTS=2
	;;
	java11)
		 JDK_URL=https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_linux_hotspot_11.0.1_13.tar.gz
		 COMPONENTS=1
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
mkdir -p /opt/openjdk
cd /opt/openjdk
curl -L ${JDK_URL} | tar zx --strip-components=${COMPONENTS}
test -f /opt/openjdk/bin/java


###########################################################
# DOCKER
###########################################################

cd /
curl -L https://download.docker.com/linux/static/stable/x86_64/docker-18.06.1-ce.tgz | tar zx
mv /docker/* /bin/
chmod +x /bin/docker*

export ENTRYKIT_VERSION=0.4.0
curl -L https://github.com/progrium/entrykit/releases/download/v${ENTRYKIT_VERSION}/entrykit_${ENTRYKIT_VERSION}_Linux_x86_64.tgz | tar zx
chmod +x entrykit && \
mv entrykit /bin/entrykit && \
entrykit --symlink
