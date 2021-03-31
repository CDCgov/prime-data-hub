variable "environment" {
    type = string
    description = "Target Environment"
}

variable "resource_group" {
    type = string
    description = "Resource Group Name"
}

variable "resource_prefix" {
    type = string
    description = "Resource Prefix"
}

variable "name" {
    type = string
    description = "Storage Account Name"
}

variable "location" {
    type = string
    description = "Storage Account Location"
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}

variable "endpoint_subnet_id" {
    type = string
    description = "Private Endpoint Subnet ID"
}

variable "eventhub_namespace_name" {
    type = string
    description = "Event hub to stream logs to"
}

variable "eventhub_manage_auth_rule_id" {
    type = string
    description = "Event Hub Manage Authorization Rule ID"
}