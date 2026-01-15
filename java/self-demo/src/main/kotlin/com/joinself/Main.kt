package com.joinself

import com.joinself.selfsdk.account.Account
import com.joinself.selfsdk.account.Callbacks
import com.joinself.selfsdk.account.Config
import com.joinself.selfsdk.account.LogLevel
import com.joinself.selfsdk.account.Target
import com.joinself.selfsdk.asset.BinaryObject
import com.joinself.selfsdk.credential.*
import com.joinself.selfsdk.credential.CredentialBuilder
import com.joinself.selfsdk.credential.predicate.Predicate
import com.joinself.selfsdk.credential.predicate.PredicateTree
import com.joinself.selfsdk.error.SelfError
import com.joinself.selfsdk.error.SelfStatus
import com.joinself.selfsdk.event.*
import com.joinself.selfsdk.identity.OperationBuilder
import com.joinself.selfsdk.identity.Role
import com.joinself.selfsdk.identity.RoleSet
import com.joinself.selfsdk.keypair.signing.PublicKey
import com.joinself.selfsdk.message.*
import com.joinself.selfsdk.platform.Attestation
import com.joinself.selfsdk.time.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.runBlocking
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
var inboxAddress: PublicKey? = null
var responderAddress: PublicKey? = null
var groupAddress: PublicKey? = null

/**
 * run in terminal: ./gradlew :self-demo:run
 */
