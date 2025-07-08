package com.joinself

import com.joinself.selfsdk.kmp.account.Account
import com.joinself.selfsdk.kmp.account.LogLevel
import com.joinself.selfsdk.kmp.account.Target
import com.joinself.selfsdk.kmp.asset.BinaryObject
import com.joinself.selfsdk.kmp.credential.Address
import com.joinself.selfsdk.kmp.credential.CredentialBuilder
import com.joinself.selfsdk.kmp.credential.PresentationBuilder
import com.joinself.selfsdk.kmp.error.SelfStatus
import com.joinself.selfsdk.kmp.error.SelfStatusName
import com.joinself.selfsdk.kmp.event.AnonymousMessage
import com.joinself.selfsdk.kmp.event.Commit
import com.joinself.selfsdk.kmp.event.Flag
import com.joinself.selfsdk.kmp.event.FlagSet
import com.joinself.selfsdk.kmp.event.Integrity
import com.joinself.selfsdk.kmp.event.KeyPackage
import com.joinself.selfsdk.kmp.event.Message
import com.joinself.selfsdk.kmp.event.Proposal
import com.joinself.selfsdk.kmp.event.QrEncoding
import com.joinself.selfsdk.kmp.event.Reference
import com.joinself.selfsdk.kmp.event.Welcome
import com.joinself.selfsdk.kmp.keypair.signing.PublicKey
import com.joinself.selfsdk.kmp.message.Chat
import com.joinself.selfsdk.kmp.message.ComparisonOperator
import com.joinself.selfsdk.kmp.message.ContentType
import com.joinself.selfsdk.kmp.message.CredentialPresentationDetailParameter
import com.joinself.selfsdk.kmp.message.CredentialPresentationRequestBuilder
import com.joinself.selfsdk.kmp.message.CredentialPresentationResponse
import com.joinself.selfsdk.kmp.message.CredentialVerificationRequestBuilder
import com.joinself.selfsdk.kmp.message.CredentialVerificationResponse
import com.joinself.selfsdk.kmp.message.DiscoveryRequestBuilder
import com.joinself.selfsdk.kmp.message.DiscoveryResponse
import com.joinself.selfsdk.kmp.message.Receipt
import com.joinself.selfsdk.kmp.platform.Attestation
import com.joinself.selfsdk.kmp.time.Timestamp
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Semaphore
import kotlin.coroutines.suspendCoroutine


sealed class SERVER_REQUESTS {
    companion object {
        val REQUEST_CREDENTIAL_AUTH: String = "REQUEST_CREDENTIAL_AUTH"
        val REQUEST_CREDENTIAL_EMAIL: String = "PROVIDE_CREDENTIAL_EMAIL"
        val REQUEST_CREDENTIAL_DOCUMENT: String = "PROVIDE_CREDENTIAL_DOCUMENT"
        val REQUEST_CREDENTIAL_CUSTOM: String = "PROVIDE_CREDENTIAL_CUSTOM"
        val REQUEST_DOCUMENT_SIGNING: String = "REQUEST_DOCUMENT_SIGNING"
        val REQUEST_GET_CUSTOM_CREDENTIAL: String = "REQUEST_GET_CUSTOM_CREDENTIAL"
    }
}

