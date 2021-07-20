package gov.cdc.prime.router.azure

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.Tables.COVID_RESULT_METADATA
import gov.cdc.prime.router.azure.db.Tables.EMAIL_SCHEDULE
import gov.cdc.prime.router.azure.db.Tables.REPORT_LINEAGE
import gov.cdc.prime.router.azure.db.Tables.SETTING
import gov.cdc.prime.router.azure.db.Tables.TASK
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.ReportFile.REPORT_FILE
import gov.cdc.prime.router.azure.db.tables.pojos.CovidResultMetadata
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Setting
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.azure.db.tables.records.CovidResultMetadataRecord
import gov.cdc.prime.router.azure.db.tables.records.TaskRecord
import java.sql.Connection
import java.sql.DriverManager
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import org.apache.logging.log4j.kotlin.Logging
import org.flywaydb.core.Flyway
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.JSON
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DSL.inline
import org.postgresql.Driver

const val databaseVariable = "POSTGRES_URL"
const val userVariable = "POSTGRES_USER"
const val passwordVariable = "POSTGRES_PASSWORD"

typealias DataAccessTransaction = Configuration

/**
 * A data access layer for the database. The idea or abstraction is CRUD on tables in the database.
 * The interface uses the POJO abstractions of the database tables.
 *
 * The companion object does the connection pooling and settings.
 */
class DatabaseAccess(private val create: DSLContext) : Logging {
    constructor(
            dataSource: DataSource = commonDataSource
    ) : this(DSL.using(dataSource, SQLDialect.POSTGRES))
    constructor(connection: Connection) : this(DSL.using(connection, SQLDialect.POSTGRES))

    fun checkConnection() {
        create.selectFrom(REPORT_FILE).where(REPORT_FILE.REPORT_ID.eq(UUID.randomUUID())).fetch()
    }

    /** Make the other calls in the context of a SQL transaction */
    fun transact(block: (txn: DataAccessTransaction) -> Unit) {
        create.transaction { txn: Configuration -> block(txn) }
    }

    /** Make the other calls in the context of a SQL transaction, returning a result */
    fun <T> transactReturning(block: (txn: DataAccessTransaction) -> T): T {
        return create.transactionResult { txn: Configuration -> block(txn) }
    }

    /*
     * Task queries
     */

    /** Fetch a task record and lock it so other connections can grab it */
    fun fetchAndLockTask(reportId: ReportId, txn: DataAccessTransaction): Task {
        return DSL.using(txn)
                .selectFrom(TASK)
                .where(TASK.REPORT_ID.eq(reportId))
                .forUpdate()
                .fetchOne()
                ?.into(Task::class.java)
                ?: error("Could not find $reportId that matches a task")
    }

    /** Fetch multiple task records and lock them so other connections can not grab them */
    fun fetchAndLockTasks(
            nextAction: TaskAction,
            at: OffsetDateTime?,
            receiverFullName: String,
            limit: Int,
            txn: DataAccessTransaction
    ): List<Task> {
        val cond =
                if (at == null) {
                    TASK.RECEIVER_NAME.eq(receiverFullName).and(TASK.NEXT_ACTION.eq(nextAction))
                } else {
                    TASK.RECEIVER_NAME
                            .eq(receiverFullName)
                            .and(TASK.NEXT_ACTION.eq(nextAction))
                            .and(TASK.NEXT_ACTION_AT.eq(at))
                }
        return DSL.using(txn)
                .selectFrom(TASK)
                .where(cond)
                .limit(limit)
                .forUpdate()
                .skipLocked() // Allows the same query to run in parallel. Otherwise, the query
                // would lock the table.
                .fetch()
                .into(Task::class.java)
    }

    fun fetchTask(reportId: ReportId): Task {
        return create.selectFrom(TASK)
                .where(TASK.REPORT_ID.eq(reportId))
                .fetchOne()
                ?.into(Task::class.java)
                ?: error("Could not find $reportId that matches a task")
    }

    /** Take a report and put into the database after already serializing the body of the report */
    fun insertTask(
            report: Report,
            bodyFormat: String,
            bodyUrl: String,
            nextAction: Event,
            txn: DataAccessTransaction? = null,
    ) {
        fun insert(txn: Configuration) {
            val task = createTaskRecord(report, bodyFormat, bodyUrl, nextAction)
            DSL.using(txn).executeInsert(task)
        }

        if (txn != null) {
            insert(txn)
        } else {
            create.transaction { innerTxn -> insert(innerTxn) }
        }
    }