@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    println("Self Demo")

    val onConnect: Channel<Boolean> = Channel()
    val userHome = System.getProperty("user.home")
    val storagePath = "$userHome/.self_demo_server"
    val directory = File(storagePath)
    if (!directory.exists()) directory.mkdirs() // create directory if not exist
    println("\nserver data is stored at: $storagePath\n")

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val config = Config(
        storagePath =  "$storagePath/selfsdk.db", // or ":memory:",
        storageKey = ByteArray(size = 32),
        target = Target.previewSandbox(),
        logLevel =  LogLevel.INFO
    )
    val callbacks = Callbacks(
        onConnect = {
            println("KMP connected")

            onConnect.trySend(true)
        },
        onDisconnect = { account, reason: SelfStatus? ->
            println("KMP disconnected")
        },
        onAcknowledgement = {account, reference: Reference ->
            println("KMP onAcknowledgement id:${reference.id().toHexString()}")
        },
        onError = {account, reference: Reference, error: SelfStatus ->
            println("KMP onError")
        },
        onCommit = {account, commit: Commit ->
            println("KMP commited")
        },
        onKeyPackage = {account, keyPackage: KeyPackage ->
            println("KMP keyPackage")
            account.connectionEstablish(asAddress =  keyPackage.toAddress(), keyPackage = keyPackage.keyPackage(),
                onCompletion = {status: SelfStatus, gAddress: PublicKey ->
                    println("connection establish status:${status.name()} - group:${gAddress.encodeHex()}")
                    responderAddress = keyPackage.fromAddress()
                    groupAddress = gAddress

                    coroutineScope.launch {
                        delay(1000)
                        generateQrCode(account)
                    }
                }
            )
        },
        onWelcome = {account, welcome: Welcome ->
            println("KMP welcome")
            account.connectionAccept(asAddress = welcome.toAddress(), welcome =  welcome.welcome()) { status: SelfStatus, gAddress: PublicKey ->
                println("accepted connection encrypted group status:${status.name()} - from:${welcome.fromAddress().encodeHex()} - group:${gAddress.encodeHex()}")
                responderAddress = welcome.fromAddress()
                groupAddress = gAddress
            }
        },
        onProposal = {account, proposal: Proposal ->
            println("KMP proposal")
        },
        onDropped = {account, dropped: Dropped ->
            println("KMP dropped ${dropped.reason()}")
        },
        onMessage = { account, message: Message ->
            val content = message.content()
            val contentType = content.contentType()
            println("\nKMP message type: $contentType")
            when (contentType) {
                ContentType.DISCOVERY_RESPONSE -> {
                    val discoveryResponse = DiscoveryResponse.decode(content)
                    val responseTo = discoveryResponse.responseTo().toHexString()
                    println("received response to discovery request from:${message.fromAddress().encodeHex()} - requestId:${responseTo} - messageId:${message.id().toHexString()}")

                    coroutineScope.launch {
                        generateQrCode(account)
                    }
                }
                ContentType.INTRODUCTION -> {
                    println("received introduction")
                    val introduction = Introduction.decode(content)
                    val presentations = introduction.presentations()
                    presentations?.forEach { pre ->
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
                            sendLivenessRequest(account)
                        }
                        SERVER_REQUESTS.REQUEST_CREDENTIAL_EMAIL -> {
                            val emailPredicate = Predicate.contains(CredentialField.TYPE, CredentialType.EMAIL)
                                .and(Predicate.notEmpty(CredentialField.SUBJECT_EMAIL_ADDRESS))
                            val predicatesTree = PredicateTree.create(emailPredicate)
                            val credentialRequest = CredentialPresentationRequestBuilder()
                                .presentationType("EmailPresentation")
                                .predicates(predicatesTree)
                                .expires(Timestamp.now() + 3600)
                                .finish()
                            val credentialRequestId = credentialRequest.id().toHexString()

                            val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
                            println("send email credential request status: ${sendStatus.name()} - to:${groupAddress?.encodeHex()} - requestId:${credentialRequestId}")
                        }
                        SERVER_REQUESTS.REQUEST_CREDENTIAL_DOCUMENT -> {
                            sendDocumentRequest(account)
                        }
                        SERVER_REQUESTS.REQUEST_CREDENTIAL_CUSTOM -> {
                            val customPredicate = Predicate.contains(CredentialField.TYPE, "CustomerCredential")
                                .and(Predicate.notEmpty("/credentialSubject/name"))
                            val predicatesTree = PredicateTree.create(customPredicate)

                            val credentialRequest = CredentialPresentationRequestBuilder()
                                .presentationType("CustomPresentation")
                                .predicates(predicatesTree)
                                .expires(Timestamp.now() + 3600)
                                .finish()
                            val credentialRequestId = credentialRequest.id().toHexString()

                            val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
                            println("send custom credential request status: ${sendStatus.name()} - to:${groupAddress?.encodeHex()} - requestId:${credentialRequestId}")
                        }
                        SERVER_REQUESTS.REQUEST_DOCUMENT_SIGNING -> {
                            sendAgreementRequest(account)
                        }
                        SERVER_REQUESTS.REQUEST_GET_CUSTOM_CREDENTIAL -> {
                            sendCustomCredentials(account)
                        }
                        else -> {
                            val attachments = chat.attachments()
                            attachments.forEach {attachment ->
                                account.objectDownload(attachment) { dStatus ->
                                    println("attachment ${attachment.mimeType()} - size:${attachment.data()?.size}")
                                }
                            }

                            sendChatResponse(account, chat)
                        }
                    }
                }
                ContentType.RECEIPT -> {
                    val receipt = Receipt.decode(content)
                    val delivered = receipt.delivered().filter{ it.isNotEmpty() }.map { it.toHexString() }.toList()
                    val read = receipt.read().filter{ it.isNotEmpty() }.map { it.toHexString() }.toList()
                    println("received receipt \ndelivered:$delivered\nread:$read")
                    println()
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
        onIntegrity = {account, integrity: Integrity ->
            println("KMP integrity")
            Attestation.deviceCheck(applicationAddress = PublicKey.decodeHex("0016fced9deea88223b7faaee3e28f0363c99974c67ee7842ead128a0f36a9f1e3"), integrityToken =  ByteArray(integrity.requestHash().size + 128))
        }
    )

    val account = Account(config, callbacks)
    onConnect.receive()

    generateDocument(account)

    generateQrCode(account)

    println("\n")
    println("Type quit or Ctrl-C to exit")
    while (true) {
        val q = readln()
        if (q == "quit") {
            break
        }
    }
}

private suspend fun document(account: Account): PublicKey {
    val identifierAddress = account.keychainSigningCreate()
    val invocationAddress = account.keychainSigningCreate()
    val assertionAddress = account.keychainSigningCreate()
    val authenticationAddress = account.keychainSigningCreate()

    val messagingAddress = inboxAddress

    val operation = OperationBuilder()
        .identifier(identifierAddress)
        .sequence(0)
        .timestamp(Timestamp.now())
        .grantEmbeddedSigning(assertionAddress, RoleSet(Role.INVOCATION))
        .grantEmbeddedSigning(invocationAddress, RoleSet(Role.ASSERTION))
        .grantEmbeddedSigning(authenticationAddress, RoleSet(Role.AUTHENTICATION))
        .grantEmbeddedSigning(messagingAddress!!, RoleSet(Role.MESSAGING))
        .signWith(identifierAddress)
        .signWith(invocationAddress)
        .signWith(assertionAddress)
        .signWith(authenticationAddress)
        .signWith(messagingAddress)
        .finish()

    val status = suspendCoroutine { continuation ->
        account.identityExecute(operation) { status ->
            println("identityExecute status:${status.name()} ${status.errorMessage()}")
            if (status.success()) {
                continuation.resumeWith(Result.success(status))
            } else {
                continuation.resumeWith(Result.failure(SelfError(status.errorMessage()!!)))
            }
        }
    }

    println("\ndocument address:${identifierAddress.encodeHex()}")
    return identifierAddress
}

private suspend fun generateDocument(account: Account) {
    if (inboxAddress == null) inboxAddress = inboxOpen(account)
    requireNotNull(inboxAddress)

    val documentAddresses = account.identityList()
    if (documentAddresses.isNotEmpty()) {
        println("\nApplication address: ${documentAddresses.first().encodeHex()}")
        return
    }

    val identifierAddress = account.keychainSigningCreate()
    val invocationAddress = account.keychainSigningCreate()
    val assertionAddress = account.keychainSigningCreate()
    val authenticationAddress = account.keychainSigningCreate()
    val messagingAddress = inboxAddress


    val operation = OperationBuilder()
        .identifier(identifierAddress)
        .sequence(0)
        .timestamp(Timestamp.now())
        .grantEmbeddedSigning(assertionAddress, RoleSet(Role.INVOCATION))
        .grantEmbeddedSigning(invocationAddress, RoleSet(Role.ASSERTION))
        .grantEmbeddedSigning(authenticationAddress, RoleSet(Role.AUTHENTICATION))
        .grantEmbeddedSigning(messagingAddress!!, RoleSet(Role.MESSAGING))
        .signWith(identifierAddress)
        .signWith(invocationAddress)
        .signWith(assertionAddress)
        .signWith(authenticationAddress)
        .signWith(messagingAddress)
        .finish()

    val status = suspendCoroutine { continuation ->
        account.identityExecute(operation) { status ->
            println("identityExecute status:${status.name()} ${status.errorMessage()}")
            if (status.success()) {
                continuation.resumeWith(Result.success(status))
            } else {
                continuation.resumeWith(Result.failure(SelfError(status.errorMessage()!!)))
            }
        }
    }

    println("\nApplication address:${identifierAddress.encodeHex()}")
}

private suspend fun inboxOpen(account: Account): PublicKey? {
    return suspendCoroutine { continuation ->
        account.inboxOpen (expires = 0L) { status: SelfStatus, address: PublicKey ->
            println("inbox open status:${status.name()} - address:${address.encodeHex()}")
            if (status.success()) {
                continuation.resumeWith(Result.success(address))
            } else {
                continuation.resumeWith(Result.success(null))
            }
        }
    }
}

private suspend fun generateQrCode(account: Account) {
    if (inboxAddress == null) inboxAddress = inboxOpen(account)

    if (inboxAddress == null) {
        throw Exception("Can't open inbox")
    }
    println("\n")
    println("server address: ${inboxAddress!!.encodeHex()}")
    println("clients should use this address or scan the qrcode to connect to this server")

    val documentAddress = document(account)
    val expires = Timestamp.now() + 3600
//    val keyPackage = account.connectionNegotiateOutOfBand(inboxAddress!!, expires)
    val discoveryRequest = DiscoveryRequestBuilder()
//        .keyPackage(keyPackage)
        .documentAddress(documentAddress)
        .inboxAddress(inboxAddress!!)
        .expires(expires)
        .finish()
    val anonymousMessage = AnonymousMessage.fromContent(discoveryRequest)
    anonymousMessage.setFlags(FlagSet(Flag.TARGET_SANDBOX))
    val qrCodeBytes = anonymousMessage.encodeQR(QrEncoding.UNICODE)
    val qrCodeString = qrCodeBytes.decodeToString()
    println(qrCodeString)
    println()
}

@OptIn(ExperimentalStdlibApi::class)
private fun sendLivenessRequest(account: Account) {
    val livenessPredicate = Predicate.contains(CredentialField.TYPE, CredentialType.LIVENESS_AND_FACIAL_COMPARISON)
        .and(Predicate.notEmpty(CredentialField.SUBJECT_LIVENESS_AND_FACIAL_COMPARISON_SOURCE_IMAGE_HASH))
    val predicatesTree = PredicateTree.create(livenessPredicate)

    val credentialRequest = CredentialPresentationRequestBuilder()
        .presentationType("CustomPresentation")
        .predicates(predicatesTree)
        .expires(Timestamp.now() + 3600)
        .finish()
    val credentialRequestId = credentialRequest.id().toHexString()

    val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
    println("send liveness request status: ${sendStatus.name()} - to:${groupAddress?.encodeHex()} - requestId:${credentialRequestId}")
}

@OptIn(ExperimentalStdlibApi::class)
private fun sendDocumentRequest(account: Account) {
    val livenessPredicate = Predicate.contains(CredentialField.TYPE, CredentialType.PASSPORT)
        .and(Predicate.notEmpty(CredentialField.SUBJECT_PASSPORT_DOCUMENT_NUMBER))
    val predicatesTree = PredicateTree.create(livenessPredicate)

    val credentialRequest = CredentialPresentationRequestBuilder()
        .presentationType("DocumentPresentation")
        .predicates(predicatesTree)
        .expires(Timestamp.now() + 3600)
        .finish()
    val credentialRequestId = credentialRequest.id().toHexString()

    val sendStatus = account.messageSend(groupAddress!!, credentialRequest)
    println("send credential request status: ${sendStatus.name()} - to:${groupAddress?.encodeHex()} - requestId:${credentialRequestId}")
}

@OptIn(ExperimentalStdlibApi::class)
private fun sendAgreementRequest(account: Account) {
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
        println("failed to upload object ${uploadStatus.name()}")
        return
    }
    val claims = HashMap<String, Any>()
    claims["termsHash"] = agreementTerms.hash()!!.toHexString()
    claims["parties"] = arrayOf(
        hashMapOf("type" to "signatory", "id" to inboxAddress!!.encodeHex()),
        hashMapOf("type" to "signatory", "id" to responderAddress?.encodeHex()),
    )

    val unsignedAgreementCredential = CredentialBuilder()
        .credentialType("AgreementCredential")
        .credentialSubject(Address.key(inboxAddress!!))
        .credentialSubjectClaims(claims)
        .issuer(Address.key(inboxAddress!!))
        .validFrom(Timestamp.now())
        .signWith(inboxAddress!!, Timestamp.now())
        .finish()
    val signedAgreementCredential = account.credentialIssue(unsignedAgreementCredential)

    val unsignedAgreementPresentation = PresentationBuilder()
        .presentationType( "AgreementPresentation")
        .holder(Address.key(inboxAddress!!))
        .credentialAdd(signedAgreementCredential)
        .finish()
    val signedAgreementPresentation = account.presentationIssue(unsignedAgreementPresentation)

    val agreementRequest = CredentialVerificationRequestBuilder()
        .credentialType("AgreementCredential")
        .evidence("terms", agreementTerms)
        .proof(signedAgreementPresentation)
        .expires(Timestamp.now() + 3600)
        .finish()

    val sendStatus = account.messageSend(responderAddress!!, agreementRequest)
    println("send agreement status:${sendStatus.name()} - to:${responderAddress!!.encodeHex()} - requestId:${agreementRequest.id().toHexString()}")
}