/**
 * run in terminal: ./gradlew :self-demo:run
 */
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Self Demo")

    val signal = Semaphore(1)
    signal.acquire()

    var inboxAddress: PublicKey? = null
    var responderAddress: PublicKey? = null
    var groupAddress: PublicKey? = null
    var discoveryRequestId: String = ""

    val sandbox = true
    val rpcAddress = if (sandbox) Target.PRODUCTION_SANDBOX.rpcEndpoint() else Target.PRODUCTION.rpcEndpoint()
    val objectAddress = if (sandbox) Target.PRODUCTION_SANDBOX.objectEndpoint() else Target.PRODUCTION.objectEndpoint()
    val messageAddress = if (sandbox) Target.PRODUCTION_SANDBOX.messageEndpoint() else Target.PRODUCTION.messageEndpoint()

    val account = Account()
    val status = account.configure(
        storagePath = ":memory:",
        storageKey = ByteArray(32),
        rpcEndpoint = rpcAddress,
        objectEndpoint = objectAddress,
        messageEndpoint = messageAddress,
        logLevel = LogLevel.INFO,
        onConnect = {
            println("KMP connected")
            signal.release()
        },
        onDisconnect = { reason: SelfStatus? ->
            println("KMP disconnected")
        },
        onAcknowledgement = {reference: Reference ->
            println("KMP onAcknowledgement id:${reference.id().toHexString()}")
        },
        onError = {reference: Reference, error: SelfStatus ->
            println("KMP onError")
        },
        onCommit = { commit: Commit ->
            println("KMP commited")
        },
        onKeyPackage = { keyPackage: KeyPackage ->
            println("KMP keypackage")
            account.connectionEstablish(asAddress =  keyPackage.toAddress(), keyPackage = keyPackage.keyPackage(),
                onCompletion = {status: SelfStatus, gAddress: PublicKey ->
                    println("connection establish status:${SelfStatusName.getName(status.code())} - group:${gAddress.encodeHex()}")
                    responderAddress = keyPackage.fromAddress()
                    groupAddress = gAddress
                    signal.release()
                }
            )
        },
        onWelcome = { welcome: Welcome ->
            println("KMP welcome")
            account.connectionAccept(asAddress = welcome.toAddress(), welcome =  welcome.welcome()) { status: SelfStatus, gAddress: PublicKey ->
                println("accepted connection encrypted group status:${SelfStatusName.getName(status.code())} - from:${welcome.fromAddress().encodeHex()} - group:${gAddress.encodeHex()}")
                responderAddress = welcome.fromAddress()
                groupAddress = gAddress
            }
        },
        onProposal = { proposal: Proposal ->
            println("KMP proposal")
        },
        onMessage = { message: Message ->
            val content = message.content()
            val contentType = content.contentType()
            println("\nKMP message type: $contentType")
            when (contentType) {
                ContentType.DISCOVERY_RESPONSE -> {
                    val discoveryResponse = DiscoveryResponse.decode(content)
                    val responseTo = discoveryResponse.responseTo().toHexString()
                    println("received response to discovery request from:${message.fromAddress().encodeHex()} - requestId:${responseTo} - messageId:${message.id().toHexString()}")

                    if (responseTo != discoveryRequestId) {
                        println("received response to unknown request requestId:$responseTo")
                    }

                    signal.release()
                }
                ContentType.INTRODUCTION -> {
                    println("received introduction")
                }
                ContentType.CHAT -> {
                    val chat = Chat.decode(content)

                    println(
                        "\nfrom:${message.fromAddress().encodeHex()}" +
                        "\nmessageId:${message.id().toHexString()}" +
                        "\nmessage:${chat.message()}" +
                        "\n"
                    )

                    when (chat.message()) {
                        SERVER_REQUESTS.REQUEST_CREDENTIAL_AUTH -> {
                            val credentialRequest = CredentialPresentationRequestBuilder()
                                .presentationType(arrayOf("VerifiablePresentation", "CustomPresentation"))
                                .details(arrayOf("VerifiableCredential","LivenessCredential"), arrayOf(CredentialPresentationDetailParameter.create(ComparisonOperator.NOT_EQUALS, "sourceImageHash", "")))
                                .expires(Timestamp.now() + 3600)
                                .finish()
                            val credentialRequestId = credentialRequest.id().toHexString()

                            val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
                            println("send auth request status: ${SelfStatusName.getName(sendStatus.code())} - to:${groupAddress.encodeHex()} - requestId:${credentialRequestId}")
                        }
                        SERVER_REQUESTS.REQUEST_CREDENTIAL_EMAIL -> {
                            val credentialRequest = CredentialPresentationRequestBuilder()
                                .presentationType(arrayOf("VerifiablePresentation", "EmailPresentation"))
                                .details(arrayOf("VerifiableCredential","EmailCredential"), arrayOf(CredentialPresentationDetailParameter.create(ComparisonOperator.NOT_EQUALS, "emailAddress", "")))
                                .expires(Timestamp.now() + 3600)
                                .finish()
                            val credentialRequestId = credentialRequest.id().toHexString()

                            val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
                            println("send email credential request status: ${SelfStatusName.getName(sendStatus.code())} - to:${groupAddress.encodeHex()} - requestId:${credentialRequestId}")
                        }
                        SERVER_REQUESTS.REQUEST_CREDENTIAL_DOCUMENT -> {
                            val credentialRequest = CredentialPresentationRequestBuilder()
                                .presentationType(arrayOf("VerifiablePresentation", "DocumentPresentation"))
                                .details(arrayOf("VerifiableCredential","PassportCredential"), arrayOf(CredentialPresentationDetailParameter.create(ComparisonOperator.NOT_EQUALS, "documentNumber", "")))
                                .expires(Timestamp.now() + 3600)
                                .finish()
                            val credentialRequestId = credentialRequest.id().toHexString()

                            val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
                            println("send document credential request status: ${SelfStatusName.getName(sendStatus.code())} - to:${groupAddress.encodeHex()} - requestId:${credentialRequestId}")
                        }
                        SERVER_REQUESTS.REQUEST_CREDENTIAL_CUSTOM -> {
                            val credentialRequest = CredentialPresentationRequestBuilder()
                                .presentationType(arrayOf("VerifiablePresentation", "CustomPresentation"))
                                .details(arrayOf("VerifiableCredential","CustomerCredential"), arrayOf(CredentialPresentationDetailParameter.create(ComparisonOperator.NOT_EQUALS, "name", "")))
                                .expires(Timestamp.now() + 3600)
                                .finish()
                            val credentialRequestId = credentialRequest.id().toHexString()

                            val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
                            println("send custom credential request status: ${SelfStatusName.getName(sendStatus.code())} - to:${groupAddress.encodeHex()} - requestId:${credentialRequestId}")
                        }
                        SERVER_REQUESTS.REQUEST_DOCUMENT_SIGNING -> {
                            val terms = "Agreement test"
                            val agreementTerms = BinaryObject.create(
                                "text/plain",
                                terms.encodeToByteArray()
                            )

                            account.objectStore(agreementTerms)
                            val uploadStatus = runBlocking {
                                suspendCoroutine { continuation ->
                                    account.objectUpload( agreementTerms, false) { status ->
                                        continuation.resumeWith(Result.success(status))
                                    }
                                }
                            }
                            if (!uploadStatus.success()) {
                                println("failed to upload object ${SelfStatusName.getName(uploadStatus.code())}")
                                return@configure
                            }
                            val claims = HashMap<String, Any>()
                            claims["termsHash"] = agreementTerms.hash()!!.toHexString()
                            claims["parties"] = arrayOf(
                                hashMapOf("type" to "signatory", "id" to inboxAddress!!.encodeHex()),
                                hashMapOf("type" to "signatory", "id" to responderAddress?.encodeHex()),
                            )

                            val unsignedAgreementCredential = CredentialBuilder()
                                .credentialType(arrayOf("VerifiableCredential", "AgreementCredential"))
                                .credentialSubject(Address.key(inboxAddress!!))
                                .credentialSubjectClaims(claims)
                                .issuer(Address.key(inboxAddress!!))
                                .validFrom(Timestamp.now())
                                .signWith(inboxAddress!!, Timestamp.now())
                                .finish()
                            val signedAgreementCredential = account.credentialIssue(unsignedAgreementCredential)

                            val unsignedAgreementPresentation = PresentationBuilder()
                                .presentationType(arrayOf("VerifiablePresentation", "AgreementPresentation"))
                                .holder(Address.key(inboxAddress!!))
                                .credentialAdd(signedAgreementCredential)
                                .finish()
                            val signedAgreementPresentation = account.presentationIssue(unsignedAgreementPresentation)

                            val agreementRequest = CredentialVerificationRequestBuilder()
                                .credentialType(arrayOf("VerifiableCredential", "AgreementCredential"))
                                .evidence("terms", agreementTerms)
                                .proof(signedAgreementPresentation)
                                .expires(Timestamp.now() + 3600)
                                .finish()

                            val sendStatus = account.messageSend(responderAddress!!, agreementRequest)
                            println("send agreement status:${SelfStatusName.getName(sendStatus.code())} - to:${responderAddress!!.encodeHex()} - requestId:${agreementRequest.id().toHexString()}")
                        }
                        SERVER_REQUESTS.REQUEST_GET_CUSTOM_CREDENTIAL -> {
                            val subjectAddress = Address.key(responderAddress!!)
                            val issuerAddress = Address.key(inboxAddress!!)
                            val customerCredential = CredentialBuilder()
                                .credentialType(arrayOf("VerifiableCredential", "CustomerCredential"))
                                .credentialSubject(subjectAddress)
                                .credentialSubjectClaims(mapOf(
                                    "name" to "Test Name"))
                                .issuer(issuerAddress)
                                .validFrom(Timestamp.now())
                                .signWith(inboxAddress!!, Timestamp.now())
                                .finish()
                            val customerVerifiableCredential = account.credentialIssue(customerCredential)

                            val content = com.joinself.selfsdk.kmp.message.CredentialBuilder()
                                .credential(customerVerifiableCredential)
                                .finish()

                            val messageId = content.id().toHexString()

                            val sendStatus = account.messageSend(responderAddress, content)
                            println("send Custom Credentials status: ${SelfStatusName.getName(sendStatus.code())} - to:${responderAddress.encodeHex()} - messageId:${messageId}")

                        }
                    }
                }
                ContentType.RECEIPT -> {
                    val receipt = Receipt.decode(content)
                    val delivered = receipt.delivered().filter{ it.isNotEmpty() }.map { it.toHexString() }.toList()
                    val read = receipt.read().filter{ it.isNotEmpty() }.map { it.toHexString() }.toList()
                    println("received receipt \ndelivered:$delivered\nread:$read")
                    println("\n\n")
                }
                ContentType.CREDENTIAL_PRESENTATION_RESPONSE -> {
                    val credentialResponse = CredentialPresentationResponse.decode(content)
                    println("Response received with status:${credentialResponse.status().name}")
                    credentialResponse.presentations().forEach { pre ->
                        val credentials = pre.credentials()
                        credentials.forEach { cred ->
                            val claims = cred.credentialSubjectClaims()
                            claims.forEach {
                                println(
                                        "types:${cred.credentialType().toList()}" +
                                        "\nfield:${it.key}" +
                                        "\nvalue:${it.value}"
                                )
                                println()
                            }
                        }
                    }
                }
                ContentType.CREDENTIAL_VERIFICATION_RESPONSE -> {
                    val verificationResponse = CredentialVerificationResponse.decode(content)
                    println("Response received with status:${verificationResponse.status().name}")
                    verificationResponse.credentials().forEach { cred ->
                        val claims = cred.credentialSubjectClaims()
                        claims.forEach {
                            println(
                                    "types:${cred.credentialType().toList()}" +
                                    "\nfield:${it.key}" +
                                    "\nvalue:${it.value}"
                            )
                            println()
                        }
                    }
                }
                else -> { }
            }
        },
        onIntegrity = { integrity: Integrity ->
            println("KMP integrity")
            Attestation.deviceCheck(applicationAddress = PublicKey.decodeHex("0016fced9deea88223b7faaee3e28f0363c99974c67ee7842ead128a0f36a9f1e3"), integrityToken =  ByteArray(integrity.requestHash().size + 128))
        }
    )
    println("status: ${SelfStatusName.getName(status.code())}")
    signal.acquire()

    inboxAddress = runBlocking {
        suspendCoroutine { continuation ->
            account.inboxOpen { status: SelfStatus, address: PublicKey ->
                println("inbox open status:${SelfStatusName.getName(status.code())} - address:${address.encodeHex()}")
                if (status.success()) {
                    continuation.resumeWith(Result.success(address))
                } else {
                    continuation.resumeWith(Result.success(null))
                }
            }
        }
    }
    if (inboxAddress == null) {
        throw Exception("Can't open inbox")
    }
    println("\n\n")
    println("server address: ${inboxAddress.encodeHex()}")
    println("clients should use this address or scan the qrcode to connect to this server")

    val expires = Timestamp.now() + 3600
    val keyPackage = account.connectionNegotiateOutOfBand(inboxAddress, expires)
    val discoveryRequest = DiscoveryRequestBuilder()
        .keyPackage(keyPackage)
        .expires(expires)
        .finish()
    val anonymousMessage = AnonymousMessage.fromContent(discoveryRequest)
    if (sandbox) {
        anonymousMessage.setFlags(FlagSet(Flag.TARGET_SANDBOX))
    }
    val qrCodeBytes = anonymousMessage.encodeQR(QrEncoding.UNICODE)
    val qrCodeString = qrCodeBytes.decodeToString()
    println(qrCodeString)

    println("\n\n")
    println("Type quit or Ctrl-C to exit")
    while (true) {
        val q = readln()
        if (q == "quit") {
            break
        }
    }
}