    fun updateTask(
            reportId: ReportId,
            nextAction: TaskAction,
            nextActionAt: OffsetDateTime?,
            retryToken: String?,
            finishedField: Field<OffsetDateTime>,
            txn: DataAccessTransaction?
    ) {
        val ctx = if (txn != null) DSL.using(txn) else create
        ctx.update(TASK)
                .set(TASK.NEXT_ACTION, nextAction)
                .set(TASK.NEXT_ACTION_AT, nextActionAt)
                .set(TASK.RETRY_TOKEN, if (retryToken != null) JSON.valueOf(retryToken) else null)
                .set(finishedField, OffsetDateTime.now())
                .where(TASK.REPORT_ID.eq(reportId))
                .execute()
    }

    /*
     * ActionHistory queries
     */

    /** You should include org as a search criteria to enforce authorization to get that report. */
    fun fetchReportFile(
            reportId: ReportId,
            org: Organization? = null,
            txn: DataAccessTransaction? = null
    ): ReportFile {
        val ctx = if (txn != null) DSL.using(txn) else create
        val cond =
                if (org == null) {
                    Tables.REPORT_FILE.REPORT_ID.eq(reportId)
                } else {
                    Tables.REPORT_FILE
                            .REPORT_ID
                            .eq(reportId)
                            .and(Tables.REPORT_FILE.RECEIVING_ORG.eq(org.name))
                }
        return ctx.selectFrom(Tables.REPORT_FILE)
                .where(cond)
                .fetchOne()
                ?.into(ReportFile::class.java)
                ?: error(
                        "Could not find $reportId in REPORT_FILE" +
                                if (org != null) {
                                    " associated with organization ${org.name}"
                                } else ""
                )
    }

    fun fetchAllInternalReports(
            createdDateTime: OffsetDateTime? = null,
            txn: DataAccessTransaction? = null
    ): List<ReportFile> {
        val createdDt = createdDateTime ?: OffsetDateTime.now().minusDays(30)
        val ctx = if (txn != null) DSL.using(txn) else create
        val cond =
                Tables.REPORT_FILE
                        .SENDING_ORG
                        .isNotNull
                        .and(Tables.REPORT_FILE.BODY_FORMAT.eq("INTERNAL"))
                        .and(Tables.REPORT_FILE.CREATED_AT.ge(createdDt))
        return ctx.selectFrom(Tables.REPORT_FILE).where(cond).fetchArray().map {
            it.into(ReportFile::class.java)
        }
    }

    /** Returns null if report has no item-level lineage info tracked. */
    fun fetchItemLineagesForReport(
            reportId: ReportId,
            itemCount: Int,
            txn: DataAccessTransaction? = null
    ): List<ItemLineage>? {
        val ctx = if (txn != null) DSL.using(txn) else create
        val itemLineages =
                ctx.selectFrom(Tables.ITEM_LINEAGE)
                        .where(Tables.ITEM_LINEAGE.CHILD_REPORT_ID.eq(reportId))
                        .orderBy(
                                Tables.ITEM_LINEAGE.CHILD_INDEX
                        ) // todo Don't know if this will be too slow?  Use a map in mem?
                        .fetch()
                        .into(ItemLineage::class.java)
                        .toList()
        // sanity check.  If there are lineages, every record up to itemCount should have at least
        // one lineage.
        // OK to have more than one lineage.  Eg, a merge.
        if (itemLineages.isEmpty()) {
            return null
        } else {
            if (itemLineages.size < itemCount)
                    error(
                            "For $reportId, must have at least $itemCount item lineages. There were ${itemLineages.size}"
                    )
            val uniqueIndexCount = itemLineages.map { it.childIndex }.toSet().size
            if (uniqueIndexCount != itemCount)
                    error(
                            "For report $reportId, expected $itemCount unique indexes; there were $uniqueIndexCount"
                    )
        }
        return itemLineages
    }

