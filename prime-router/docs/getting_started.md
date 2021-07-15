# Developers Getting Started

- [Developers Getting Started](#developers-getting-started)
  * [Developer Workstation Setup](#developer-workstation-setup)
    + [Pre-requisites](#pre-requisites)
      - [Mac or Linux OS](#mac-or-linux-os)
      - [Windows OS](#windows-os)
    + [PostgreSQL](#postgresql)
      - [Mac or Linux OS](#mac-or-linux-os-1)
      - [Windows OS](#windows-os-1)
      - [PostgreSQL via Docker](#postgresql-via-docker)
  * [Clone the Repository](#clone-the-repository)
  * [Compiling](#compiling)
  * [Function Development with Docker Compose](#function-development-with-docker-compose)
    + [Running the Router Locally](#running-the-router-locally)
  * [Testing](#testing)
    + [Unit Tests](#unit-tests)
    + [Local End-to-end Tests](#local-end-to-end-tests)
  * [Using local configuration for organizations.yml](#using-local-configuration-for-organizationsyml)
  * [Getting Around SSL Errors](#getting-around-ssl-errors)
    + [Docker Builds](#docker-builds)
  * [Managing the local Hashicorp Vault secrets database](#managing-the-local-hashicorp-vault-secrets-database)
    + [Initialize the Vault](#initialize-the-vault)
    + [Re-initialize the Vault](#re-initialize-the-vault)
    + [Using the Vault locally](#using-the-vault-locally)
  * [Troubleshooting](#troubleshooting)
    + [prime test Utility](#prime-test-utility)
      - [Missing env var](#missing-env-var) 


## Developer Workstation Setup

### Pre-requisites
#### Mac or Linux OS

1. Set up Java 11 and Gradle by opening up a Terminal session and
entering:
    ```
    brew install openjdk@11
    brew install gradle
    ```

2. Install Azure tools
    ```
    brew update && brew install azure-cli
    brew tap azure/functions
    brew install azure-functions-core-tools@3
    ```

3. Install the [Docker Desktop](https://www.docker.com/get-started) or the equivalent.

#### Windows OS
Install the following applications in your Windows workstation.  Note that you will require administrator privileges to install them

1. [OpenJDK 11 through OpenJDK 15](https://jdk.java.net/) - the Java virtual machine and development kit (*note* - OpenJDK@16 currently does not work with the application. Do not use it.)
1. [Git Bash](https://git-scm.com/download/win) - Git command line tools and Linux Bash shell
1. [Gradle](https://gradle.org/install/) - Tool for building and managing Java based projects.  Make sure to add the Gradle executable (`gradlew`) to the path
1. [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli-windows?tabs=azure-cli) - Azure command line tools
1. [Azure function core tools v3](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=windows%2Ccsharp%2Cbash#v2) - Azure core functions tools
1. [Docker Desktop](https://www.docker.com/get-started) - container management application


### PostgreSQL
You need a running local PostgreSQL database to **compile** and run the project locally.  Your local database will contain a database called prime_data_hub.  The credentials to access this database by the tests and build tools is set to prime/changeIT!.

#### Mac or Linux OS
##### PostgreSQL via Brew
One way is to use [brew](https://brew.sh) again to get this database. (Mac or Linux)
```
brew install postgresql@11
brew install flyway
brew services start postgresql@11

Add /usr/local/opt/postgresql@11/bin to your PATH

# Run createuser.  When prompted set the password as changeIT!
createuser -P prime
createdb --owner=prime prime_data_hub
```

If you need Flyway, you can install it via `brew` as above.

##### PostgreSQL via `apt` on Ubuntu/Debian
Installing PostgreSQL and Flyway on Ubuntu
```
sudo apt install postgresql
cd /usr/local/lib
wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/7.3.0/flyway-commandline-7.3.0-linux-x64.tar.gz | tar xvz && sudo ln -s `pwd`/flyway-7.3.0/flyway /usr/local/bin

# Run createuser - when prompted set the password as changeIT!
sudo -u postgres createuser -P prime
sudo -u postgres createdb --owner=prime prime_data_hub
```

If you need Flyway, you can install it via `apt` as above.

#### Windows OS
1. Install [PostgreSQL](https://www.enterprisedb.com/downloads/postgres-postgresql-downloads)
1. Start the SQL command line interface by running the following command at the command prompt and enter the `postgres` user password you set during installation:
    ```
    psql -u postgres
    ```
1. Enter the following SQL at the SQL prompt to create the necessary user and database:
    ```
    CREATE USER prime WITH PASSWORD 'changeIT!';
    CREATE DATABASE prime_data_hub WITH OWNER = prime;
    exit
    ```

#### PostgreSQL via Docker
In [`devenv-infrastructure.sh`](../devenv-infrastructure.sh)
```sh
docker-compose -f ./docker-infrastructure.yml up --detach
```

## Clone the Repository    
1. Use your favorite Git tool to clone the [PRIME ReportStream repository](https://github.com/CDCgov/prime-data-hub)
    - On Git Bash, use the command:
        ```
        git clone https://github.com/CDCgov/prime-data-hub.git
        ```
    - On other tools, you can clone from the URL https://github.com/CDCgov/prime-data-hub.git
1. Change the working directory to the `prime-router` directory.
    ```
    cd <your_path>/prime-router
    ```


## Compiling

Compile the project by running the following command:

```
./gradlew package
```

Other gradle tasks you can run are:
- clean - deletes the build artifacts
- compile - compile the code
- test - run the unit tests
- package - package the build artifacts for deployment
- primeCLI - run the prime CLI.  Specify arguments with --args='<args>'
- migrate - load the database with the current schema
- testEnd2End - run the end to end tests.  Requires the Docker container running

If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

Check out the database if you like:
```
psql prime_data_hub
    select * from task;
```

If you need to reload the database from scratch then use the following commands.  Note this uses the tool [Flyway](https://flywaydb.org/) to reload the database:
```
# drop and recreate the local database
dropdb prime_data_hub # to trash your local database
createdb --owner=prime prime_data_hub

# migrate the local database by hand
flyway -user=prime -password=changeIT! -url=jdbc:postgresql://localhost:5432/prime_data_hub -locations=filesystem:./src/main/resources/db/migration migrate
```

Use any other tools that you want to develop the code. Be productive. Modify this document if you have a practice that will be useful.

Some useful tools for Kotlin/Java development include:
- [KTLint](https://ktlint.github.io/) the Kotlin linter that we use to format our KT code
- [Microsoft VSCode](https://code.visualstudio.com/Download) with the available Kotlin extension
- [JetBrains IntelliJ](https://www.jetbrains.com/idea/download/#section=mac)

If you are using IntelliJ, you can install the [IntelliJ KLint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint-unofficial-) or configure it to follow standard Kotlin conventions by
```
cd prime_router
brew install ktlint
ktlint applyToIDEAProject
```

A useful Azure tool to examine Azurite and Azure storage is (Storage Explorer)[https://azure.microsoft.com/en-us/features/storage-explorer/] from Microsoft.

## Function Development with Docker Compose
### Running the Router Locally
The project's [README](../readme.md) file contains some steps to use the PRIME router in a CLI. However, for the POC app and most other users of the PRIME router will the router in the Microsoft Azure cloud. When hosted in Azure, the PRIME router uses Docker containers. The `DockerFile` describes how to build this container.

Developers can also run the router locally with the same Azure runtime and libraries to help develop and debug Azure code. In this case, a developer can use a local Azure storage emulator, called Azurite.

To orchestrate running the Azure function code and Azurite, Docker Compose is a useful tool. After installing or the equivalent, build the project using `Gradle` and then run the project in Docker containers using `docker-compose.`  Note: make sure Docker Desktop or equivalent is running before running the following commands.
```
mkdir -p .vault/env
touch .vault/env/.env.local
./gradlew package  
PRIME_ENVIRONMENT=local docker-compose up
```
Docker-compose will build a `prime_dev` container with the output of the `./gradlew package` command and launch an Azurite container. The first time you run this command, it builds a whole new image, which may take a while. However, after the first time `docker-compose` is run, `docker-compose` should start up in a few seconds. The output should look like:

![Docker Compose](assets/docker_compose_log.png)

Looking at the log above, you may notice that container has a debug open at port `5005`. This configuration allows you to attach a Java debugger to debug your code.

If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

## Updating Schema Documentation
Run the following Gradle command to generate the schema documentation.  The documentation is written to `docs/schema-documentation`

`./gradlew generateDocs`


## Testing
### Unit Tests
Unit tests are run as part of the build.  To run the unit tests, run the following command:
```
./gradlew test
```

Sometimes you want to force the unit tests to run.   You can do that with the -Pforcetest option, like one of these examples:
```
./gradlew test -P forcetest
./gradlew package -P forcetest
```

### Data Conversion Quick Test
The quick test is meant to test the data conversion and generation code.  Use the following command to run all quick tests.  On Windows OS, use Git Bash or similar Linux shell to run this command.
```
./quick-test.sh all
```

### Local End-to-end Tests
End-to-end tests check if the deployed system is configured correctly.  The test uses an organization called IGNORE for running the tests.  On Windows OS, use Git Bash or similar Linux shell to run these commands.
1. Perform a one-time setup of the required SFTP credentials for the test organization using the following commands.  Use the username and password assigned to the local SFTP server (default of foo/pass) and change the arguments for the --user and --pass as needed.  Note that running these commands multiple times will not break anything:
    ```bash
    export $(cat ./.vault/env/.env.local | xargs)
    ./gradlew primeCLI --args='create-credential --type=UserPass --persist=IGNORE--CSV --user foo --pass pass'
    ./gradlew primeCLI --args='create-credential --type=UserPass --persist=IGNORE--HL7 --user foo --pass pass'
    ./gradlew primeCLI --args='create-credential --type=UserPass --persist=IGNORE--HL7-BATCH --user foo --pass pass'
    ./gradlew primeCLI --args='create-credential --type=UserPass --persist=DEFAULT-SFTP --user foo --pass pass'
    ```
1. Run the Prime Router in the Docker container.
1. To run the test, run the following commands, replacing the value for Postgres URL, user and/or password as needed:
    ```bash
    ./gradlew testEnd2End
    ```
1. Verify that all tests are successful.

### Changing the Database Properties
You can change the default database properties used in the build script by setting the following properties:
- DB_USER - Postgres database username (defaults to prime)
- DB_PASSWORD - Postgres database password (defaults to changeIT!)
- DB_URL - Postgres database URL (defaults to jdbc:postgresql://localhost:5432/prime_data_hub)

In the command line, you can set these properties as follows:
```bash
./gradlew testEnd2End -PDB_USER=prime -PDB_PASSWORD=mypassword
```

Or you can specify these properties via environment variables per the Gradle project properties environment ORG_GRADLE_PROJECT_<property>.  For example:
```bash
export ORG_GRADLE_PROJECT_DB_USER=prime
export ORG_GRADLE_PROJECT_DB_PASSWORD=mypass
```

## Using local configuration for organizations.yml

By default, the functions will pull their configuration for organizations from the `organizations.yml` file.  You can override this locally or in test by declaring an environment variable `PRIME_ENVIRONMENT`.  If you declare something like, `export PRIME_ENVIRONMENT=mylocal` then the system will look for a configuration file `organizations-mylocal.yml` and will use that, even if the `organizations.yml` file exists.  In this way, you can set up local SFTP routing, etc. without impacting the production (`organizations.yml`) config.  Note that depending on the OS - case matters.

## Getting Around SSL Errors

If your agency's network intercepts SSL requests, you might have to disable SSL verifications to get around invalid certificate errors.


### Docker Builds

This can be accomplished by setting an environment variable `PRIME_DATA_HUB_INSECURE_SSL=true`. You can pass this in as a one-off when you build a component, for example:

```bash
PRIME_DATA_HUB_INSECURE_SSL=true docker-compose up
```

Or you can add this line in your `~/.bash_profile` to ensure your local builds will always disable SSL verification:

```bash
export PRIME_DATA_HUB_INSECURE_SSL=true
```

## Managing the local Hashicorp Vault secrets database

Our `docker-compose.yml` includes Hashicorp Vault alongside our other containers to enable local secrets storage. Under normal circumstances, developers will not have to interact directly with the Vault configuration, but some helpful guidance is provided below for troubleshooting.

### Initialize the Vault

Run the following commands to initialize vault:
```bash
mkdir -p .vault/env
touch .vault/env/.env.local
```
When starting up our containers with `docker-compose up` on first-run, the container will create a new Vault database and store the following files in `.vault/env`:

* `key` - unseal key for decrypting the database
* `.env.local` - the root token in envfile format for using the Vault api / command line

The database is stored in a docker-compose container `vault` that persists across up and down events. All files are excluded in `.gitignore` and should never be persisted to source control.

### Re-initialize the Vault

If you would like to start with a fresh Vault database, you can clear the Vault database with the following commands:

```bash
cd prime_router
docker-compose down -v
rm -rf .vault/env/{key,.env.local}
docker volume rm prime-router_vault # removes vault volume
touch .vault/env/.env.local
```

Note: The `docker-compose down -v` option deletes all volumes associated with our docker-compose file.

### Using the Vault locally

Our `docker-compose.yml` will automatically load the environment variables needed for the Vault. If you need to use the Vault outside Docker, you can find the environment variables you need in:

```
.vault/env/.env.local
```

They can automatically be loaded in most IDEs:
- IntelliJ: https://plugins.jetbrains.com/plugin/7861-envfile
- VSCode: https://dev.to/andreasbergstrom/placeholder-post-1klo

Alternatively, inject them in your terminal with (useful for using the CLI):

```bash
export $(cat ./.vault/env/.env.local | xargs)
```


## TroubleShooting

### prime test Utility

The prime-router comes packaged with a executable that can help in finding misconfigurations and other problems with the appliciation.

Use the following command to launch the tool locally: 

```shell
cd prime-router
export POSTGRES_PASSWORD='changeIT!'
export POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
export POSTGRES_USER=prime
./prime test
```

This can be used while the prime-router application is running on your system.
