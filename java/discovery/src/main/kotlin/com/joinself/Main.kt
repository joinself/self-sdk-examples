package com.joinself

import com.joinself.selfsdk.account.Account
import com.joinself.selfsdk.account.LogLevel
import com.joinself.selfsdk.account.Target
import com.joinself.selfsdk.error.SelfStatus
import com.joinself.selfsdk.event.*
import com.joinself.selfsdk.keypair.signing.PublicKey
import com.joinself.selfsdk.message.ContentType
import com.joinself.selfsdk.message.DiscoveryRequestBuilder
import com.joinself.selfsdk.message.DiscoveryResponse
import com.joinself.selfsdk.platform.Attestation
import com.joinself.selfsdk.time.Timestamp
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Semaphore
import kotlin.coroutines.suspendCoroutine


/**
 * run in terminal: ./gradlew :discovery:run
 */
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Discovery Sample")

    val signal = Semaphore(1)
    signal.acquire()

    var inboxAddress: PublicKey? = null
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
                onCompletion = {status: SelfStatus, groupAddress: PublicKey ->
                    println("connection establish status:${status.name()} - group:${groupAddress.encodeHex()}")
                }
            )
        },
        onWelcome = { welcome: Welcome ->
            println("KMP welcome")
            account.connectionAccept(asAddress = welcome.toAddress(), welcome =  welcome.welcome()) { status: SelfStatus, groupAddress: PublicKey ->
                println("connection accepted status:${status.name()} - from:${welcome.fromAddress().encodeHex()} - group:${groupAddress.encodeHex()}")
            }
        },
        onDropped = {dropped: Dropped ->
            println("KMP dropped ${dropped.reason()}")
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
                    println("received response to discovery request from:${message.fromAddress().encodeHex()} - requestId:$responseTo - messageId:${message.id().toHexString()}")

                    if (responseTo != discoveryRequestId) {
                        println("received response to unknown request requestId:$responseTo")
                    }

                    signal.release()
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
    signal.acquire()
    println("status: ${status.name()}")

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

    println("Press enter to exit")
    readln()
}