    fun fetchDownloadableReportFiles(
            since: OffsetDateTime?,
            orgName: String,
            txn: DataAccessTransaction? = null,
    ): List<ReportFile> {
        val ctx = if (txn != null) DSL.using(txn) else create
        val cond =
                if (since == null) {
                    Tables.REPORT_FILE
                            .RECEIVING_ORG
                            .eq(orgName)
                            .and(Tables.REPORT_FILE.NEXT_ACTION.eq(TaskAction.send))
                } else {
                    Tables.REPORT_FILE
                            .RECEIVING_ORG
                            .eq(orgName)
                            .and(Tables.REPORT_FILE.NEXT_ACTION.eq(TaskAction.send))
                            .and(Tables.REPORT_FILE.CREATED_AT.ge(since))
                }

        return ctx.selectFrom(Tables.REPORT_FILE)
                .where(cond)
                .fetch()
                .into(ReportFile::class.java)
                .toList()
    }

    fun fetchChildReports(
            parentReportId: UUID,
            txn: DataAccessTransaction? = null,
    ): List<ReportId> {
        val ctx = if (txn != null) DSL.using(txn) else create
        return ctx.select(REPORT_LINEAGE.CHILD_REPORT_ID)
                .from(REPORT_LINEAGE)
                .where(REPORT_LINEAGE.PARENT_REPORT_ID.eq(parentReportId))
                .fetch()
                .into(ReportId::class.java)
                .toList()
    }

    /** Settings queries */
    fun fetchSetting(
            type: SettingType,
            name: String,
            parentId: Int?,
            txn: DataAccessTransaction
    ): Setting? {
        return DSL.using(txn)
                .selectFrom(SETTING)
                .where(
                        SETTING.IS_ACTIVE.isTrue,
                        SETTING.TYPE.eq(type),
                        SETTING.NAME.eq(name),
                        if (parentId != null) SETTING.ORGANIZATION_ID.eq(parentId)
                        else SETTING.ORGANIZATION_ID.isNull
                )
                .fetchOne()
                ?.into(Setting::class.java)
    }

    fun fetchSetting(
            type: SettingType,
            name: String,
            organizationName: String,
            txn: DataAccessTransaction
    ): Setting? {
        val org = SETTING.`as`("org")
        val item = SETTING.`as`("item")
        return DSL.using(txn)
                .select(item.asterisk())
                .from(item)
                .join(org)
                .on(item.ORGANIZATION_ID.eq(org.SETTING_ID))
                .where(
                        item.IS_ACTIVE.isTrue,
                        item.TYPE.eq(type),
                        item.NAME.eq(name),
                        org.IS_ACTIVE.isTrue,
                        org.TYPE.eq(SettingType.ORGANIZATION),
                        org.ORGANIZATION_ID.isNull,
                        org.NAME.eq(organizationName),
                )
                .fetchOne()
                ?.into(Setting::class.java)
    }

    /**
     * Fetch both the item and the organization of the item at the same time to optimize db queries
     */
    fun fetchOrganizationAndSetting(
            type: SettingType,
            name: String,
            organizationName: String,
            txn: DataAccessTransaction? = null
    ): Pair<Setting, Setting>? {
        val org = SETTING.`as`("org")
        val item = SETTING.`as`("item")
        val ctx = if (txn != null) DSL.using(txn) else create
        val result =
                ctx.select(item.asterisk(), org.asterisk())
                        .from(item)
                        .join(org)
                        .on(item.ORGANIZATION_ID.eq(org.SETTING_ID))
                        .where(
                                item.IS_ACTIVE.isTrue,
                                item.TYPE.eq(type),
                                item.NAME.eq(name),
                                org.IS_ACTIVE.isTrue,
                                org.TYPE.eq(SettingType.ORGANIZATION),
                                org.ORGANIZATION_ID.isNull,
                                org.NAME.eq(organizationName),
                        )
                        .fetchOne()
                        ?: return null

        val itemSetting =
                Setting(
                        result.get(item.SETTING_ID),
                        result.get(item.TYPE),
                        result.get(item.NAME),
                        result.get(item.ORGANIZATION_ID),
                        result.get(item.VALUES),
                        result.get(item.IS_DELETED),
                        result.get(item.IS_ACTIVE),
                        result.get(item.VERSION),
                        result.get(item.CREATED_BY),
                        result.get(item.CREATED_AT)
                )
        val orgSetting =
                Setting(
                        result.get(org.SETTING_ID),
                        result.get(org.TYPE),
                        result.get(org.NAME),
                        result.get(org.ORGANIZATION_ID),
                        result.get(org.VALUES),
                        result.get(org.IS_DELETED),
                        result.get(org.IS_ACTIVE),
                        result.get(org.VERSION),
                        result.get(org.CREATED_BY),
                        result.get(org.CREATED_AT)
                )
        return Pair(orgSetting, itemSetting)
    }

