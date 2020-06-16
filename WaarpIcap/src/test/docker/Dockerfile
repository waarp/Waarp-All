FROM ubuntu:20.04

ARG SQUIDCLAMAV_GIT_URL=https://github.com/darold/squidclamav.git
ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		git \
		c-icap \
		ca-certificates \
		patch \
		libicapapi5 \
		libicapapi-dev \
		libc-icap-mod-virus-scan \
		libc-dev \
		libssl-dev \
		gcc \
		make \
		file \
	&& apt-mark auto \
		git \
		ca-certificates \
		patch \
		libicapapi-dev \
		libc-dev \
		gcc \
		make \
		file \
\
	&& git clone --recursive "${SQUIDCLAMAV_GIT_URL}" "/usr/src/squidclamav" \
	&& (cd /usr/src/squidclamav \
		&& ./configure \
		&& make -j$(nproc) \
		&& make install \
	) \
	&& rm -rf /usr/src/squidclamav \
\
	&& apt-get autoremove --purge -y \
	&& apt-get clean \
	&& rm -rf /var/tmp/* /tmp/* /var/lib/apt/cache/* \
\
&& sed -i 's,clamd_local ,#clamd_local ,' /etc/c-icap/squidclamav.conf \
	&& sed -i 's,#clamd_ip .\+,clamd_ip clamav,' /etc/c-icap/squidclamav.conf \
	&& sed -i 's,#clamd_port ,clamd_port ,' /etc/c-icap/squidclamav.conf \
	&& (echo "acl all src 0.0.0.0/0.0.0.0" \
		&& echo "icap_access allow all" \
		&& echo "Include virus_scan.conf") >> /etc/c-icap/c-icap.conf \
\
&& echo "Include clamav_mod.conf" >> /etc/c-icap/virus_scan.conf \
\
&& freshclam 

COPY entrypoint.sh /usr/local/bin/docker-entrypoint
RUN chmod +x /usr/local/bin/* \
	&& sed -i 's,\r,,g' /usr/local/bin/*

ENTRYPOINT ["docker-entrypoint"]
