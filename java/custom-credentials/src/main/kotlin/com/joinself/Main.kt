package com.joinself

import com.joinself.selfsdk.account.*
import com.joinself.selfsdk.account.Target
import com.joinself.selfsdk.credential.Address
import com.joinself.selfsdk.error.SelfStatus
import com.joinself.selfsdk.event.*
import com.joinself.selfsdk.keypair.signing.PublicKey
import com.joinself.selfsdk.message.ContentType
import com.joinself.selfsdk.message.CredentialBuilder
import com.joinself.selfsdk.message.DiscoveryRequestBuilder
import com.joinself.selfsdk.message.DiscoveryResponse
import com.joinself.selfsdk.platform.Attestation
import com.joinself.selfsdk.time.Timestamp
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Semaphore
import kotlin.coroutines.suspendCoroutine


/**
 * run in terminal: ./gradlew :custom-credentials:run
 */
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Custom Credentials Sample")

    val signal = Semaphore(1)
    signal.acquire()

    var inboxAddress: PublicKey? = null
    var responderAddress: PublicKey? = null
    var discoveryRequestId: String = ""

    val config = Config(
        storagePath = ":memory:",
        storageKey = ByteArray(size = 32),
        target = Target.productionSandbox(),
        logLevel =  LogLevel.INFO
    )
    val callbacks = Callbacks(
        onConnect = {account ->
            println("KMP connected")
            signal.release()
        },
        onDisconnect = {account, reason: SelfStatus? ->
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
            println("KMP keypackage")
            account.connectionEstablish(asAddress =  keyPackage.toAddress(), keyPackage = keyPackage.keyPackage(),
                onCompletion = {status: SelfStatus, groupAddress: PublicKey ->
                    println("connection establish status:${status.name()} - group:${groupAddress.encodeHex()}")
                    responderAddress = keyPackage.fromAddress()

                    signal.release()
                }
            )
        },
        onWelcome = {account, welcome: Welcome ->
            println("KMP welcome")
            account.connectionAccept(asAddress = welcome.toAddress(), welcome =  welcome.welcome()) { status: SelfStatus, groupAddress: PublicKey ->
                println("accepted connection encrypted group status:${status.name()} - from:${welcome.fromAddress().encodeHex()} - group:${groupAddress.encodeHex()}")
            }
        },
        onDropped = {account,dropped: Dropped ->
            println("KMP dropped ${dropped.reason()}")
        },
        onProposal = {account, proposal: Proposal ->
            println("KMP proposal")
        },
        onMessage = {account, message: Message ->
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
                    responderAddress = message.fromAddress()

                    signal.release()
                }
                else -> {

                }
            }
        },
        onIntegrity = {account, integrity: Integrity ->
            println("KMP integrity")
            Attestation.deviceCheck(applicationAddress = PublicKey.decodeHex("0016fced9deea88223b7faaee3e28f0363c99974c67ee7842ead128a0f36a9f1e3"), integrityToken =  ByteArray(integrity.requestHash().size + 128))
        }
    )
    val account = Account(config, callbacks)
    signal.acquire()

    inboxAddress = runBlocking {
        suspendCoroutine { continuation ->
            account.inboxOpen(expires = 0L) { status: SelfStatus, address: PublicKey ->
                println("inbox open status:${status.name()} - address:${address.encodeHex()}")
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
    anonymousMessage.setFlags(FlagSet(Flag.TARGET_SANDBOX))

    val qrCodeBytes = anonymousMessage.encodeQR(QrEncoding.UNICODE)
    val qrCodeString = qrCodeBytes.decodeToString()

    println("scan the qr code to complete the discovery request")
    println(qrCodeString)

    discoveryRequestId = discoveryRequest.id().toHexString()
    println("waiting for response to discovery request requestId:${discoveryRequestId}")

    signal.acquire()

    if (responderAddress == null) {
        println("responder address is null")
        return
    }
    println("\n")

    val subjectAddress = Address.key(responderAddress)
    val issuerAddress = Address.key(inboxAddress)
    val customerCredential = com.joinself.selfsdk.credential.CredentialBuilder()
        .credentialType("CustomerCredential")
        .credentialSubject(subjectAddress)
        .credentialSubjectClaims(mapOf(
            "customer" to mapOf(
                "name" to "Test Name")))
        .issuer(issuerAddress)
        .validFrom(Timestamp.now())
        .signWith(inboxAddress, Timestamp.now())
        .finish()
    val customerVerifiableCredential = account.credentialIssue(customerCredential)

    val content = CredentialBuilder()
        .credential(customerVerifiableCredential)
        .finish()

    val messageId = content.id().toHexString()

    val sendStatus = account.messageSend(responderAddress, content)
    println("send Custom Credentials status: ${sendStatus.name()} - to:${responderAddress.encodeHex()} - messageId:${messageId}")


    println("\n\n")
    println("Press enter to exit")
    readln()
}