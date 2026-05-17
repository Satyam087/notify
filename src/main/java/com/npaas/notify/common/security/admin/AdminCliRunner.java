package com.npaas.notify.common.security.admin;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AdminCliRunner implements ApplicationRunner {

    private static final String CREATE_API_KEY_COMMAND = "admin:create-api-key";

    private final TenantApiKeyAdminService tenantApiKeyAdminService;
    private final ConfigurableApplicationContext applicationContext;

    public AdminCliRunner(
            TenantApiKeyAdminService tenantApiKeyAdminService,
            ConfigurableApplicationContext applicationContext) {
        this.tenantApiKeyAdminService = tenantApiKeyAdminService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> nonOptionArgs = args.getNonOptionArgs();
        if (nonOptionArgs.isEmpty() || !CREATE_API_KEY_COMMAND.equals(nonOptionArgs.get(0))) {
            return;
        }

        try {
            String tenantSlug = requiredOption(args, "tenant");
            String keyName = requiredOption(args, "name");
            GeneratedTenantApiKey apiKey = tenantApiKeyAdminService.createApiKey(tenantSlug, keyName);
            printCreatedKey(tenantSlug, keyName, apiKey);
            applicationContext.close();
        } catch (RuntimeException exception) {
            System.err.println("Failed to create tenant API key: " + exception.getMessage());
            applicationContext.close();
            throw exception;
        }
    }

    private String requiredOption(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.isEmpty() || values.get(0).isBlank()) {
            throw new IllegalArgumentException("Missing required option --" + name);
        }

        return values.get(0).trim();
    }

    private void printCreatedKey(String tenantSlug, String keyName, GeneratedTenantApiKey apiKey) {
        System.out.println();
        System.out.println("Tenant API key created");
        System.out.println("----------------------");
        System.out.println("Tenant: " + tenantSlug);
        System.out.println("Name: " + keyName);
        System.out.println("Key ID: " + apiKey.id());
        System.out.println("Stored prefix: " + apiKey.keyPrefix());
        System.out.println();
        System.out.println("Copy this raw key now. It will not be shown again:");
        System.out.println(apiKey.rawKey());
        System.out.println();
    }
}