    fun fetchSettings(type: SettingType, txn: DataAccessTransaction): List<Setting> {
        return DSL.using(txn)
                .selectFrom(SETTING)
                .where(SETTING.IS_ACTIVE.isTrue, SETTING.TYPE.eq(type))
                .orderBy(SETTING.SETTING_ID)
                .fetch()
                .into(Setting::class.java)
    }

    fun fetchSettings(
            type: SettingType,
            organizationId: Int,
            txn: DataAccessTransaction
    ): List<Setting> {
        return DSL.using(txn)
                .select()
                .from(SETTING)
                .where(
                        SETTING.IS_ACTIVE.isTrue,
                        SETTING.TYPE.eq(type),
                        SETTING.ORGANIZATION_ID.eq(organizationId)
                )
                .orderBy(SETTING.SETTING_ID)
                .fetch()
                .into(Setting::class.java)
    }

    fun insertSetting(setting: Setting, txn: DataAccessTransaction): Int {
        return DSL.using(txn)
                .insertInto(SETTING)
                .set(SETTING.SETTING_ID, DSL.defaultValue(SETTING.SETTING_ID))
                .set(SETTING.TYPE, setting.type)
                .set(SETTING.ORGANIZATION_ID, setting.organizationId)
                .set(SETTING.NAME, setting.name)
                .set(SETTING.IS_ACTIVE, setting.isActive)
                .set(SETTING.IS_DELETED, setting.isDeleted)
                .set(SETTING.VALUES, setting.values)
                .set(SETTING.VERSION, setting.version)
                .set(SETTING.CREATED_AT, setting.createdAt)
                .set(SETTING.CREATED_BY, setting.createdBy)
                .returningResult(SETTING.SETTING_ID)
                .fetchOne()
                ?.value1()
                ?: error("Fetch error")
    }

    fun updateOrganizationId(
            currentOrganizationId: Int,
            newOrganizationId: Int,
            txn: DataAccessTransaction
    ) {
        DSL.using(txn)
                .update(SETTING)
                .set(SETTING.ORGANIZATION_ID, newOrganizationId)
                .where(SETTING.ORGANIZATION_ID.eq(currentOrganizationId), SETTING.IS_ACTIVE.isTrue)
                .execute()
    }

    /** search for a setting and it children, insert a deleted setting for those found */
    fun insertDeletedSettingAndChildren(
            settingId: Int,
            settingMetadata: SettingMetadata,
            txn: DataAccessTransaction
    ) {
        DSL.using(txn)
                .insertInto(
                        SETTING,
                        SETTING.TYPE,
                        SETTING.ORGANIZATION_ID,
                        SETTING.NAME,
                        SETTING.VALUES,
                        SETTING.IS_DELETED,
                        SETTING.IS_ACTIVE,
                        SETTING.VERSION,
                        SETTING.CREATED_BY,
                        SETTING.CREATED_AT
                )
                .select(
                        DSL.select(
                                        SETTING.TYPE,
                                        SETTING.ORGANIZATION_ID,
                                        SETTING.NAME,
                                        SETTING.VALUES,
                                        DSL.value(true, SETTING.IS_DELETED),
                                        DSL.value(false, SETTING.IS_ACTIVE),
                                        SETTING.VERSION.plus(1),
                                        DSL.value(settingMetadata.createdBy, SETTING.CREATED_BY),
                                        DSL.value(settingMetadata.createdAt, SETTING.CREATED_AT)
                                )
                                .from(SETTING)
                                .where(
                                        SETTING.SETTING_ID
                                                .eq(settingId)
                                                .or(SETTING.ORGANIZATION_ID.eq(settingId)),
                                        SETTING.IS_ACTIVE.isTrue
                                )
                )
                .execute()
    }

    fun deactivateSetting(settingId: Int, txn: DataAccessTransaction) {
        DSL.using(txn)
                .update(SETTING)
                .set(SETTING.IS_ACTIVE, false)
                .where(SETTING.SETTING_ID.eq(settingId))
                .execute()
    }

    fun deactivateSettingAndChildren(settingId: Int, txn: DataAccessTransaction) {
        DSL.using(txn)
                .update(SETTING)
                .set(SETTING.IS_ACTIVE, false)
                .where(
                        SETTING.SETTING_ID.eq(settingId).or(SETTING.ORGANIZATION_ID.eq(settingId)),
                        SETTING.IS_ACTIVE.isTrue
                )
                .execute()
    }

