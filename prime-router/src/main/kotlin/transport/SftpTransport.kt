package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SFTPTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.SftpCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.credentials.UserPpkCredential
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.RemoteResourceFilter
import net.schmizz.sshj.sftp.StatefulSFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile
import net.schmizz.sshj.userauth.password.PasswordUtils
import net.schmizz.sshj.xfer.InMemorySourceFile
import net.schmizz.sshj.xfer.LocalSourceFile
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.kotlin.Logging
import org.apache.logging.log4j.kotlin.logger
import java.io.InputStream
import java.io.StringReader

class SftpTransport : ITransport, Logging {
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val sftpTransportType = transportType as SFTPTransportType
        val host: String = sftpTransportType.host
        val port: String = sftpTransportType.port
        return try {
            if (header.content == null)
                error("No content to sftp for report ${header.reportFile.reportId}")
            val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")

            // if the transport definition has defined default
            // credentials use them, otherwise go with the
            // standard way by using the receiver full name
            val credential = lookupCredentials(sftpTransportType.credentialName ?: receiver.fullName)

            // Dev note:  db table requires body_url to be unique, but not external_name
            val fileName = Report.formExternalFilename(header)
            val sshClient = connect(host, port, credential)
            context.logger.info("Successfully connected to $sftpTransportType, ready to upload $fileName")
            uploadFile(sshClient, sftpTransportType.filePath, fileName, header.content)
            val msg = "Success: sftp upload of $fileName to $sftpTransportType"
            context.logger.info(msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                fileName,
                sftpTransportType.toString(),
                msg,
                header.reportFile.itemCount
            )
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            null
        } catch (t: Throwable) {
            val msg =
                "FAILED Sftp upload of inputReportId ${header.reportFile.reportId} to " +
                    "$sftpTransportType (orgService = ${header.receiver?.fullName ?: "null"})" +
                    ", Exception: ${t.localizedMessage}"
            context.logger.warning(msg)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }

    companion object {

        fun lookupCredentials(credentialName: String): SftpCredential {
            val credentialLabel = credentialName
                .replace(".", "--")
                .replace("_", "-")
                .uppercase()
            // Assumes credential will be cast as SftpCredential, if not return null, and thus the error case
            return CredentialHelper.getCredentialService().fetchCredential(
                credentialLabel, "SftpTransport", CredentialRequestReason.SFTP_UPLOAD
            ) as? SftpCredential?
                ?: error("Unable to find SFTP credentials for $credentialName connectionId($credentialLabel)")
        }

        fun connect(
            host: String,
            port: String,
            credential: SftpCredential,
        ): SSHClient {
            // create our client
            val sshClient = SSHClient()
            try {
                sshClient.addHostKeyVerifier(PromiscuousVerifier())
                sshClient.connect(host, port.toInt())
                when (credential) {
                    is UserPassCredential -> sshClient.authPassword(credential.user, credential.pass)
                    is UserPpkCredential -> {
                        val key = PuTTYKeyFile()
                        val keyContents = StringReader(credential.key)
                        when (StringUtils.isBlank(credential.keyPass)) {
                            true -> key.init(keyContents)
                            false -> key.init(keyContents, PasswordUtils.createOneOff(credential.keyPass.toCharArray()))
                        }
                        sshClient.authPublickey(credential.user, key)
                    }
                    else -> error("Unknown SftpCredential ${credential::class.simpleName}")
                }
                return sshClient
            } catch (t: Throwable) {
                sshClient.disconnect()
                throw t
            }
        }

        /**
         * Call this after you have already successfully connect()ed.
         */
        fun uploadFile(
            sshClient: SSHClient,
            path: String,
            fileName: String,
            contents: ByteArray
        ) {
            try {
                try {
                    sshClient.newSFTPClient().use { client ->
                        client.fileTransfer.preserveAttributes = false
                        client.use {
                            it.put(makeSourceFile(contents, fileName), "$path/$fileName")
                        }
                    }
                } finally {
                    sshClient.disconnect()
                }
            } catch (ce: net.schmizz.sshj.connection.ConnectionException) {
                // if the timeout happens on disconnect it gets wrapped up in the connectException
                // and we need to check the root cause
                if (ce.cause is java.util.concurrent.TimeoutException) {
                    // do nothing. some servers just take a long time to disconnect
                    logger().warn("Connection exception during ls: ${ce.localizedMessage}")
                } else {
                    throw ce
                }
            }
        }

        fun ls(sshClient: SSHClient, path: String): List<String> {
            return ls(sshClient, path, null)
        }

        fun ls(sshClient: SSHClient, path: String, resourceFilter: RemoteResourceFilter?): List<String> {
            val lsResults = mutableListOf<String>()
            try {
                try {
                    sshClient.use { client ->
                        client.newSFTPClient().use {
                            it.ls(path, resourceFilter).map { l -> lsResults.add(l.toString()) }
                        }
                    }
                } finally {
                    sshClient.disconnect()
                }
            } catch (ce: net.schmizz.sshj.connection.ConnectionException) {
                // if the timeout happens on disconnect it gets wrapped up in the connectException
                // and we need to check the root cause
                if (ce.cause is java.util.concurrent.TimeoutException) {
                    // do nothing. some servers just take a long time to disconnect
                    logger().warn("Connection exception during ls: ${ce.localizedMessage}")
                } else {
                    throw ce
                }
            }

            return lsResults
        }

        fun rm(sshClient: SSHClient, path: String, fileName: String) {
            try {
                try {
                    sshClient.use { client ->
                        client.newSFTPClient().use {
                            it.rm("$path/$fileName")
                        }
                    }
                } finally {
                    sshClient.disconnect()
                }
            } catch (ce: net.schmizz.sshj.connection.ConnectionException) {
                // if the timeout happens on disconnect it gets wrapped up in the connectException
                // and we need to check the root cause
                if (ce.cause is java.util.concurrent.TimeoutException) {
                    // do nothing. some servers just take a long time to disconnect
                    logger().warn("Connection exception during ls: ${ce.localizedMessage}")
                } else {
                    throw ce
                }
            }
        }

        fun pwd(sshClient: SSHClient): String {
            var pwd: String
            try {
                sshClient.newStatefulSFTPClient().use { client ->
                    val statefulClient = client as StatefulSFTPClient
                    sshClient.timeout = 120000
                    statefulClient.use {
                        pwd = it.pwd()
                    }
                }
            } finally {
                sshClient.disconnect()
            }

            return pwd
        }

        private fun makeSourceFile(contents: ByteArray, fileName: String): LocalSourceFile {
            return object : InMemorySourceFile() {
                override fun getName(): String {
                    return fileName
                }

                override fun getLength(): Long {
                    return contents.size.toLong()
                }

                override fun getInputStream(): InputStream {
                    return contents.inputStream()
                }

                override fun isDirectory(): Boolean {
                    return false
                }

                override fun isFile(): Boolean {
                    return true
                }

                override fun getPermissions(): Int {
                    return 777
                }
            }
        }
    }
}