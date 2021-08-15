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
  ([source](https://github.com/waarp/Waarp-All/tree/v3.6/WaarpR66))
* Waarp Gateway FTP: a service to interconnect FTP- and R66-based file exchanges
  ([source](https://github.com/waarp/Waarp-All/tree/v3.6/WaarpGatewayFtp))
* Waarp Password: a tool to generate the password files used by Waarp R66 and
  Waarp Gateway FTP
  ([source](https://github.com/waarp/Waarp-All/tree/v3.6/WaarpPassword))
* Waarp FTP: a fast and extensible FTP server based on Netty
  ([source](https://github.com/waarp/Waarp-All/tree/v3.6/WaarpFtp))

The following applications are deprecated and won't be maintained anymore:
- Waarp Administrator
- Waarp XmlEditor

## Features

* Supports Java 6, 8 and 11 in different Jars (JRE 11 is compatible up to JRE 16)
* Supports multiple databases: Postgresql (recommended), MySQL, MariaDB,  Oracle DB, H2
* Unlimited number of transfers
* Unlimited number of connections
* Traceability
* End-to-end security
* End-to-end integrity checks
* Virtualization of access path
* Encrypted connections with TLS
* Partners authentication (with login/password and/or strong TLS client
  authentication)
* Pre and Post actions, and Error actions, both on sender and receiver sides
* Works in clusters
* REST API
* Support for ICAP servers submission (antivirus for example)
* Support for S3 access with GET, PUT and DELETE operations
* And much much more!


## Getting Started

### Build from source

Just clone the project and use Maven version 3.6.3 minimum to build it (3.8.x for
JRE > 11).

*Even though Java 6 is supported at runtime, Java 8 or 11 is required to build the
project (JDK 11 can be replaced by a newer version, tested until 16 but with maven 3.8.x)*

```sh
git clone https://github.com/waarp/Waarp-All.git
cd Waarp-All
mvn -P jre11 package
```

If you want to build unofficial RPM/DEV/TGZ/ZIP and documentation, you can do as the following,
ensuring you have already cloned and install using `pip` the repo for Sphinx template for Waarp
`code.waarp.fr:2222/waarp/sphinx-template.git` with the following packages for Sphinx:

- sphinx
- sphinx-autobuild
- sphinxcontrib-httpdomain
  - Possibly fix the current version 1.6 to 1.7
    - `sphinxcontrib/httpdomain.py`
      - line 766
      - `+ app.add_domain(HTTPDomain)`
- sphinxcontrib-openapi
- sphinx.ext.todo

```sh
mvn -P jre11,release package
```

You can use a JDK 11 (or higher) with `jre11` profile, and a JDK 8 with `jre8` or `jre6` profiles.

`mvn -P jre11 package` also runs the full test suite, which takes quite some time (for more
information about setting up your environment to run the tests, see below).

If you want to build the jars without running the tests, use the following
command instead:

```sh
mvn -p jre11 -D skipTests package
```

After that, you will find the JARs for each module and application in their
respective `target` directory (ex: `./WaarpR66/target/WaarpR66-*.jar`)

And moreover, you will have also a shaded jar that include all dependencies,
except Oracle JDBC for licence issue, under name `WaarpR66-X.Y.Z-jar-with-depencencies.jar`, or
equivalent for all runnable jars.

A detailed documentation (in French) is also available in the directory `doc/releasing.md`.


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

Note that documentations are built in final module named `WaarpPackaging` and `WaarpPackaging6`.

## Support

[Waarp](https://www.Waarp.fr) provides professional support and services.

You can also have community support on [our forum](https://discuss.waarp.org).

## Related Projects

Those projects are part of the Waarp Platform, but are managed seperately:

- [Waarp Vitam](https://github.com/waarp/WaarpVitam)

Waarp solutions are built on the amazing [Netty](https://netty.io/) framework.

## License

This project is distributed under the terms of the [GNU GPLv3](LICENSE.txt) License

