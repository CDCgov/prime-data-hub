SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

TF_ENV = "dev" #VAR Your target environment (e.g. dev, test, staging, prod)
TF_STAGE = ""  # not surfaced
TF_CMD = ""  # not surfaced

AZ_RESOURCE_PREFIX = "pdh$(TF_ENV)" ## VAR Azure environment resource prefix (e.g. pdhtest)
AZ_RESOURCE_GROUP = "prime-data-hub-$(TF_ENV)" ## VAR Azure resource group (e.g. prime-data-hub-staging)

ZDD_SOURCE_SLOT = "candidate" #VAR Source function app slot for ZDD deployment
ZDD_TARGET_SLOT = "production" #VAR Destination function app slot to deploy to

# HELP
# This will output the help for each task
# thanks to https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
.PHONY: help tf-cli tf-cmd tf-01-network tf-02-config tf-03-persistent tf-04-app tf-validate zdd-promote-slot

help: ## This help
	@echo "Invoke any of the following targets"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sed "s/^/    - /g" | sort
	@echo ""
	@echo "When invoking, you can specify the following variables on invocation"
	@awk 'BEGIN {FS = " = .*?#VAR "} /^[a-zA-Z0-9_-]+ = .*?#VAR / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sed "s/^/    - /g" | sort
	@echo ""
	@echo "Example: "
	@echo "    make TF_ENV=\"test\" all"
	@echo "    make TF_ENV=\"dev\" AZ_RESOURCE_PREFIX=\"rkh5012\" zdd-promote-slot"
	@echo ""

.DEFAULT_GOAL := help


# TERRAFORM

build-tf: ## Build the Terraform docker container
	@docker-compose build

tf-cli: build-tf ## Drops into a Docker container configured with the Terraform CLI and Azure
	docker-compose run -e ENVIRONMENT=$(TF_ENV) -w /app/src/environments/$(TF_STAGE) tf-cli

tf-cmd: build-tf ## Runa a command in the Docker container configured with the Terraform CLI and Azure
	docker-compose run -e ENVIRONMENT=$(TF_ENV) -w /app/src/environments/$(TF_STAGE) --entrypoint "${TF_CMD}" tf-cli

tf-01-network: ## Preconfigures Terraform network environment module
	${MAKE} tf-cli TF_STAGE="01-network"

tf-02-config: ## Preconfigures Terraform config environment module
	${MAKE} tf-cli TF_STAGE="02-config"

tf-03-persistent: ## Preconfigures Terraform persistent environment module
	${MAKE} tf-cli TF_STAGE="03-persistent"

tf-04-app: ## Preconfigures Terraform app environment module
	${MAKE} tf-cli TF_STAGE="04-app"

tf-validate: ## Validates the Terraform configuration across all modules
	${MAKE} tf-cmd TF_STAGE="01-network" TF_CMD="tf validate"
	${MAKE} tf-cmd TF_STAGE="02-config" TF_CMD="tf validate"
	${MAKE} tf-cmd TF_STAGE="03-persistent" TF_CMD="tf validate"
	${MAKE} tf-cmd TF_STAGE="04-app" TF_CMD="tf validate"


# AZURE

# Internal target: check if we are currently logged in, so we get a friendly error if not
.az-logged-in:
	@if ! az account show >& /dev/null ; then echo "You must be logged in to the az command line"; exit 1; fi

# Internal target: check if the passed-in wildcard is a known environment name. Hard-coding them because let's be real here.
.az-valid-env:
	@case $(TF_ENV) in rheft|staging|prod) ;; *) echo "$* is not a valid environment"; exit 1;; esac

zdd-promote-slot: .az-logged-in .az-valid-env ## Promote the function app through a ZDD slot swap
	az functionapp deployment slot swap -g $(AZ_RESOURCE_GROUP) -n $(AZ_RESOURCE_PREFIX)-functionapp --slot $(ZDD_SOURCE_SLOT) --target-slot $(ZDD_TARGET_SLOT)