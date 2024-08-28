ARG OS_BASE_IMAGE_NAME=sles
ARG OS_BASE_IMAGE_REPO=armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release
ARG OS_BASE_IMAGE_TAG=6.16.0-13

FROM ${OS_BASE_IMAGE_REPO}/${OS_BASE_IMAGE_NAME}:${OS_BASE_IMAGE_TAG}

ARG BUILD_DATE
ARG COMMIT
ARG APP_VERSION

LABEL author="ENM/Chanakya"
LABEL com.ericsson.product-number="CXC 174 3000"
LABEL org.opencontainers.image.title="CNIV PM Benchmark" \
      org.opencontainers.image.created=${BUILD_DATE} \
      org.opencontainers.image.revision=${COMMIT} \
      org.opencontainers.image.vendor="Ericsson" \
      org.opencontainers.image.version=${APP_VERSION}

# DR-D1123-122
ARG USER_ID=100001
ARG USER_NAME="iotest"

ARG _IMAGE_CONTENT_=image_content
ARG _JARS_=$_IMAGE_CONTENT_/iotest.jar
#ARG _LTE_RESOURCES_=/*.xml
ARG _ARTIFACTS_HOME_=/pmfilebench/

COPY ${_JARS_} $_ARTIFACTS_HOME_
#COPY image_content/*.xml /
COPY image_content/entry.sh /

RUN mkdir -p $_ARTIFACTS_HOME_ \
    && echo "$USER_ID:x:$USER_ID:0:An Identity for $USER_NAME:/nonexistent:/bin/false" >>/etc/passwd \
    && echo "$USER_ID:!::0:::::" >>/etc/shadow

ARG CBOS_REPO=arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles
ARG OS_BASE_IMAGE_TAG

RUN zypper addrepo --gpgcheck-strict -f https://${CBOS_REPO}/${OS_BASE_IMAGE_TAG} COMMON_BASE_OS_SLES_REPO \
    && zypper --gpg-auto-import-keys refresh \
    && zypper install -y 'java-11-openjdk>=11.0.21.0' \
    && zypper clean \
    && echo "$USER_NAME:x:$USER_ID:$USER_ID::/nonexistent:/bin/false" >>/etc/passwd \
    && echo "$USER_NAME:!:0::::::" >>/etc/shadow

USER $USER_ID