@OptIn(ExperimentalStdlibApi::class)
private fun sendCustomCredentials(account: Account) {
    val subjectAddress = Address.key(responderAddress!!)
    val issuerAddress = Address.key(inboxAddress!!)
    val customerCredential = CredentialBuilder()
        .credentialType("CustomerCredential")
        .credentialSubject(subjectAddress)
        .credentialSubjectClaims(mapOf(
            "name" to "Test Name"))
        .issuer(issuerAddress)
        .validFrom(Timestamp.now())
        .signWith(inboxAddress!!, Timestamp.now())
        .finish()
    val customerVerifiableCredential = account.credentialIssue(customerCredential)

    val content = com.joinself.selfsdk.message.CredentialBuilder()
        .credential(customerVerifiableCredential)
        .finish()

    val messageId = content.id().toHexString()

    val sendStatus = account.messageSend(responderAddress!!, content)
    println("send Custom Credentials status: ${sendStatus.name()} - to:${responderAddress?.encodeHex()} - messageId:${messageId}")
}

@OptIn(ExperimentalStdlibApi::class)
private fun sendChatResponse(account: Account, chat: Chat) {
    val content = "server: respond to ${chat.message()}"
    val chat = ChatBuilder()
        .message(content)
        .finish()
    val sendStatus = account.messageSend(responderAddress!!, chat)
    val msgId = chat.id().toHexString()
    println("send chat status:${sendStatus.name()} - to:${responderAddress?.encodeHex()} - messageId:$msgId")

    // send chat notification
    val chatSummary = chat.summary()
    account.notificationSend(responderAddress!!, chatSummary) { status: SelfStatus ->
        println("send chat notification status:${status.name()} - id:${chatSummary.id().toHexString()}")
    }
}