    /**
     * Find the current setting version looking through inactive and active settings. Return -1 no
     * setting is found.
     */
    fun findSettingVersion(
            type: SettingType,
            name: String,
            organizationId: Int?,
            txn: DataAccessTransaction
    ): Int {
        return DSL.using(txn)
                .select(DSL.max(SETTING.VERSION))
                .from(SETTING)
                .where(
                        SETTING.TYPE.eq(type),
                        SETTING.NAME.eq(name),
                        if (organizationId == null) SETTING.ORGANIZATION_ID.isNull
                        else SETTING.ORGANIZATION_ID.eq(organizationId)
                )
                .fetchOne()
                ?.getValue(DSL.max(SETTING.VERSION))
                ?: -1
    }

    /** EmailSchedule queries */
    fun fetchEmailSchedules(txn: DataAccessTransaction? = null): List<String> {

        val ctx = if (txn != null) DSL.using(txn) else create
        return ctx.select(EMAIL_SCHEDULE.VALUES)
                .from(EMAIL_SCHEDULE)
                .where(EMAIL_SCHEDULE.IS_ACTIVE.eq(true))
                .fetch()
                .into(String::class.java)
    }

    fun insertEmailSchedule(body: String?, user: String, txn: DataAccessTransaction? = null): Int? {
        val ctx = if (txn != null) DSL.using(txn) else create
        return ctx.insertInto(EMAIL_SCHEDULE)
                .set(
                        EMAIL_SCHEDULE.EMAIL_SCHEDULE_ID,
                        DSL.defaultValue(EMAIL_SCHEDULE.EMAIL_SCHEDULE_ID)
                )
                .set(EMAIL_SCHEDULE.VALUES, JSON.valueOf(body))
                .set(EMAIL_SCHEDULE.IS_ACTIVE, true)
                .set(EMAIL_SCHEDULE.VERSION, 1)
                .set(EMAIL_SCHEDULE.CREATED_BY, user)
                .set(EMAIL_SCHEDULE.CREATED_AT, OffsetDateTime.now())
                .returningResult(EMAIL_SCHEDULE.EMAIL_SCHEDULE_ID)
                .fetchOne()
                ?.into(Int::class.java)
    }

    fun deleteEmailSchedule(id: Int, txn: DataAccessTransaction? = null) {
        val ctx = if (txn != null) DSL.using(txn) else create
        ctx.update(EMAIL_SCHEDULE)
                .set(EMAIL_SCHEDULE.IS_ACTIVE, false)
                .where(EMAIL_SCHEDULE.EMAIL_SCHEDULE_ID.eq(id))
                .execute()
    }

    fun saveTestData(testData: List<CovidResultMetadata>, txn: DataAccessTransaction) {
        DSL.using(txn)
                .batchInsert(
                        testData.map { td ->
                            CovidResultMetadataRecord().also { record ->
                                record.messageId = td.messageId
                                record.reportId = td.reportId
                                record.reportIndex = td.reportIndex
                                record.orderingProviderName = td.orderingProviderName
                                record.orderingProviderCounty = td.orderingProviderCounty
                                record.orderingProviderId = td.orderingProviderId
                                record.orderingProviderPostalCode = td.orderingProviderPostalCode
                                record.orderingProviderState = td.orderingProviderState
                                record.orderingFacilityCity = td.orderingFacilityCity
                                record.orderingFacilityCounty = td.orderingFacilityCounty
                                record.orderingFacilityName = td.orderingFacilityName
                                record.orderingFacilityPostalCode = td.orderingFacilityPostalCode
                                record.orderingFacilityState = td.orderingFacilityState
                                record.testResult = td.testResult
                                record.testResultCode = td.testResultCode
                                record.equipmentModel = td.equipmentModel
                                record.specimenCollectionDateTime = td.specimenCollectionDateTime
                                record.testingLabCity = td.testingLabCity
                                record.testingLabClia = td.testingLabClia
                                record.testingLabCounty = td.testingLabCounty
                                record.testingLabName = td.testingLabName
                                record.testingLabPostalCode = td.testingLabPostalCode
                                record.testingLabState = td.testingLabState
                                record.patientAge = td.patientAge
                                record.patientCounty = td.patientCounty
                                record.patientEthnicity = td.patientEthnicity
                                record.patientEthnicityCode = td.patientEthnicityCode
                                record.patientGender = td.patientGender
                                record.patientGenderCode = td.patientGenderCode
                                record.patientPostalCode = td.patientPostalCode
                                record.patientRace = td.patientRace
                                record.patientRaceCode = td.patientRaceCode
                                record.patientState = td.patientState
                                record.siteOfCare = td.siteOfCare
                            }
                        }
                )
                .execute()
    }

