terraform {
    required_version = ">= 0.14"
}

locals {
  slots = {
    active: azurerm_function_app.function_app
    candidate: azurerm_function_app_slot.candidate
  }

  all_app_settings = {
    "POSTGRES_USER" = var.postgres_user
    "POSTGRES_PASSWORD" = var.postgres_password

    "PRIME_ENVIRONMENT" = (var.environment == "prod" ? "prod" : "test")

    "OKTA_baseUrl" = "hhs-prime.okta.com"
    "OKTA_redirect" = var.okta_redirect_url

    # Manage client secrets via a Key Vault
    "CREDENTIAL_STORAGE_METHOD" ="AZURE"
    "CREDENTIAL_KEY_VAULT_NAME" = "${var.resource_prefix}-clientconfig"

    # Manage app secrets via a Key Vault
    "SECRET_STORAGE_METHOD" = "AZURE"
    "SECRET_KEY_VAULT_NAME" = "${var.resource_prefix}-appconfig"

    # Route outbound traffic through the VNET
    "WEBSITE_VNET_ROUTE_ALL" = 1

    # Route storage account access through the VNET
    "WEBSITE_CONTENTOVERVNET" = 1

    # Use the VNET DNS server (so we receive private endpoint URLs)
    "WEBSITE_DNS_SERVER" = "168.63.129.16"

    "DOCKER_REGISTRY_SERVER_URL" = var.login_server
    "DOCKER_REGISTRY_SERVER_USERNAME" = var.admin_user
    "DOCKER_REGISTRY_SERVER_PASSWORD" = var.admin_password
    "DOCKER_CUSTOM_IMAGE_NAME" = "${var.login_server}/${var.resource_prefix}:latest"

    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = false

    "APPINSIGHTS_INSTRUMENTATIONKEY" = var.ai_instrumentation_key

    "FEATURE_FLAG_SETTINGS_ENABLED" = true
  }
}

resource "azurerm_function_app" "function_app" {
  name = "${var.resource_prefix}-functionapp"
  location = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = var.app_service_plan_id
  storage_account_name = var.storage_account_name
  storage_account_access_key = var.storage_account_key
  https_only = true
  os_type = "linux"
  version = "~3"
  enable_builtin_logging = false

  site_config {
    ip_restriction {
      action = "Allow"
      name = "AllowVNetTraffic"
      priority = 100
      virtual_network_subnet_id = var.public_subnet_id
    }

    ip_restriction {
      action = "Allow"
      name = "AllowFrontDoorTraffic"
      priority = 110
      service_tag = "AzureFrontDoor.Backend"
    }

    ip_restriction {
      action = "Allow"
      name = "Ron IP"
      priority = 120
      ip_address = "165.225.48.87/31" # /31 is correct, can be 165.225.48.87 or 165.225.48.88
    }

    ip_restriction {
      action = "Allow"
      name = "Jim IP"
      priority = 130
      ip_address = "108.51.58.151/32"
    }

    scm_use_main_ip_restriction = true

    http2_enabled = true
    always_on = true
    use_32_bit_worker_process = false
    linux_fx_version = "DOCKER|${var.login_server}/${var.resource_prefix}:latest"

    cors {
      allowed_origins = [
        "https://${var.resource_prefix}public.z13.web.core.windows.net",
        "https://prime.cdc.gov",
        "https://${var.environment}.prime.cdc.gov",
        "https://reportstream.cdc.gov",
        "https://${var.environment}.reportstream.cdc.gov",
      ]
    }
  }

  app_settings = merge(local.all_app_settings, {
    "POSTGRES_URL" = var.postgres_url

    # HHS Protect Storage Account
    "PartnerStorage" = var.storage_partner_connection_string
  })

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_function_app_slot" "candidate" {
  function_app_name = azurerm_function_app.function_app.name
  name = "candidate"
  location = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = var.app_service_plan_id
  storage_account_name = var.storage_account_name
  storage_account_access_key = var.storage_account_key
  https_only = true
  os_type = "linux"
  version = "~3"
  enable_builtin_logging = false

  site_config {
    ip_restriction {
      action = "Allow"
      name = "AllowVNetTraffic"
      priority = 100
      virtual_network_subnet_id = var.public_subnet_id
    }

    ip_restriction {
      action = "Allow"
      name = "AllowFrontDoorTraffic"
      priority = 110
      service_tag = "AzureFrontDoor.Backend"
    }

    scm_use_main_ip_restriction = true

    http2_enabled = true
    always_on = true
    use_32_bit_worker_process = false
    linux_fx_version = "DOCKER|${var.login_server}/${var.resource_prefix}:latest"
  }

  app_settings = merge(local.all_app_settings, {
    "POSTGRES_URL" = var.postgres_url_candidate

    # HHS Protect Storage Account
    "PartnerStorage" = var.storage_partner_candidate_connection_string
  })

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }
}

// DISABLED AS FRONT DOOR CAN NOT CONNECT - RKH

//module "function_app_private_endpoint" {
//  source = "../common/private_endpoint"
//  resource_id = azurerm_function_app.function_app.id
//  name = azurerm_function_app.function_app.name
//  type = "function_app"
//  resource_group = var.resource_group
//  location = var.location
//  endpoint_subnet_id = var.endpoint_subnet_id
//}

resource "azurerm_app_service_virtual_network_swift_connection" "function_app_vnet_integration" {
  app_service_id = azurerm_function_app.function_app.id
  subnet_id = var.public_subnet_id
}

resource "azurerm_app_service_slot_virtual_network_swift_connection" "candidate_slot_vnet_integration" {
  slot_name = azurerm_function_app_slot.candidate.name
  app_service_id = azurerm_function_app.function_app.id
  subnet_id = var.public_subnet_id
}

module "functionapp_app_log_event_hub_log" {
  source = "../event_hub_log"
  resource_type = "function_app"
  log_type = "app"
  eventhub_namespace_name = var.eventhub_namespace_name
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
}

resource "azurerm_monitor_diagnostic_setting" "functionapp_app_log" {
  name = "${var.resource_prefix}-function_app-app-log"
  target_resource_id = azurerm_function_app.function_app.id
  eventhub_name = module.functionapp_app_log_event_hub_log.event_hub_name
  eventhub_authorization_rule_id = var.eventhub_manage_auth_rule_id

  log {
    category = "FunctionAppLogs"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  metric {
    category = "AllMetrics"
    enabled = false

    retention_policy {
      days = 0
      enabled = false
    }
  }
}
