#!/bin/bash
# Service accounts generated by Terraform and pulled from vault:
# https://github.com/broadinstitute/terraform-ap-deployments/blob/f26945d9d857e879f01671726188cecdc2d7fb10/terra-env/vault_crl_janitor.tf#L43
# TODO(PF-67): Find solution for piping configs and secrets.


VAULT_TOKEN=${1:-$(cat $HOME/.vault-token)}
DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:consul-0.20.0
VAULT_AZURE_MANAGED_APP_CLIENT_PATH=secret/dsde/terra/azure/common/managed-app-publisher
AZURE_MANAGED_APP_CLIENT_OUTPUT_FILE_PATH="$(dirname $0)"/src/test/resources/integration_azure_managed_app_client.json
AZURE_PROPERTIES_OUTPUT_FILE_PATH="$(dirname $0)"/src/test/resources/integration_azure_env.properties

docker run --rm --cap-add IPC_LOCK \
            -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
            vault read -format json ${VAULT_AZURE_MANAGED_APP_CLIENT_PATH} \
            | jq -r .data > ${AZURE_MANAGED_APP_CLIENT_OUTPUT_FILE_PATH}

# The docker image above doesn't have an image that will run on ARM architecture (such as the apple m1 chip)
# The command below is an alternative with an installed version of vault
# VAULT_ADDR="https://clotho.broadinstitute.org:8200"
# vault read -format=json "${VAULT_AZURE_MANAGED_APP_CLIENT_PATH}" | jq -r .data > ${AZURE_MANAGED_APP_CLIENT_OUTPUT_FILE_PATH}

AZURE_MANAGED_APP_CLIENT_ID=$(jq -r '."client-id"' ${AZURE_MANAGED_APP_CLIENT_OUTPUT_FILE_PATH})
AZURE_MANAGED_APP_CLIENT_SECRET=$(jq -r '."client-secret"' ${AZURE_MANAGED_APP_CLIENT_OUTPUT_FILE_PATH})
AZURE_MANAGED_APP_TENANT_ID=$(jq -r '."tenant-id"' ${AZURE_MANAGED_APP_CLIENT_OUTPUT_FILE_PATH})
cat > ${AZURE_PROPERTIES_OUTPUT_FILE_PATH} <<EOF
integration.azure.admin.clientId=${AZURE_MANAGED_APP_CLIENT_ID}
integration.azure.admin.clientSecret=${AZURE_MANAGED_APP_CLIENT_SECRET}
integration.azure.admin.tenantId=${AZURE_MANAGED_APP_TENANT_ID}
EOF
