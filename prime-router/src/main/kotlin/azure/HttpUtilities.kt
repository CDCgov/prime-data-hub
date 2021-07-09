package gov.cdc.prime.router.azure

import com.azure.core.util.Base64Url
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.PAYLOAD_MAX_BYTES
import org.apache.logging.log4j.kotlin.Logging
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Sender
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class ReportStreamEnv(val baseUrl: String) {
    TEST("https://pdhtest-functionapp.azurewebsites.net"),
    LOCAL("http://localhost:7071"),
    STAGING("https://staging.prime.cdc.gov"),
    PROD("not implemented")
}

class HttpUtilities {
    companion object: Logging {
        const val jsonMediaType = "application/json"

        const val reportsEndpoint = "/api/reports"

        fun okResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.OK)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun okResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
            lastModified: OffsetDateTime?,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.OK)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .also { addHeaderIfModified(it, lastModified) }
                .build()
        }

        fun okResponse(
            request: HttpRequestMessage<String?>,
            lastModified: OffsetDateTime?,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .also { addHeaderIfModified(it, lastModified) }
                .build()
        }

        fun createdResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.CREATED)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        /**
         * Allows the validator to figure out specific failure, and pass it in here.
         * Can be used for any response code.
         * Todo: other generic failure response methods here could be removed, and replaced with this
         *      generic method, instead of having to create a new method for every HttpStatus code.
         */
        fun httpResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
            httpStatus: HttpStatus,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(httpStatus)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun badRequestResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun unauthorizedResponse(
            request: HttpRequestMessage<String?>
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.UNAUTHORIZED)
                .build()
        }

        fun unauthorizedResponse(
            request: HttpRequestMessage<String?>,
            responseBody: String,
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.UNAUTHORIZED)
                .body(responseBody)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun notFoundResponse(
            request: HttpRequestMessage<String?>
        ): HttpResponseMessage {
            return request
                .createResponseBuilder(HttpStatus.NOT_FOUND)
                .build()
        }

        fun internalErrorResponse(
            request: HttpRequestMessage<String?>
        ): HttpResponseMessage {
            val body = """{"error": "Internal error at ${OffsetDateTime.now()}"}"""
            return request
                .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body)
                .header(HttpHeaders.CONTENT_TYPE, jsonMediaType)
                .build()
        }

        fun errorJson(message: String): String {
            return """{"error": "$message"}"""
        }

        private fun addHeaderIfModified(
            builder: HttpResponseMessage.Builder,
            lastModified: OffsetDateTime?
        ) {
            if (lastModified == null) return
            // https://datatracker.ietf.org/doc/html/rfc7232#section-2.2 defines this header format
            val lastModifiedFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss")
            // Convert to GMT timezone
            val lastModifiedGMT = OffsetDateTime.ofInstant(lastModified.toInstant(), ZoneOffset.UTC)
            val lastModifiedFormatted = "${lastModifiedGMT.format(lastModifiedFormatter)} GMT"
            builder.header(HttpHeaders.LAST_MODIFIED, lastModifiedFormatted)
        }

        /**
         * convenience method that combines logging, and generation of an HtttpResponse
         * Not enforced, but meant to be used for unhappy outcomes
         */
        fun bad(
            request: HttpRequestMessage<String?>,
            msg: String,
            status: HttpStatus = HttpStatus.BAD_REQUEST
        ): HttpResponseMessage {
            logger.error(msg)
            return HttpUtilities.httpResponse(request, msg,status)
        }

        /**
         * Do a variety of checks on payload size.
         * Returns a Pair (http error code, human readable error message)
         */
        fun payloadSizeCheck(request: HttpRequestMessage<String?>): Pair<HttpStatus, String> {
            val contentLengthStr = request.headers["content-length"]
                ?: return HttpStatus.LENGTH_REQUIRED to "ERROR: No content-length header found.  Refusing this request."
            val contentLength = try {
                contentLengthStr.toLong()
            } catch (e: NumberFormatException) {
                return HttpStatus.LENGTH_REQUIRED to "ERROR: content-length header is not a number"
            }
            when {
                contentLength < 0 -> {
                    return HttpStatus.LENGTH_REQUIRED to "ERROR: negative content-length $contentLength"
                }
                contentLength > PAYLOAD_MAX_BYTES -> {
                    return HttpStatus.PAYLOAD_TOO_LARGE to
                        "ERROR: content-length $contentLength is larger than max $PAYLOAD_MAX_BYTES"
                }
            }
            // content-length header is ok.  Now check size of actual body as well
            val content = request.body
            if (content != null && content.length > PAYLOAD_MAX_BYTES) {
                return HttpStatus.PAYLOAD_TOO_LARGE to
                    "ERROR: body size ${content.length} is larger than max $PAYLOAD_MAX_BYTES " +
                    "(content-length header = $contentLength"
            }
            return HttpStatus.OK to ""
        }

        /**
         * A generic function to POST a Prime Data Hub report File to a particular Prime Data Hub Environment,
         * as if from sendingOrgName.sendingOrgClientName.
         * Returns Pair(Http response code, json response text)
         */
        fun postReportFile(
            environment: ReportStreamEnv,
            file: File,
            sendingOrgName: String,
            sendingOrgClient: Sender,
            key: String? = null,
            option: ReportFunction.Options ? = null
        ): Pair<Int, String> {
            if (!file.exists()) error("Unable to find file ${file.absolutePath}")
            return postReportBytes(environment, file.readBytes(), sendingOrgName, sendingOrgClient, key, option)
        }

        /**
         * A generic function to POST data to a particular Prime Data Hub Environment,
         * as if from sendingOrgName.sendingOrgClientName.
         * Returns Pair(Http response code, json response text)
         */
        fun postReportBytes(
            environment: ReportStreamEnv,
            bytes: ByteArray,
            sendingOrgName: String,
            sendingOrgClient: Sender,
            key: String?,
            option: ReportFunction.Options?
        ): Pair<Int, String> {
            val headers = mutableListOf<Pair<String, String>>()
            when (sendingOrgClient.format) {
                Sender.Format.HL7 -> headers.add("Content-Type" to Report.Format.HL7.mimeType)
                else -> headers.add("Content-Type" to Report.Format.CSV.mimeType)
            }
            val clientStr = sendingOrgName + if (sendingOrgClient.name.isNotBlank()) ".${sendingOrgClient.name}" else ""
            headers.add("client" to clientStr)
            if (key == null && environment == ReportStreamEnv.TEST) error("key is required for Test environment")
            if (key != null)
                headers.add("x-functions-key" to key)
            val url = environment.baseUrl + reportsEndpoint + if (option != null) "?option=$option" else ""
            return postHttp(url, bytes, headers)
        }

        /**
         * A generic function that posts data to a URL <address>.
         * Returns a Pair (HTTP response code, text of the response)
         */
        fun postHttp(urlStr: String, bytes: ByteArray, headers: List<Pair<String, String>>? = null): Pair<Int, String> {
            val urlObj = URL(urlStr)
            with(urlObj.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                headers?.forEach {
                    addRequestProperty(it.first, it.second)
                }
                outputStream.use {
                    it.write(bytes)
                }
                val response = try {
                    inputStream.bufferedReader().readText()
                } catch (e: IOException) {
                    // HttpUrlStatus treats not-success codes as IOExceptions.
                    // I found that the returned json is secretly still here:
                    errorStream?.bufferedReader()?.readText()
                        ?: this.responseMessage
                }
                return responseCode to response
            }
        }
    }
}