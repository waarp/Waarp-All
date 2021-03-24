Waarp All
=========

Waarp provides a secure and efficient open source MFT solution

Waarp Platform is a set of applications and tools specialized in managing and
monitoring a high number of transfers in a secure and reliable way.

It relies on its own open protocol named R66, which has been designed to
optimize file transfers, ensure the integrity of the data provide ways to
integrate transfers in larger business transactions.

Current applications are:
* Waarp R66: the transfer agent that implements the R66 protocol
  ([source](https://github.com/waarp/Waarp-All/tree/v3.2/WaarpR66))
* Waarp Gateway FTP: a service to interconnect FTP- and R66-based file exchanges
  ([source](https://github.com/waarp/Waarp-All/tree/v3.2/WaarpGatewayFtp))
* Waarp Password: a tool to generate the password files used by Waarp R66 and
  Waarp Gateway FTP
  ([source](https://github.com/waarp/Waarp-All/tree/v3.2/WaarpPassword))
* Waarp FTP: a fast and extensible FTP server based on Netty
  ([source](https://github.com/waarp/Waarp-All/tree/v3.2/WaarpFtp))

The following applications are deprecated and won't be maintained anymore:
- Waarp Administrator
- Waarp XmlEditor

## Features

* Supports Java 6-8
* Supports multiple databases: Postgresql, MySQL, MariaDB,  Oracle DB, H2
* Unlimited number of transfers
* Unlimited number of connections
* Traceability
* End-to-end security
* End-to-end integrity checks
* Virtualization of access path
* Encrypted connections with TLS
* Partners authentication (with login/password and/or strong TLS client
  authentication)
* Works in clusters
* REST API
* And much much more!


## Getting Started

### Build from source

Just clone the project and use Maven version 3.6.3 to build it.

*Even though Java 6 is supported at runtime, Java 8 or 11 is required to build the
project*

```sh
git clone https://github.com/waarp/Waarp-All.git
cd Waarp-All
mvn -P jre11 package
```

`mvn -P jre11 package` also runs the full test suite, which takes quite some time (for more
information about setting up your environment to run the tests, see below).

If you want to build the jars without running the tests, use the following
command instead:

```sh
mvn -P jre11 -D skipTests package 
```

After that, you will find the JARs for each module and application in their
respective `target` directory (ex: `./WaarpR66/target/WaarpR66-*.jar`)

### Installation

Detailed instructions are provided in the
[documentation](https://doc.waarp.org/waarp-r66/latest/fr/ ) (in French only for
now) to install Waarp R66 from portable archives ad OS packages (Deb, RPM).

### Run the tests

*Even though Java 6 is supported at runtime, Java 8 is required to build the
project*

The full test suite (including integration tests on several databases) requires
[Docker](https://www.docker.com).

From the root of the project, run the command:

```sh
mvn -P jre11 test
```

### Build the documentation

Documentation is managed with [Sphinx](https://www.sphinx-doc.org/). To build
it, you need Python:

```sh
cd doc/waarp-r66

# create a virtual environment (you need to do this only once
python -m virtualenv .venv

# Enter the virtual environment
. .venv/bin/activate

# Install the requirements
pip install -r requirement.txt

# Build the doc. After that, it will be available in the directory build/html
make html
```

## Support

[Waarp](https://www.Waarp.fr) provides professional support and services.

You can also have community support on [our forum](https://discuss.waarp.org).

## Related Projects

Those projects are part of the Waarp Platform, but are managed seperately:

- [Waarp Vitam](https://github.com/waarp/WaarpVitam)

Waarp solutions are built on the amazing [Netty](https://netty.io/) framework.

## License

This project is distributed under the terms of the [GNU GPLv3](LICENSE.txt) License

