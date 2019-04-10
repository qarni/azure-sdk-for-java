/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.consumption.v2019_01_01.implementation;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.AzureResponseBuilder;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.apigeneration.Beta;
import com.microsoft.azure.management.apigeneration.Beta.SinceVersion;
import com.microsoft.azure.arm.resources.AzureConfigurable;
import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.rest.RestClient;
import com.microsoft.azure.management.consumption.v2019_01_01.UsageDetails;
import com.microsoft.azure.management.consumption.v2019_01_01.Marketplaces;
import com.microsoft.azure.management.consumption.v2019_01_01.Budgets;
import com.microsoft.azure.management.consumption.v2019_01_01.Tags;
import com.microsoft.azure.management.consumption.v2019_01_01.Charges;
import com.microsoft.azure.management.consumption.v2019_01_01.Balances;
import com.microsoft.azure.management.consumption.v2019_01_01.ReservationsSummaries;
import com.microsoft.azure.management.consumption.v2019_01_01.ReservationsDetails;
import com.microsoft.azure.management.consumption.v2019_01_01.ReservationRecommendations;
import com.microsoft.azure.management.consumption.v2019_01_01.PriceSheets;
import com.microsoft.azure.management.consumption.v2019_01_01.Forecasts;
import com.microsoft.azure.management.consumption.v2019_01_01.Operations;
import com.microsoft.azure.management.consumption.v2019_01_01.AggregatedCosts;
import com.microsoft.azure.arm.resources.implementation.AzureConfigurableCoreImpl;
import com.microsoft.azure.arm.resources.implementation.ManagerCore;

/**
 * Entry point to Azure Consumption resource management.
 */
public final class ConsumptionManager extends ManagerCore<ConsumptionManager, ConsumptionManagementClientImpl> {
    private UsageDetails usageDetails;
    private Marketplaces marketplaces;
    private Budgets budgets;
    private Tags tags;
    private Charges charges;
    private Balances balances;
    private ReservationsSummaries reservationsSummaries;
    private ReservationsDetails reservationsDetails;
    private ReservationRecommendations reservationRecommendations;
    private PriceSheets priceSheets;
    private Forecasts forecasts;
    private Operations operations;
    private AggregatedCosts aggregatedCosts;
    /**
    * Get a Configurable instance that can be used to create ConsumptionManager with optional configuration.
    *
    * @return the instance allowing configurations
    */
    public static Configurable configure() {
        return new ConsumptionManager.ConfigurableImpl();
    }
    /**
    * Creates an instance of ConsumptionManager that exposes Consumption resource management API entry points.
    *
    * @param credentials the credentials to use
    * @param subscriptionId the subscription UUID
    * @return the ConsumptionManager
    */
    public static ConsumptionManager authenticate(AzureTokenCredentials credentials, String subscriptionId) {
        return new ConsumptionManager(new RestClient.Builder()
            .withBaseUrl(credentials.environment(), AzureEnvironment.Endpoint.RESOURCE_MANAGER)
            .withCredentials(credentials)
            .withSerializerAdapter(new AzureJacksonAdapter())
            .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
            .build(), subscriptionId);
    }
    /**
    * Creates an instance of ConsumptionManager that exposes Consumption resource management API entry points.
    *
    * @param restClient the RestClient to be used for API calls.
    * @param subscriptionId the subscription UUID
    * @return the ConsumptionManager
    */
    public static ConsumptionManager authenticate(RestClient restClient, String subscriptionId) {
        return new ConsumptionManager(restClient, subscriptionId);
    }
    /**
    * The interface allowing configurations to be set.
    */
    public interface Configurable extends AzureConfigurable<Configurable> {
        /**
        * Creates an instance of ConsumptionManager that exposes Consumption management API entry points.
        *
        * @param credentials the credentials to use
        * @param subscriptionId the subscription UUID
        * @return the interface exposing Consumption management API entry points that work across subscriptions
        */
        ConsumptionManager authenticate(AzureTokenCredentials credentials, String subscriptionId);
    }

    /**
     * @return Entry point to manage UsageDetails.
     */
    public UsageDetails usageDetails() {
        if (this.usageDetails == null) {
            this.usageDetails = new UsageDetailsImpl(this);
        }
        return this.usageDetails;
    }

    /**
     * @return Entry point to manage Marketplaces.
     */
    public Marketplaces marketplaces() {
        if (this.marketplaces == null) {
            this.marketplaces = new MarketplacesImpl(this);
        }
        return this.marketplaces;
    }

    /**
     * @return Entry point to manage Budgets.
     */
    public Budgets budgets() {
        if (this.budgets == null) {
            this.budgets = new BudgetsImpl(this);
        }
        return this.budgets;
    }

    /**
     * @return Entry point to manage Tags.
     */
    public Tags tags() {
        if (this.tags == null) {
            this.tags = new TagsImpl(this);
        }
        return this.tags;
    }

    /**
     * @return Entry point to manage Charges.
     */
    public Charges charges() {
        if (this.charges == null) {
            this.charges = new ChargesImpl(this);
        }
        return this.charges;
    }

    /**
     * @return Entry point to manage Balances.
     */
    public Balances balances() {
        if (this.balances == null) {
            this.balances = new BalancesImpl(this);
        }
        return this.balances;
    }

    /**
     * @return Entry point to manage ReservationsSummaries.
     */
    public ReservationsSummaries reservationsSummaries() {
        if (this.reservationsSummaries == null) {
            this.reservationsSummaries = new ReservationsSummariesImpl(this);
        }
        return this.reservationsSummaries;
    }

    /**
     * @return Entry point to manage ReservationsDetails.
     */
    public ReservationsDetails reservationsDetails() {
        if (this.reservationsDetails == null) {
            this.reservationsDetails = new ReservationsDetailsImpl(this);
        }
        return this.reservationsDetails;
    }

    /**
     * @return Entry point to manage ReservationRecommendations.
     */
    public ReservationRecommendations reservationRecommendations() {
        if (this.reservationRecommendations == null) {
            this.reservationRecommendations = new ReservationRecommendationsImpl(this);
        }
        return this.reservationRecommendations;
    }

    /**
     * @return Entry point to manage PriceSheets.
     */
    public PriceSheets priceSheets() {
        if (this.priceSheets == null) {
            this.priceSheets = new PriceSheetsImpl(this);
        }
        return this.priceSheets;
    }

    /**
     * @return Entry point to manage Forecasts.
     */
    public Forecasts forecasts() {
        if (this.forecasts == null) {
            this.forecasts = new ForecastsImpl(this);
        }
        return this.forecasts;
    }

    /**
     * @return Entry point to manage Operations.
     */
    public Operations operations() {
        if (this.operations == null) {
            this.operations = new OperationsImpl(this);
        }
        return this.operations;
    }

    /**
     * @return Entry point to manage AggregatedCosts.
     */
    public AggregatedCosts aggregatedCosts() {
        if (this.aggregatedCosts == null) {
            this.aggregatedCosts = new AggregatedCostsImpl(this);
        }
        return this.aggregatedCosts;
    }

    /**
    * The implementation for Configurable interface.
    */
    private static final class ConfigurableImpl extends AzureConfigurableCoreImpl<Configurable> implements Configurable {
        public ConsumptionManager authenticate(AzureTokenCredentials credentials, String subscriptionId) {
           return ConsumptionManager.authenticate(buildRestClient(credentials), subscriptionId);
        }
     }
    private ConsumptionManager(RestClient restClient, String subscriptionId) {
        super(
            restClient,
            subscriptionId,
            new ConsumptionManagementClientImpl(restClient).withSubscriptionId(subscriptionId));
    }
}
