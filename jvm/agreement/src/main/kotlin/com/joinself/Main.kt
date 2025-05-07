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
import com.joinself.selfsdk.kmp.event.*
import com.joinself.selfsdk.kmp.keypair.signing.PublicKey
import com.joinself.selfsdk.kmp.message.*
import com.joinself.selfsdk.kmp.platform.Attestation
import com.joinself.selfsdk.kmp.time.Timestamp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.Semaphore
import kotlin.coroutines.suspendCoroutine


/**
 * run in terminal: ./gradlew :agreement:run
 */
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Agreement Sample")

    val signal = Semaphore(1)
    signal.acquire()

    var inboxAddress: PublicKey? = null
    var responderAddress: PublicKey? = null
    var discoveryRequestId: String = ""
    var agreementRequestId: String = ""
    var agreementResponse: CredentialVerificationResponse? = null

    val sandbox = true
    val rpcAddress = if (sandbox) Target.PREVIEW_SANDBOX.rpcEndpoint() else Target.PREVIEW.rpcEndpoint()
    val objectAddress = if (sandbox) Target.PREVIEW_SANDBOX.objectEndpoint() else Target.PREVIEW.objectEndpoint()
    val messageAddress = if (sandbox) Target.PREVIEW_SANDBOX.messageEndpoint() else Target.PREVIEW.messageEndpoint()

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
                    responderAddress = message.fromAddress()

                    signal.release()
                }
                ContentType.CREDENTIAL_VERIFICATION_RESPONSE -> {
                    agreementResponse = CredentialVerificationResponse.decode(content)
                    val responseTo = agreementResponse?.responseTo()?.toHexString()
                    println("received response to credential verification request from:${message.fromAddress().encodeHex()} - requestId:${responseTo} - messageId:${message.id().toHexString()}")
                    if (responseTo != agreementRequestId) {
                        println("received response to unknown request requestId:$responseTo")
                    }
                    signal.release()
                }
                else -> {

                }
            }
        },
        onProposal = { proposal: Proposal ->
            println("KMP proposal")
        },
        onWelcome = { welcome: Welcome ->
            println("KMP welcome")
            account.connectionAccept(asAddress = welcome.toAddress(), welcome =  welcome.welcome()) { status: SelfStatus, groupAddress: PublicKey ->
                println("accepted connection encrypted group status:${SelfStatusName.getName(status.code())} - from:${welcome.fromAddress().encodeHex()} - group:${groupAddress.encodeHex()}")
            }
        },
        onIntegrity = { integrity: Integrity ->
            println("KMP integrity")
            Attestation.deviceCheck(applicationAddress = PublicKey.decodeHex("0016fced9deea88223b7faaee3e28f0363c99974c67ee7842ead128a0f36a9f1e3"), integrityToken =  ByteArray(integrity.requestHash().size + 128))
        }
    )
    signal.acquire()
    println("status: ${SelfStatusName.getName(status.code())}")

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

    if (responderAddress == null) {
        println("responder address is null")
        return
    }
    println("\n")

    // create terms file
    val terms = "Agreement test"
    val agreementTerms = BinaryObject.create(
        "text/plain",
        terms.encodeToByteArray()
    )

    account.objectStore(agreementTerms)
    val uploadStatus = runBlocking {
         suspendCoroutine { continuation ->
            account.objectUpload(inboxAddress, agreementTerms, false) { status ->
                continuation.resumeWith(Result.success(status))
            }
        }
    }
    if (!uploadStatus.success()) {
        println("failed to upload object ${SelfStatusName.getName(uploadStatus.code())}")
        return
    }
    val claims = HashMap<String, Any>()
    claims["termsHash"] = agreementTerms.hash()!!.toHexString()
    claims["parties"] = arrayOf(
        hashMapOf("type" to "signatory", "id" to inboxAddress.encodeHex()),
        hashMapOf("type" to "signatory", "id" to responderAddress?.encodeHex()),
    )

    val unsignedAgreementCredential = CredentialBuilder()
        .credentialType(arrayOf("VerifiableCredential", "AgreementCredential"))
        .credentialSubject(Address.key(inboxAddress))
        .credentialSubjectClaims(claims)
        .issuer(Address.key(inboxAddress))
        .validFrom(Timestamp.now())
        .signWith(inboxAddress, Timestamp.now())
        .finish()
    val signedAgreementCredential = account.credentialIssue(unsignedAgreementCredential)

    val unsignedAgreementPresentation = PresentationBuilder()
        .presentationType(arrayOf("VerifiablePresentation", "AgreementPresentation"))
        .holder(Address.key(inboxAddress))
        .credentialAdd(signedAgreementCredential)
        .finish()
    val signedAgreementPresentation = account.presentationIssue(unsignedAgreementPresentation)

    val agreementRequest = CredentialVerificationRequestBuilder()
        .credentialType(arrayOf("VerifiableCredential", "AgreementCredential"))
        .evidence("terms", agreementTerms)
        .proof(signedAgreementPresentation)
        .expires(Timestamp.now() + 3600)
        .finish()

    agreementRequestId = agreementRequest.id().toHexString()
    val sendStatus = account.messageSend(responderAddress!!, agreementRequest)
    println("send agreement status:${SelfStatusName.getName(sendStatus.code())} - to:${responderAddress!!.encodeHex()} - requestId:${agreementRequest.id().toHexString()}")
    signal.acquire()

    if (agreementResponse == null) {
        println("agreement response is null")
        return
    }
    println("\n-------------\n")
    println("Response received with status:${agreementResponse!!.status().name}")
    var isIssued: Boolean = false
    var isSigner: Boolean = false
    agreementResponse?.credentials()?.forEach { cred ->
        try {
            cred.validate()
        } catch (ex: Exception) {
            println("failed to validate credential")
            ex.printStackTrace()
        }

        val responseClaims = cred.credentialSubjectClaims()
        val parties = responseClaims["parties"] as JsonArray
        parties.forEach { _party ->
            val party = _party as JsonObject
            val subjectId = party.get("id")?.jsonPrimitive?.content
            if (subjectId == inboxAddress.encodeHex()) {
                isIssued = true
            }
            if (subjectId == responderAddress?.encodeHex()) {
                isSigner = true
            }
        }
    }

    if (isIssued && isSigner) {
        println("Agreement is valid and signed by both parties")
    } else {
        println("Agreement is not valid or not signed by both parties")
    }

    println("\n\n")
    println("Press enter to exit")
    readln()
}