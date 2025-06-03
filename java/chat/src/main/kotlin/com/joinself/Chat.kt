package com.joinself

import com.joinself.selfsdk.kmp.account.Account
import com.joinself.selfsdk.kmp.account.LogLevel
import com.joinself.selfsdk.kmp.account.Target
import com.joinself.selfsdk.kmp.credential.Address
import com.joinself.selfsdk.kmp.credential.CredentialBuilder
import com.joinself.selfsdk.kmp.credential.PresentationBuilder
import com.joinself.selfsdk.kmp.error.SelfStatus
import com.joinself.selfsdk.kmp.error.SelfStatusName
import com.joinself.selfsdk.kmp.event.*
import com.joinself.selfsdk.kmp.keypair.signing.PublicKey
import com.joinself.selfsdk.kmp.message.*
import com.joinself.selfsdk.kmp.platform.Attestation
import com.joinself.selfsdk.kmp.time.Timestamp
import com.joinself.selfsdk.kmp.token.Token
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Semaphore
import kotlin.coroutines.suspendCoroutine


/**
 * run in terminal: ./gradlew :chat:run
 */
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Chat Sample")

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
            println("KMP message type: $contentType")
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
                    val introduction = Introduction.decode(content)
                    introduction.tokens().firstOrNull()?.let { token: Token ->
                        if (groupAddress != null) {
                            account.tokenStore(
                                fromAddress = message.fromAddress(),
                                toAddress = groupAddress!!,
                                forAddress = groupAddress!!,
                                token = token
                            )
                            println("store push token fromAddress:${message.fromAddress().encodeHex()} - forAddress:${groupAddress?.encodeHex()}")
                        }
                    }
                    signal.release()
                }
                ContentType.CHAT -> {
                    val chat = Chat.decode(content)

                    println(
                        "received chat message " +
                        "\nfrom:${message.fromAddress().encodeHex()}" +
                        "\nmessageId:${message.id().toHexString()}" +
                        "\nmessage:${chat.message()}" +
                        "\nattachments:${chat.attachments().size}"
                    )
                    println("\n")
                }
                ContentType.RECEIPT -> {
                    val receipt = Receipt.decode(content)
                    val delivered = receipt.delivered().filter{ it.isNotEmpty() }.map { it.toHexString() }.toList()
                    val read = receipt.read().filter{ it.isNotEmpty() }.map { it.toHexString() }.toList()
                    println("received receipt \ndelivered:$delivered\nread:$read")
                    println("\n\n")
                }
                else -> {

                }
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

    println("scan the qr code to complete the discovery request")
    println(qrCodeString)

    discoveryRequestId = discoveryRequest.id().toHexString()
    println("waiting for response to discovery request requestId:${discoveryRequestId}")

    signal.acquire()
    signal.acquire()

    if (responderAddress == null || groupAddress == null) {
        println("responder address is null")
        return
    }
    println("\n")

    // send introduction
    val identifierAddress = account.keychainSigningCreate()
    val issuerAddress = Address.key(inboxAddress);
    val subjectAddress = Address.key(inboxAddress);
    val timestamp = Timestamp.now()

    val displayNameCredential = CredentialBuilder()
        .credentialType(arrayOf("VerifiableCredential", "ApplicationCredential"))
        .credentialSubject(subjectAddress)
        .credentialSubjectClaim("applicationName", "JVM chat example")
        .issuer(issuerAddress)
        .validFrom(timestamp)
        .signWith(inboxAddress, timestamp)
        .finish()
    val displayNameVerifiableCredential = account.credentialIssue(displayNameCredential)
    val unsignedPresentation = PresentationBuilder()
        .presentationType(arrayOf("VerifiablePresentation", "ApplicationPresentation"))
        .holder(Address.aureWithKey(identifierAddress, inboxAddress))
        .credentialAdd(displayNameVerifiableCredential)
        .finish()
    val signedPresentation = account.presentationIssue(unsignedPresentation)
    val introBuilder = IntroductionBuilder()
        .documentAddress(identifierAddress)
        .presentation(signedPresentation)
    val introductionStatus = account.messageSend(responderAddress, introBuilder.finish())
    println("send introduction status:${SelfStatusName.getName(introductionStatus.code())}")

    val chat = ChatBuilder()
        .message("hello")
        .finish()
    var sendStatus = account.messageSend(groupAddress, chat)

    var msgId = chat.id().toHexString()
    println("send chat status:${SelfStatusName.getName(sendStatus.code())} - to:${groupAddress.encodeHex()} - messageId:$msgId")

    println("\n\n")
    println("Press enter to exit")
    while (true) {
        val line = readln()
        if (line.isEmpty()) break

        val chat = ChatBuilder()
            .message(line)
            .finish()
        sendStatus = account.messageSend(groupAddress, chat)
        msgId = chat.id().toHexString()
        println("send chat status:${SelfStatusName.getName(sendStatus.code())} - to:${groupAddress.encodeHex()} - messageId:$msgId")

        // send chat notification
        val chatSummary = chat.summary()
        account.notificationSend(groupAddress, chatSummary) { status: SelfStatus ->
            println("send chat notification status:${SelfStatusName.getName(status.code())} - id:${chatSummary.id().toHexString()}")
        }
        println("\n")
    }
}