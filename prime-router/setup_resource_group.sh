#!/bin/bash
# This script sets up a developers resource group as explained in the "Deploying to Azure"
# in the getting startedy

confirm() {
    # call with a prompt string or use a default
    read -r -p "${1} [y/N] " response
    case "$response" in
        [yY][eE][sS]|[yY])
            true
            ;;
        *)
            exit 1
            ;;
    esac
}

# Check the PRIME_DEV_NAME
if [ -z "${PRIME_DEV_NAME}" ]; then
  echo "PRIME_DEV_NAME must be set. Example: export PRIME_DEV_NAME rhawes. Suggest sticking this in your shell profile"
  exit 1
fi

# Set variable names
if [[ $PRIME_DEV_NAME == prime-data-hub-* ]]
then
  resource_group=${PRIME_DEV_NAME}
  storage_account=${PRIME_DEV_NAME//[-]/}
else
  resource_group=prime-dev-${PRIME_DEV_NAME}
  storage_account=${PRIME_DEV_NAME}primedev
fi

registry=${PRIME_DEV_NAME//[-]/}PrimeDevRegistry
plan=${PRIME_DEV_NAME//[-]/}PrimeDevPlan
registry_lc=$(echo "$registry" | tr '[A-Z]' '[a-z]')
app_name=prime-data-hub
image="$registry_lc".azurecr.io/"$app_name"
full_app_name="${PRIME_DEV_NAME}"-"$app_name"

# Check the resource group
resource_group_exists=$(az group exists --resource-group "$resource_group")
if [ "$resource_group_exists" != "true" ]; then
  echo "$resource_group" does not exist or you are not logged into Azure
  exit 1
fi
echo "Using the $resource_group resource group"

# Create the storage account
confirm "Create a $storage_account Azure storage account in your resource group?"
az storage account create --name "$storage_account" --location eastus --resource-group "$resource_group" --sku Standard_LRS

# Create a container registry
confirm "Create an Azure container registry in your resource group?"
az acr create --resource-group "$resource_group" --name "$registry" --sku Basic --admin-enabled true

# Create a subscription plan
confirm "Create an Azure an elastic plan for your function?"
az functionapp plan create --resource-group "$resource_group" --name "$plan" --location eastus --number-of-workers 1 --sku B1 --is-linux

# Build the a docker image
confirm "Build a Docker image with tag of $image? Warning: this will pull down a lot of stuff "
docker build --tag "$image" .

# Login to your registry
confirm "Login to your container registry?"
az acr login --name "$registry_lc"

# Push the docker image to you
confirm "Push the docker image to your container registry?"
docker push "$image"

# Create the function app
confirm "Create a $full_app_name function app with the image you just pushed"
az functionapp create \
   --name "$full_app_name" \
   --functions-version 3 \
   --storage-account "$storage_account" \
   --resource-group "$resource_group" \
   --plan "$plan" \
   --runtime java \
   --runtime-version 11 \
   --deployment-container-image-name "$image":latest

# Setup a web hook to between the function and the registry
confirm "Create a web hook to automatically deploy your containers when you push new containers?"
webhook=$(az functionapp deployment container config --enable-cd --query CI_CD_URL --output tsv --name "$full_app_name" --resource-group "$resource_group")
az acr webhook create --actions push \
                      --name primeDataHub \
                      --registry "$registry" \
                      --uri  "$webhook" \
                      --resource-group "$resource_group" \
                      --scope "$app_name":latest

echo All done
echo Now try running test-ingest.sh
