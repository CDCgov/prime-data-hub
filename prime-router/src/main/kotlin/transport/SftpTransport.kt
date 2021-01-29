package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialManagement
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserPassCredential
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.IOException
import java.io.InputStream
import java.util.logging.Level

class SftpTransport : ITransport, CredentialManagement {
    override fun send(
        orgService: OrganizationService,
        transportType: TransportType,
        contents: ByteArray,
        inputReportId: ReportId,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val sftpTransportType = transportType as SFTPTransportType
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port
        return try {
            val (user, pass) = lookupCredentials(orgService.fullName)
            val extension = orgService.format.toExt()
            // Dev note:  db table requires fileName to be unique.
            val fileName = "${orgService.fullName.replace('.', '-')}-$sentReportId.$extension"

            uploadFile(host, port, user, pass, sftpTransportType.filePath, fileName, contents, context)
            val msg = "Success: sftp upload of $fileName to $sftpTransportType"
            context.logger.log(Level.INFO, msg)
            actionHistory.trackActionResult(msg)
            // todo fix the itemCount == -1, when we refactor workflowEngine to read from new tables.
            actionHistory.trackSentReport(orgService, sentReportId, fileName, sftpTransportType.toString(), msg, -1)
            null
        } catch (ioException: IOException) {
            val msg = "FAILED Sftp upload of inputReportId $inputReportId to $sftpTransportType (orgService = ${orgService.fullName})"
            context.logger.log(
                Level.WARNING, msg, ioException
            )
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            // Ambivalent about this - seems not useful to track a file that does not exist.   Removing for now.
// delete this            actionHistory.trackFailedReport(orgService, sentReportId, sftpTransportType.toString(), msg)
            RetryToken.allItems
        } // let non-IO exceptions be caught by the caller
    }

    private fun lookupCredentials(orgName: String): Pair<String, String> {

        val envVarLabel = orgName.replace(".", "__").replace('-', '_').toUpperCase()

        // Assumes credential will be cast as UserPassCredential, if not return null, and thus the error case
        val credential = credentialService.fetchCredential(envVarLabel, "SftpTransport", CredentialRequestReason.SFTP_UPLOAD) as? UserPassCredential?
            ?: error("Unable to find SFTP credentials for $orgName connectionId($envVarLabel)")

        return Pair(credential.user, credential.pass)
    }

    private fun uploadFile(
        host: String,
        port: String,
        user: String,
        pass: String,
        path: String,
        fileName: String,
        contents: ByteArray,
        context: ExecutionContext // TODO: temp fix to add logging
    ) {
        val sshClient = SSHClient()
        try {

            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connect(host, port.toInt())
            sshClient.authPassword(user, pass)

            var client = sshClient.newSFTPClient()
            client.getFileTransfer().setPreserveAttributes(false)
            try {

                client
                    .put(
                        object : InMemorySourceFile() {
                            override fun getName(): String { return fileName }
                            override fun getLength(): Long { return contents.size.toLong() }
                            override fun getInputStream(): InputStream { return contents.inputStream() }
                            override fun isDirectory(): Boolean { return false }
                            override fun isFile(): Boolean { return true }
                            override fun getPermissions(): Int { return 777 }
                        },
                        path + "/" + fileName
                    )
                // TODO: remove this over logging when bug is fixed
                // context.logger.log(Level.INFO, "SFTP PUT succeeded: $fileName")
            } finally {
                client.close()
            }
        } finally {
            sshClient.disconnect()
            // context.logger.log(Level.INFO, "SFTP DISCONNECT succeeded: $fileName")
        }
    }
}