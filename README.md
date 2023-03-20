# Attini CLI

This project contains the source code for the Attini CLI, which is used
to work with the [Attini framework](https://github.com/attini-cloud-solutions/attini-framework).

The Attini CLI is built using [Micronaut](https://micronaut.io/) (GraalVM) and [picocli](https://picocli.info/).

## Installation

To install the pre-built CLI provided by Attini, use this command:

```bash
/bin/bash -c "$(curl -fsSL https://docs.attini.io/blob/attini-cli/install-cli.sh)"
```

Find more details [here](https://docs.attini.io/getting-started/installations/cli.html)

## Build your own CLI
Clone the repository and run the command:

```bash
make build-native
```

Now you will find the CLI at `target/attini`.

To install the CLI on your machine, you need to put the CLI somewhere on your system path and give it executable permission. For example:

```bash
cp target/attini /usr/local/bin/attini
chmod +x /usr/local/bin/attini
```

The CLI you built will work on your machine.

If it's built on Linux, it will work on all other Linux machines with the same CPU architecture that you have.

If you build it on Mac, you will need to sign it if you want to share it.