    fun deleteTestDataForReportId(reportId: UUID, txn: DataAccessTransaction) {
        DSL.using(txn)
                .deleteFrom(COVID_RESULT_METADATA)
                .where(COVID_RESULT_METADATA.REPORT_ID.eq(reportId))
                .execute()
    }

    fun checkReportExists(reportId: ReportId, txn: DataAccessTransaction): Boolean {
        // this is how you do a select 1 from ... in jooq
        return (DSL.using(txn)
                .select(inline(1))
                .from(REPORT_FILE)
                .where(REPORT_FILE.REPORT_ID.eq(reportId))
                .count()) > 0
    }

    /** Fetch the newest CreatedAt timestamp, active or deleted, or return [ifEmptySettings] */
    fun fetchLastModified(txn: DataAccessTransaction? = null): OffsetDateTime? {
        val ctx = if (txn != null) DSL.using(txn) else create
        return ctx.select(DSL.max(SETTING.CREATED_AT))
                .from(SETTING)
                .fetchOne()
                ?.getValue(DSL.max(SETTING.CREATED_AT))
    }

    /** Common companion object */
    companion object {
        /** Global var. Set to false prior to the lazy init, to prevent flyway migrations */
        var isFlywayMigrationOK = true

        /**
         * Create a connection pool
         *
         * Dev Note: Why a connection pool with a "stateless" Azure function? The reason is that
         * Azure functions are actually deployed in a web server. That is functions amortize startup
         * costs by reusing an existing process for a function invocation. Hence, a connection pool
         * is a win in latency after the first initialization.
         */
        private val hikariDataSource: HikariDataSource by lazy {
            DriverManager.registerDriver(Driver())

            val password = System.getenv(passwordVariable)
            val user = System.getenv(userVariable)
            val databaseUrl = System.getenv(databaseVariable)
            val config = HikariConfig()
            config.jdbcUrl = databaseUrl
            config.username = user
            config.password = password
            config.addDataSourceProperty(
                    "dataSourceClassName",
                    "org.postgresql.ds.PGSimpleDataSource"
            )
            config.addDataSourceProperty("cachePrepStmts", "true")
            config.addDataSourceProperty("prepStmtCacheSize", "250")
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            config.addDataSourceProperty(
                    "connectionTimeout",
                    "60000"
            ) // Default is 30000 (30 seconds)

            // See this info why these are a good value
            //  https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
            config.minimumIdle = 2
            config.maximumPoolSize = 25
            // This strongly recommended to be set "be several seconds shorter than any database or
            // infrastructure
            // imposed connection time limit". Not sure what value is but have observed that
            // connection are closed
            // after about 10 minutes
            config.maxLifetime = 180000
            val dataSource = HikariDataSource(config)

            val flyway = Flyway.configure().dataSource(dataSource).load()
            if (isFlywayMigrationOK) {
                flyway.migrate()
            }

            dataSource
        }

        val commonDataSource: DataSource
            get() = hikariDataSource

        fun createTaskRecord(
                report: Report,
                bodyFormat: String,
                bodyUrl: String,
                nextAction: Event,
        ): TaskRecord {
            return TaskRecord(
                    report.id,
                    nextAction.eventAction.toTaskAction(),
                    nextAction.at,
                    report.schema.name,
                    report.destination?.fullName ?: "",
                    report.itemCount,
                    bodyFormat,
                    bodyUrl,
                    report.createdDateTime,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            )
        }

        fun createTask(
                report: Report,
                bodyFormat: String,
                bodyUrl: String,
                nextAction: Event,
        ): Task {
            return Task(
                    report.id,
                    nextAction.eventAction.toTaskAction(),
                    nextAction.at,
                    report.schema.name,
                    report.destination?.fullName ?: "",
                    report.itemCount,
                    bodyFormat,
                    bodyUrl,
                    report.createdDateTime,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
            )
        }
    }
}
